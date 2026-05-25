package clipto.action

import clipto.domain.Clip
import clipto.dynamic.DynamicField
import clipto.dynamic.DynamicValueConfig
import clipto.dynamic.IDynamicValuesRepository
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetClipTextAction @Inject constructor(
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
        .map { it.text!! }
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