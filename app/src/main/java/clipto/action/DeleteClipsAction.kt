package clipto.action

import clipto.domain.Clip
import clipto.repository.IClipRepository
import clipto.store.main.MainState
import dagger.Lazy
import io.reactivex.Completable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteClipsAction @Inject constructor(
        private val mainState: MainState,
        private val clipRepository: Lazy<IClipRepository>
) : CompletableAction<DeleteClipsAction.Context>() {

    override val name: String = "delete_clips"

    fun execute(clips: Collection<Clip>, undoDelete: Boolean = true, callback: () -> Unit = {}) = execute(Context(clips, undoDelete), callback)

    override fun create(context: Context): Completable = clipRepository.get()
            .deleteAll(context.clips.toList())
            .doOnSuccess {
                if (context.undoDelete) {
                    mainState.undoDeleteClips.setValue(setOf(it.first()))
                }
                mainState.clearSelection()
            }
            .ignoreElement()

    data class Context(
            val clips: Collection<Clip>,
            val undoDelete: Boolean = true
    ) : ActionContext(showLoadingIndicator = true)

}