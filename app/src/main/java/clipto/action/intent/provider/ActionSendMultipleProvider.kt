package clipto.action.intent.provider

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import clipto.action.intent.IntentAction
import clipto.action.intent.IntentActionProvider
import clipto.analytics.Analytics
import clipto.common.extensions.withPermissions
import clipto.domain.FileType
import clipto.presentation.clip.add.AddClipFragment
import clipto.presentation.clip.add.data.AddClipRequest
import clipto.presentation.file.add.AddFileFragment
import clipto.presentation.file.add.data.FileData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionSendMultipleProvider @Inject constructor() : IntentActionProvider<ActionSendMultipleProvider.Action>(
    actionClass = Action::class.java,
    actionId = "action_send_multiple_provider"
) {

    override fun canHandleIntent(intent: Intent): Boolean = intent.action == Intent.ACTION_SEND_MULTIPLE

    override fun handleIntent(context: Context, intent: Intent, callback: () -> Unit) {
        if (context !is FragmentActivity) {
            callback.invoke()
            return
        }
        try {
            val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            if (!uris.isNullOrEmpty()) {
                context.withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE) {
                    appState.onMain {
                        val files = uris.map { uri -> FileData(uri, FileType.SEND) }
                        AddFileFragment.show(context, files)
                    }
                }
            } else {
                callback.invoke()
            }
        } catch (e: Exception) {
            Analytics.onError("doSendMultiple", e)
            callback.invoke()
        }
    }

    class Action : IntentAction

}