package clipto.action.intent.provider

import android.content.Context
import android.content.Intent
import clipto.AppContainer
import clipto.action.intent.IntentAction
import clipto.action.intent.IntentActionProvider
import clipto.domain.Clip
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreviewNoteProvider @Inject constructor() : IntentActionProvider<AppPreviewNoteProvider.Action>(
        actionClass = Action::class.java,
        actionId = "app_preview_note_provider"
) {

    override fun createNewIntent(action: Action): Intent {
        val intent = Intent(app, AppContainer::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return intent
    }

    override fun handleAction(context: Context, action: Action, callback: () -> Unit) {
        appState.requestIntentAction(action)
        callback.invoke()
    }

    data class Action(val clip: Clip) : IntentAction {
        override fun getSize(): Int = IntentAction.SIZE_NOT_SERIALIZABLE
    }

}