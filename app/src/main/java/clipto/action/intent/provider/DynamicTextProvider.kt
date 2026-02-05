package clipto.action.intent.provider

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import clipto.action.intent.IntentAction
import clipto.action.intent.IntentActionProvider
import clipto.dynamic.presentation.text.DynamicTextFragment
import clipto.presentation.contextactions.ContextActionsActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicTextProvider @Inject constructor() : IntentActionProvider<DynamicTextProvider.Action>(
        actionClass = Action::class.java,
        actionId = "dynamic_text_provider"
) {

    override fun createNewIntent(action: Action): Intent {
        val intent = Intent(app, action.contextActivity)
        ContextActionsActivity.withIgnoreFocusOverlay(intent)
        intent.flags = action.flags
        return intent
    }

    override fun handleAction(context: Context, action: Action, callback: () -> Unit) {
        context as FragmentActivity
        DynamicTextFragment.show(context)
    }

    class Action(
            val contextActivity: Class<out FragmentActivity> = ContextActionsActivity::class.java,
            val flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK
    ) : IntentAction {
        override fun getSize(): Int = IntentAction.SIZE_NOT_SERIALIZABLE
    }

}