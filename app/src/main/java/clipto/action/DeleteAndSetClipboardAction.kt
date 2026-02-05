package clipto.action

import clipto.common.extensions.notNull
import clipto.domain.Clip
import clipto.extensions.from
import clipto.repository.IClipRepository
import clipto.store.clipboard.ClipboardState
import clipto.store.clipboard.IClipboardStateManager
import dagger.Lazy
import io.reactivex.Completable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteAndSetClipboardAction @Inject constructor(
    private val clipboardState: ClipboardState,
    private val clipRepository: Lazy<IClipRepository>,
    private val clipboardStateManager: Lazy<IClipboardStateManager>
) : CompletableAction<DeleteAndSetClipboardAction.Context>() {

    override val name: String = "delete_and_set_clipboard"

    fun execute(
        textToDelete: String?,
        deleteId: Long,
        textToSet: String?,
        setId: Long,
        callback: () -> Unit = {}
    ) = execute(
        Context(
            textToDelete = textToDelete,
            deleteId = deleteId,
            textToSet = textToSet,
            setId = setId,
            callback
        )
    )

    override fun create(context: Context): Completable {
        val textToDelete = context.textToDelete.notNull()
        val textToSet = context.textToSet
        return clipRepository.get().getByText(textToDelete, context.deleteId)
            .flatMap { clipRepository.get().deleteAll(listOf(it), clearClipboard = false) }
            .onErrorReturn { emptyList() }
            .flatMapCompletable {
                if (textToSet.isNullOrBlank()) {
                    clipboardState.clearClipboard()
                    context.callback.invoke()
                    Completable.complete()
                } else {
                    clipRepository.get().getByText(textToSet, context.setId)
                        .onErrorReturn { Clip.from(textToSet) }
                        .flatMapCompletable { clip ->
                            Completable.complete().doFinally {
                                clipboardStateManager.get()
                                    .onCopy(
                                        clip,
                                        saveCopied = true,
                                        withToast = false,
                                        clearSelection = false
                                    ) {
                                        context.callback.invoke()
                                    }
                            }
                        }
                }
            }
    }

    data class Context(
        val textToDelete: String?,
        val deleteId: Long,
        val textToSet: String?,
        val setId: Long,
        val callback: () -> Unit = {}
    ) : ActionContext(showLoadingIndicator = false)

}