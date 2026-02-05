package clipto.action.intent.provider

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import clipto.action.intent.IntentAction
import clipto.action.intent.IntentActionProvider
import clipto.analytics.Analytics
import clipto.common.extensions.withPermissions
import clipto.domain.FileType
import clipto.presentation.clip.add.AddClipFragment
import clipto.presentation.clip.add.data.AddClipRequest
import clipto.presentation.file.add.AddFileFragment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionSendProvider @Inject constructor() : IntentActionProvider<ActionSendProvider.Action>(
    actionClass = Action::class.java,
    actionId = "action_send_provider"
) {

    override fun canHandleIntent(intent: Intent): Boolean = intent.action == Intent.ACTION_SEND

    override fun handleIntent(context: Context, intent: Intent, callback: () -> Unit) {
        if (context !is FragmentActivity) {
            callback.invoke()
            return
        }
        try {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            val title = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            val item = intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)
            val uri = intent.getParcelableExtra(Intent.EXTRA_STREAM) ?: intent.data ?: item?.uri
            if (!text.isNullOrBlank()) {
                Analytics.screenClipShare()
                AddClipFragment.show(
                    context, AddClipRequest(
                        text = text,
                        title = title
                    )
                )
            } else if (item != null && uri == null) {
                item.coerceToText(context)?.let { itemText ->
                    Analytics.screenClipShare()
                    AddClipFragment.show(
                        context, AddClipRequest(
                            text = itemText.toString(),
                            title = title
                        )
                    )
                }
            } else {
                if (uri != null) {
                    context.withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE) {
                        appState.onMain { AddFileFragment.show(context, uri, FileType.SEND) }
                    }
                } else {
                    callback.invoke()
                }
            }
        } catch (e: Exception) {
            Analytics.onError("doSendText", e)
            callback.invoke()
        }
    }

    class Action : IntentAction

}