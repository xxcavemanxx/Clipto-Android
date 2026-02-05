package clipto.action

import android.app.Application
import android.net.Uri
import clipto.domain.*
import clipto.repository.IClipRepository
import clipto.repository.IFileRepository
import dagger.Lazy
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveClipAction @Inject constructor(
    private val app: Application,
    private val clipRepository: Lazy<IClipRepository>,
    private val fileRepository: Lazy<IFileRepository>
) : CompletableAction<SaveClipAction.Context>() {

    override val name: String = "save_clip"

    fun execute(
        clip: Clip,
        copied: Boolean = false,
        files: List<Uri> = emptyList(),
        withLoadingState: Boolean = true,
        withDisposeRunning: Boolean = true,
        withSilentValidation: Boolean = false,
        callback: (clip: Clip) -> Unit = {}
    ) = execute(
        Context(
            clip = clip,
            files = files,
            copied = copied,
            withLoadingState = withLoadingState,
            withDisposeRunning = withDisposeRunning,
            withSilentValidation = withSilentValidation,
            callback = callback
        )
    )

    override fun create(context: Context) = context.files.map { fileRepository.get().upload(it, FileType.FILE) }
        .let { tasks ->
            if (tasks.isEmpty()) {
                Single.just(emptyList())
            } else {
                Single.zip(tasks) { it -> it.map { it as FileRef } }
            }
        }
        .flatMapCompletable { files ->
            val clip = context.clip
            if (clip.text.isNullOrBlank() && files.isNotEmpty()) {
                clip.description = files.joinToString(ClientSession.SEPARATOR_MD) { it.toString(app) }
            }
            clipRepository.get()
                .save(clip, context.copied)
                .observeOn(appState.getViewScheduler())
                .doOnSuccess { context.callback.invoke(it) }
                .ignoreElement()
        }

    data class Context(
        val clip: Clip,
        val copied: Boolean = false,
        val withLoadingState: Boolean = true,
        val files: List<Uri> = emptyList(),
        val withDisposeRunning: Boolean = true,
        val withSilentValidation: Boolean = false,
        val callback: (clip: Clip) -> Unit = {}
    ) : ActionContext(showLoadingIndicator = withLoadingState, disposeRunning = withDisposeRunning)

}