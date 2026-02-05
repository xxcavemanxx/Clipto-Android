package clipto.action

import clipto.domain.Clip
import clipto.dynamic.DynamicField
import clipto.dynamic.DynamicValueConfig
import clipto.dynamic.IDynamicValuesRepository
import clipto.repository.IFileRepository
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetClipTextAction @Inject constructor(
    private val fileRepository: Lazy<IFileRepository>,
    private val dynamicValuesRepository: Lazy<IDynamicValuesRepository>
) : CompletableAction<GetClipTextAction.Context>() {

    override val name: String = "get_clip_text"

    fun execute(
        clip: Clip,
        callback: (text: String) -> Unit = {}
    ) = execute(
        Context(
            clip = clip,
            callback = callback
        )
    )

    override fun create(context: Context): Completable = Single
        .fromCallable { context.clip }
        .flatMap { clip ->
            val text = clip.text!!
            if (DynamicField.isDynamic(text)) {
                dynamicValuesRepository.get().process(text, DynamicValueConfig(clip = clip))
                    .map { it.toString() }
                    .map { newText ->
                        Clip().apply {
                            this.textType = clip.textType
                            this.snippet = clip.snippet
                            this.tagIds = clip.tagIds
                            this.title = clip.title
                            this.fav = clip.fav
                            this.text = newText
                        }
                    }
            } else {
                Single.just(clip)
            }
        }
        .flatMap { clip -> fileRepository.get().getFiles(clip.fileIds).map { files -> clip to files } }
        .flatMap { data ->
            val clip = data.first
            val files = data.second
            fileRepository.get().getPublicLinks(files)
                .onErrorReturn { emptyList() }
                .map { urls ->
                    if (urls.isEmpty()) {
                        Pair(clip.text, clip.title)
                    } else {
                        val sb = StringBuilder()
                        sb.append(clip.text)
                        sb.append('\n')
                        sb.append('\n')
                        files.forEachIndexed { index, file ->
                            val url = urls.getOrNull(index)
                            sb.append('[')
                            sb.append(file.title)
                            sb.append(']')
                            sb.append('\n')
                            sb.append(url)
                            sb.append('\n')
                            sb.append('\n')
                        }
                        Pair(sb.trim().toString(), clip.title)
                    }
                }
        }
        .map { it.first!! }
        .observeOn(appState.getViewScheduler())
        .doOnSuccess { context.callback.invoke(it) }
        .doOnError { appState.showToast(it.message.toString()) }
        .doOnError { context.callback.invoke(context.clip.text!!) }
        .ignoreElement()

    data class Context(
        val clip: Clip,
        val callback: (text: String) -> Unit
    ) : ActionContext(withTimeout = false)

}