package clipto.action.intent.provider

import android.content.Context
import clipto.action.SaveSettingsAction
import clipto.action.intent.IntentAction
import clipto.action.intent.IntentActionProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResumeClipboardProvider @Inject constructor(
        private val saveSettingsAction: SaveSettingsAction
) : IntentActionProvider<ResumeClipboardProvider.Action>(
        actionClass = Action::class.java,
        actionId = "resume_clipboard_provider"
) {

    override fun handleAction(context: Context, action: Action, callback: () -> Unit) {
        clipboardState.canTakeNoteFromClipboard.setValue(true)
        clipboardState.refreshNotification()
        appState.getSettings().pauseClipboard = false
        saveSettingsAction.execute()
        callback.invoke()
    }

    class Action : IntentAction

}