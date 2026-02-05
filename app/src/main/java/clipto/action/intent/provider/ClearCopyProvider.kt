package clipto.action.intent.provider

import android.content.Context
import clipto.action.intent.IntentAction
import clipto.action.intent.IntentActionProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClearCopyProvider @Inject constructor() : IntentActionProvider<ClearCopyProvider.Action>(
        actionClass = Action::class.java,
        actionId = "clear_copy_provider"
) {

    override fun handleAction(context: Context, action: Action, callback: () -> Unit) {
        clipboardState.clearClipboard()
        closeSystemDialogs()
        callback.invoke()
    }

    class Action : IntentAction

}