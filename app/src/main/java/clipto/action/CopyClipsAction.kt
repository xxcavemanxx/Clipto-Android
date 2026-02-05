package clipto.action

import clipto.common.extensions.notNull
import clipto.domain.Clip
import clipto.dynamic.DynamicField
import clipto.dynamic.DynamicValueConfig
import clipto.dynamic.IDynamicValuesRepository
import clipto.extensions.isNew
import clipto.repository.IClipRepository
import clipto.store.clipboard.ClipboardState
import clipto.store.clipboard.toClipData
import clipto.store.main.MainState
import clipto.utils.DomainUtils
import com.wb.clipboard.R
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CopyClipsAction @Inject constructor(
    private val mainState: MainState,
    private val clipboardState: ClipboardState,
    private val clipRepository: Lazy<IClipRepository>,
    private val dynamicValuesRepository: Lazy<IDynamicValuesRepository>
) : CompletableAction<CopyClipsAction.Context>() {

    override val name: String = "copy_clips"

    fun execute(
        clips: Collection<Clip>,
        withToast: Boolean = true,
        saveCopied: Boolean = true,
        clearSelection: Boolean = true,
        callback: () -> Unit = {}
    ) = execute(
        Context(
            clips = clips,
            withToast = withToast,
            saveCopied = saveCopied,
            clearSelection = clearSelection
        ), callback
    )

    override fun create(context: Context): Completable = Single
        .fromCallable {
            val clips = context.clips
            if (clips.size == 1) {
                clips.first()
            } else {
                Clip().apply {
                    text = clips.map { it.text }.joinToString("\n\n").trim()
                    tagIds = DomainUtils.getCommonTagIds(clips)
                }
            }
        }
        // apply dynamic values
        .flatMap { clip ->
            val text = clip.text.notNull()
            if (DynamicField.isDynamic(text)) {
                val actionType = DynamicValueConfig.ActionType.COPY
                dynamicValuesRepository.get().process(text, DynamicValueConfig(clip = clip, actionType = actionType))
                    .map { it.toString() }
                    .map { newText ->
                        if (newText == text) {
                            clip
                        } else {
                            Clip().apply {
                                this.textType = clip.textType
                                this.tagIds = clip.tagIds
                                this.title = clip.title
                                this.fav = clip.fav
                                this.text = newText
                            }
                        }
                    }
            } else {
                Single.just(clip)
            }
        }
        .flatMap { clip ->
            if (clip.isNew()) {
                clipRepository.get().getByText(clip.text).onErrorReturn { clip }
            } else {
                Single.just(clip)
            }
        }
        .flatMap { clip ->
            val clipData = clip.toClipData()
            clipboardState.clipboardManager.setPrimaryClip(clipData)
            if (context.saveCopied && (!clip.isNew() || clipboardState.canTakeNoteFromClipboard())) {
                val delay = if (appState.getSettings().hideOnCopy) appConfig.clipboardHideOnCopyDelay() else 0
                clipRepository.get()
                    .save(clip, true).delay(delay, TimeUnit.MILLISECONDS)
                    .doOnSuccess { clipboardState.clipData.setValue(it.toClipData()) }
                    .doOnError { clipboardState.clipData.setValue(clipData) }
            } else {
                Single.just(clip).doFinally { clipboardState.clipData.setValue(clipData) }
            }
        }
        .doOnSubscribe { if (context.clearSelection) mainState.clearSelection() }
        .doOnSuccess { if (context.saveCopied && appState.getSettings().hideOnCopy) clipboardState.requestHide() }
        .doOnSuccess { if (context.withToast) appState.showToast(R.string.clip_snackbar_text_copied) }
        .doOnSuccess { clipboardState.clip.setValue(it) }
        .ignoreElement()

    data class Context(
        val clips: Collection<Clip>,
        val withToast: Boolean = true,
        val saveCopied: Boolean = true,
        val clearSelection: Boolean = true,
    ) : ActionContext(showLoadingIndicator = false, withTimeout = false)

}