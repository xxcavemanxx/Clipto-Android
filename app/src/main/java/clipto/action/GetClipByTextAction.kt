package clipto.action

import clipto.domain.Clip
import clipto.repository.IClipRepository
import dagger.Lazy
import io.reactivex.Completable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetClipByTextAction @Inject constructor(
    private val clipRepository: Lazy<IClipRepository>
) : CompletableAction<GetClipByTextAction.Context>() {

    override val name: String = "get_clip_by_text"

    fun execute(
        id: Long,
        text: String,
        fail: (text: String) -> Unit = {},
        success: (clip: Clip) -> Unit
    ) = execute(
        Context(
            id = id,
            text = text,
            fail = fail,
            success = success
        )
    )

    override fun create(context: Context): Completable = clipRepository.get()
        .getByText(context.text, context.id)
        .observeOn(appState.getViewScheduler())
        .doOnSuccess { context.success.invoke(it!!) }
        .doOnError { context.fail.invoke(context.text) }
        .ignoreElement()

    data class Context(
        val id: Long,
        val text: String,
        val fail: (text: String) -> Unit = {},
        val success: (clip: Clip) -> Unit
    ) : ActionContext()

}