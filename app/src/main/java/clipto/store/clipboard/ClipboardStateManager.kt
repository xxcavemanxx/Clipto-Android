package clipto.store.clipboard

import android.app.Application
import android.content.ClipboardManager
import android.content.Intent
import androidx.core.content.ContextCompat
import clipto.action.CopyClipsAction
import clipto.action.SaveClipAction
import clipto.analytics.Analytics
import clipto.common.extensions.disposeSilently
import clipto.common.logging.L
import clipto.config.IAppConfig
import clipto.domain.Clip
import clipto.extensions.toClip
import clipto.repository.IClipRepository
import clipto.store.app.AppState
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardStateManager @Inject constructor(
    val app: Application,
    val appState: AppState,
    val appConfig: IAppConfig,
    val clipboardState: ClipboardState,
    val copyClipsAction: CopyClipsAction,
    private val saveClipAction: SaveClipAction,
    private val clipRepository: IClipRepository
) : IClipboardStateManager, ClipboardManager.OnPrimaryClipChangedListener {

    val settings = appState.getSettings()
    val clipboardManager by lazy { clipboardState.clipboardManager }

    private var lastClipDisposable: Disposable? = null
    private var clipboardListener: Disposable? = null
    private val clipboardTracking = AtomicBoolean(false)

    private val adapter by lazy {
        if (clipboardState.isClipboardSupportedNatively()) {
            ClipboardStateManagerDefaultAdapter(this)
        } else {
            ClipboardStateManagerAdbAdapter(this)
        }
    }

    init {
        appState.clipboard.getLiveChanges({
            it.value?.let { track ->
                runCatching {
                    L.log(this, "track clipboard :: {}", track)
                    if (track) {
                        start()
                    } else {
                        stop()
                    }
                }
            }
        })
        var lastClip: Clip = Clip.NULL
        clipboardListener = clipboardState.clipData.getLiveChanges()
            .debounce(appConfig.logcatReadTimeout().toLong(), TimeUnit.MILLISECONDS)
            .subscribeOn(clipboardState.getBackgroundScheduler())
            .filter { clipboardState.canTakeNoteFromClipboard() }
            .filter { it.isNotNull() }
            .map { it.value!! }
            .subscribe(
                {
                    if (it.isNew()) {
                        it.toClip(app)?.takeIf { clip -> clip.text != lastClip.text }?.let { clip ->
                            L.log(this, "new clip :: {}", it)
                            saveClipAction.execute(
                                clip = clip,
                                copied = true,
                                withLoadingState = false,
                                withSilentValidation = true
                            ) { saved ->
                                if (lastClip == Clip.NULL) {
                                    runCatching { clipboardManager.setPrimaryClip(saved.toClipData()) }
                                }
                                lastClip = saved
                            }
                        }
                    } else if (it.toClip(app) == null) {
                        lastClip = Clip.NULL
                    }
                },
                { Analytics.onError("clipboard_changed", it) }
            )
    }

    override fun onPrimaryClipChanged() {
        clipboardState.refreshClipboard()
    }

    override fun onRefreshClipboard() {
        adapter.onRefreshClipboard()
    }

    override fun onTrack(track: Boolean, restoreLastClip: Boolean) {
        if (clipboardTracking.compareAndSet(!track, track)) {
            try {
                adapter.onTrack(track, restoreLastClip)
                if (track) {
                    onRefreshClipboard()
                    if (appState.isLastActivityNull() && clipboardState.canRestoreClip()) {
                        lastClipDisposable.disposeSilently()
                        lastClipDisposable = clipRepository.restoreLastCopiedClip()
                            .subscribeOn(clipboardState.getBackgroundScheduler())
                            .subscribe({}, {})
                    }
                }
            } catch (e: Exception) {
                clipboardTracking.compareAndSet(track, !track)
                Analytics.onError("error_step_init_clipboard_listeners", e)
            }
        } else if (track) {
            onRefreshClipboard()
        }
    }

    override fun onUniversalCopy(clip: Clip) {
        adapter.onUniversalCopy(clip)
    }

    override fun onCopy(
        clip: Clip,
        saveCopied: Boolean,
        clearSelection: Boolean,
        withToast: Boolean,
        callback: () -> Unit
    ) {
        adapter.onCopy(
            clip = clip,
            saveCopied = saveCopied,
            clearSelection = clearSelection,
            withToast = withToast,
            callback = callback
        )
    }

    override fun onCopy(
        clips: Collection<Clip>,
        saveCopied: Boolean,
        clearSelection: Boolean,
        callback: () -> Unit
    ) {
        adapter.onCopy(
            clips = clips,
            saveCopied = saveCopied,
            clearSelection = clearSelection,
            callback = callback
        )
    }

    private fun start(): Boolean {
        if (clipboardState.isClipboardActivated()) {
            ContextCompat.startForegroundService(app, Intent(app, ClipboardService::class.java))
            ClipboardAwakeWorker.schedule(app, appConfig.clipboardAwareInterval())
        }
        return !clipboardTracking.get()
    }

    private fun stop() {
        app.stopService(Intent(app, ClipboardService::class.java))
        ClipboardAwakeWorker.cancel(app)
    }

}