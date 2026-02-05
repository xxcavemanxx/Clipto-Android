package clipto.action.intent

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import clipto.action.intent.provider.*
import clipto.common.logging.L
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("UNCHECKED_CAST")
class IntentActionFactory @Inject constructor(
    private val resumeClipboardProvider: Lazy<ResumeClipboardProvider>,
    private val pauseClipboardProvider: Lazy<PauseClipboardProvider>,
    private val processTextProvider: Lazy<ActionProcessTextProvider>,
    private val deleteClipProvider: Lazy<DeleteClipProvider>,
    private val fastActionProvider: Lazy<FastActionProvider>,
    private val clearCopyProvider: Lazy<ClearCopyProvider>,
    private val showClipProvider: Lazy<ShowClipProvider>,
    private val setClipProvider: Lazy<SetClipProvider>,
    private val sendProvider: Lazy<ActionSendProvider>,
    private val sendMultipleProvider: Lazy<ActionSendMultipleProvider>,
    private val copyProvider: Lazy<CopyProvider>,
    private val appSearchNotesProvider: Lazy<AppSearchNotesProvider>,
    private val appPreviewNoteProvider: Lazy<AppPreviewNoteProvider>,
    private val appViewNoteProvider: Lazy<AppViewNoteProvider>,
    private val appEditNoteProvider: Lazy<AppEditNoteProvider>,
    private val appNewNoteProvider: Lazy<AppNewNoteProvider>,
    private val appAuthProvider: Lazy<AppAuthProvider>,
    private val dynamicTextProvider: Lazy<DynamicTextProvider>
) {

    private val providers: List<Lazy<out IntentActionProvider<out IntentAction>>> by lazy {
        listOf(
            copyProvider,
            setClipProvider,
            showClipProvider,
            clearCopyProvider,
            deleteClipProvider,
            fastActionProvider,
            pauseClipboardProvider,
            resumeClipboardProvider,
            processTextProvider,
            sendProvider,
            sendMultipleProvider,
            appSearchNotesProvider,
            appPreviewNoteProvider,
            appViewNoteProvider,
            appEditNoteProvider,
            appNewNoteProvider,
            appAuthProvider,
            dynamicTextProvider
        )
    }

    fun clearCache() = IntentActionProvider.clearActionCache()

    fun getPendingIntent(action: IntentAction): PendingIntent {
        val provider = providers.find { it.get().canHandleAction(action) }?.get() as IntentActionProvider<IntentAction>
        return provider.createPendingIntent(action)
    }

    fun getIntent(action: IntentAction): Intent {
        val provider = providers.find { it.get().canHandleAction(action) }?.get() as IntentActionProvider<IntentAction>
        return provider.createIntent(action)
    }

    fun handle(context: Context, intent: Intent, callback: () -> Unit = {}): Boolean {
        try {
            if (intent.action == null) {
                callback.invoke()
            } else {
                val provider = providers.find { it.get().canHandleIntent(intent) }?.get() as IntentActionProvider<IntentAction>?
                L.log(this, "handle intent with provider :: context={}, intent={}, provider={}", context, intent, provider)
                provider?.handleIntent(context, intent, callback) ?: callback.invoke()
                return provider != null
            }
        } catch (e: Exception) {
            L.log(this, "handle", e)
            callback.invoke()
        }
        return false
    }

}