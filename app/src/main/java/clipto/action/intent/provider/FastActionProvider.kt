package clipto.action.intent.provider

import android.content.Context
import android.content.Intent
import android.widget.TextView
import clipto.action.GetClipByTextAction
import clipto.action.intent.IntentAction
import clipto.action.intent.IntentActionProvider
import clipto.domain.FastAction
import clipto.domain.Clip
import clipto.dynamic.DynamicField
import clipto.extensions.from
import clipto.presentation.contextactions.ContextActionsActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FastActionProvider @Inject constructor(
    private val getClipByTextAction: GetClipByTextAction
) : IntentActionProvider<FastActionProvider.Action>(
    actionClass = Action::class.java,
    actionId = "fast_action_provider"
) {

    override fun createNewIntent(action: Action): Intent {
        val fastAction = FastAction.findById(action.actionId)
        if (fastAction?.requiredPreRendering == true || DynamicField.isDynamic(action.text)) {
            val intent = Intent(app, ContextActionsActivity::class.java)
            ContextActionsActivity.withIgnoreFocusOverlay(intent)
            return intent
        }
        return super.createNewIntent(action)
    }

    override fun handleAction(context: Context, action: Action, callback: () -> Unit) {
        val fastAction = FastAction.findById(action.actionId)
        if (fastAction != null) {
            if (context is ContextActionsActivity) {
                getClipByTextAction.execute(
                    id = action.id,
                    text = action.text,
                    success = { context.viewModel.dialogState.requestFastAction(fastAction, it) },
                    fail = { context.viewModel.dialogState.requestFastAction(fastAction, Clip.from(it)) }
                )
            } else {
                getClipByTextAction.execute(
                    id = action.id,
                    text = action.text,
                    success = { fastAction.process(it, TextView(app)) },
                    fail = { fastAction.process(Clip.from(action.text), TextView(app)) }
                )
            }
        } else {
            callback.invoke()
        }
        closeSystemDialogs()
    }

    data class Action(
        val actionId: Int,
        val text: String,
        val id: Long = 0
    ) : IntentAction {
        override fun getSize(): Int = text.length
    }

}