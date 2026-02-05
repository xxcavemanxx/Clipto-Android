package clipto.action.intent.provider

import android.content.Context
import clipto.action.SaveSettingsAction
import clipto.action.intent.IntentAction
import clipto.action.intent.IntentActionProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PauseClipboardProvider @Inject constructor(
        private val saveSettingsAction: SaveSettingsAction
) : IntentActionProvider<PauseClipboardProvider.Action>(
        actionClass = Action::class.java,
        actionId = "pause_clipboard_provider"
) {

    override fun handleAction(context: Context, action: Action, callback: () -> Unit) {
        clipboardState.canTakeNoteFromClipboard.setValue(false)
        clipboardState.refreshNotification()
        appState.getSettings().pauseClipboard = true
        saveSettingsAction.execute()
        callback.invoke()
    }

    class Action : IntentAction

}