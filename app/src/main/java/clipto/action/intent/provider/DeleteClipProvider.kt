package clipto.action.intent.provider

import android.content.Context
import clipto.action.DeleteAndSetClipboardAction
import clipto.action.intent.IntentAction
import clipto.action.intent.IntentActionProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteClipProvider @Inject constructor(
    private val deleteAndSetClipboardAction: DeleteAndSetClipboardAction,
) : IntentActionProvider<DeleteClipProvider.Action>(
    actionClass = Action::class.java,
    actionId = "delete_clip_provider"
) {

    override fun handleAction(context: Context, action: Action, callback: () -> Unit) {
        deleteAndSetClipboardAction.execute(
            textToDelete = action.deleteText,
            deleteId = action.deleteId,
            textToSet = action.setText,
            setId = action.setId,
            callback = callback
        )
    }

    data class Action(
        val deleteText: String,
        val deleteId: Long,
        val setText: String?,
        val setId: Long
    ) : IntentAction {
        override fun getSize(): Int = deleteText.length + (setText?.length ?: 0)
    }

}