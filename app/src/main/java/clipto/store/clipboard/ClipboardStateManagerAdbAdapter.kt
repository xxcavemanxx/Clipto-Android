package clipto.store.clipboard

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import clipto.analytics.Analytics
import clipto.common.extensions.canDrawOverlayViews
import clipto.common.extensions.disposeSilently
import clipto.common.extensions.doOnFirstLayout
import clipto.common.extensions.isNullOrDisposed
import clipto.common.logging.L
import clipto.domain.Clip
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal class ClipboardStateManagerAdbAdapter constructor(manager: ClipboardStateManager) : ClipboardStateManagerAdapter(manager) {

    private val windowManager by lazy { app.getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    private val app = manager.app
    private val appConfig = manager.appConfig
    private val clipboardState = manager.clipboardState

    private var advancedClipboardMode: AdvancedClipboardViewMode = AdvancedClipboardViewMode.PUT
    private var advancedClipboardActive = AtomicBoolean(false)
    private var advancedClipboardListener: Disposable? = null
    private var advancedClipboardClips: Collection<Clip>? = null
    private var advancedClipboardLogcatListener: Disposable? = null
    private var advancedClipboardView: View? = null
    private var advancedClipboardLastRun = 0L

    init {
        clipboardState.supportAdvancedClipboardTracking.getLiveChanges({
            val value = it.value
            if (value != null) {
                if (value && clipboardState.isClipboardActivated()) {
                    readClipFromLogcat()
                } else {
                    cancelClipFromLogcat()
                }
            }
        })
    }

    override fun onRefreshClipboard() {
        if (app.canDrawOverlayViews()) {
            showAdvancedClipboardView(AdvancedClipboardViewMode.READ)
        } else {
            super.onRefreshClipboard()
        }
    }

    override fun onTrack(track: Boolean, restoreLastClip: Boolean) {
        onResetAdvancedClipboardView()
        if (track) {
            manager.clipboardManager.addPrimaryClipChangedListener(manager)
            readClipFromLogcat()
        } else {
            manager.clipboardManager.removePrimaryClipChangedListener(manager)
            cancelClipFromLogcat()
        }
    }

    private fun readClipFromLogcat() {
        synchronized(this) {
            if (advancedClipboardLogcatListener.isNullOrDisposed() && clipboardState.isClipboardSupportedByAdb()) {
                L.log(this, "readClipFromLogcat :: started")
                val timeout = appConfig.logcatReadTimeout().toLong()
                val thread = Schedulers.from(Executors.newSingleThreadExecutor())
                advancedClipboardLogcatListener.disposeSilently()
                advancedClipboardLogcatListener = getClipFromLogcat()
                    .subscribeOn(thread)
                    .retry()
                    .filter { clipboardState.canTakeNoteFromClipboard() && !advancedClipboardActive.get() }
                    .sample(timeout, TimeUnit.MILLISECONDS, false)
                    .subscribe({
                        showAdvancedClipboardView(AdvancedClipboardViewMode.READ)
                    }, {
                        Analytics.onError("error_read_clip_from_logcat", it)
                    })
            }
        }
    }

    private fun cancelClipFromLogcat() {
        synchronized(this) {
            L.log(this, "readClipFromLogcat :: canceled")
            advancedClipboardLogcatListener.disposeSilently()
            advancedClipboardLogcatListener = null
        }
    }

    private fun getClipFromLogcat() = Observable.create<String> { emitter ->
        try {
            runCatching { Runtime.getRuntime().exec("logcat -c").waitFor() }
            val process = Runtime.getRuntime().exec("logcat -s ClipboardService:E")
            emitter.setCancellable { process.destroy() }
            val reader = process.inputStream.bufferedReader()
            val packageName = app.packageName
            while (!emitter.isDisposed) {
                try {
                    L.log(this, "readClipFromLogcat :: read line")
                    reader.readLine()
                        ?.takeIf { it.contains(packageName) }
                        ?.let { emitter.onNext(it) }
                } catch (e: Exception) {
                    L.log(this, "readClipFromLogcat :: interrupted")
                }
            }
        } catch (th: Throwable) {
            emitter.onError(th)
        }
    }

    private fun showAdvancedClipboardView(mode: AdvancedClipboardViewMode, clips: Collection<Clip>? = null) {
        if (advancedClipboardActive.compareAndSet(false, true)) {
            advancedClipboardListener?.takeIf { !it.isDisposed }?.dispose()
            advancedClipboardListener = clipboardState.getViewScheduler().scheduleDirect(
                { onResetAdvancedClipboardView() },
                appConfig.logcatCancelTimeout().toLong(),
                TimeUnit.MILLISECONDS
            )

            advancedClipboardMode = mode
            advancedClipboardClips = clips

            clipboardState.onMain {
                try {
                    onResetAdvancedClipboardView(skipStateReset = true)
                    advancedClipboardLastRun = System.currentTimeMillis()
                    val view = AdvancedClipboardView(app)
                    advancedClipboardView = view
                    val type =
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
                        } else {
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        }
                    val params = WindowManager.LayoutParams(type, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, PixelFormat.TRANSLUCENT)
                    windowManager.addView(view, params)
                } catch (e: Exception) {
                    Analytics.onError("error_save_clip_from_logcat", e)
                    onResetAdvancedClipboardView()
                }
            }
        }
    }

    private fun onResetAdvancedClipboardView(skipStateReset: Boolean = false) {
        try {
            advancedClipboardView?.let { view ->
                try {
                    windowManager.removeView(view)
                    advancedClipboardView = null
                } catch (e: Exception) {
                    Analytics.onError("error_clear_copy_view", e)
                }
            }
        } finally {
            if (!skipStateReset) {
                advancedClipboardActive.set(false)
            }
        }
    }

    private enum class AdvancedClipboardViewMode { READ, COPY, PUT }

    private inner class AdvancedClipboardView(context: Context) : FrameLayout(context) {

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            doOnFirstLayout(
                fail = { onResetAdvancedClipboardView() },
                success = {
                    when (advancedClipboardMode) {
                        AdvancedClipboardViewMode.READ -> {
                            clipboardState.getPrimaryClip()?.let { clipboardState.refreshClipboard(primaryClip = it) }
                            onResetAdvancedClipboardView()
                        }

                        AdvancedClipboardViewMode.PUT -> {
                            advancedClipboardClips
                                ?.let { clips ->
                                    manager.copyClipsAction.execute(clips, saveCopied = false) {
                                        clipboardState.onMain { onResetAdvancedClipboardView() }
                                    }

                                } ?: onResetAdvancedClipboardView()
                        }

                        AdvancedClipboardViewMode.COPY -> {
                            advancedClipboardClips
                                ?.let { clips ->
                                    manager.copyClipsAction.execute(clips, saveCopied = true) {
                                        clipboardState.onMain { onResetAdvancedClipboardView() }
                                    }

                                } ?: onResetAdvancedClipboardView()
                        }
                    }
                }
            )
        }
    }

}