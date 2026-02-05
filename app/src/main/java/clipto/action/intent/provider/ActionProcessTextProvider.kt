package clipto.action.intent.provider

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import clipto.action.intent.IntentAction
import clipto.action.intent.IntentActionProvider
import clipto.analytics.Analytics
import clipto.dao.objectbox.model.ClipBox
import clipto.presentation.clip.add.AddClipFragment
import clipto.presentation.clip.add.data.AddClipRequest
import clipto.store.clipboard.IClipboardStateManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionProcessTextProvider @Inject constructor(
    private val clipboardStateManager: IClipboardStateManager
) : IntentActionProvider<ActionProcessTextProvider.Action>(
    actionClass = Action::class.java,
    actionId = "action_process_text_provider"
) {

    override fun canHandleIntent(intent: Intent): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && intent.action == Intent.ACTION_PROCESS_TEXT

    @RequiresApi(Build.VERSION_CODES.M)
    override fun handleIntent(context: Context, intent: Intent, callback: () -> Unit) {
        intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            ?.takeIf { it.isNotBlank() }
            ?.let {
                val text = it.toString()
                if (clipboardState.canEmulateCopyAction()) {
                    val clip = ClipBox().apply {
                        this.tracked = true
                        this.text = text
                    }
                    clipboardStateManager.onCopy(
                        clip = clip,
                        clearSelection = false,
                        saveCopied = true,
                        callback = callback
                    )
                } else {
                    Analytics.screenClipProcessText()
                    AddClipFragment.show(
                        context, AddClipRequest(
                            text = text
                        )
                    )
                }
            }
            ?: run { callback.invoke() }
    }

    class Action : IntentAction

}