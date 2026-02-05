package clipto.store.clipboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import clipto.analytics.Analytics
import clipto.store.app.AppState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ClipboardReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appState: AppState

    @Inject
    lateinit var clipboardState: ClipboardState

    @Inject
    lateinit var clipboardStateManager: IClipboardStateManager

    override fun onReceive(context: Context, intent: Intent?) {
        try {
            if (clipboardState.canRunOnStartup()) {
                if (appState.clipboard.setValue(true)) {
                    Analytics.onTrackedAfterReboot()
                }
            }
        } catch (e: Exception) {
            Analytics.onError("error_track_clipboard_on_boot", e)
        }
    }

}