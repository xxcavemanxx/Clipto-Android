package clipto.action.intent

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.SparseArray
import clipto.analytics.Analytics
import clipto.common.extensions.closeSystemDialogs
import clipto.common.logging.L
import clipto.common.misc.AndroidUtils
import clipto.common.misc.IntentUtils
import clipto.config.IAppConfig
import clipto.presentation.contextactions.ContextActionsActivity
import clipto.store.app.AppState
import clipto.store.clipboard.ClipboardService
import clipto.store.clipboard.ClipboardState
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@Suppress("UNCHECKED_CAST")
abstract class IntentActionProvider<A : IntentAction>(private val actionId: String, private val actionClass: Class<A>) {

    @Inject
    lateinit var app: Application

    @Inject
    lateinit var appState: AppState

    @Inject
    lateinit var appConfig: IAppConfig

    @Inject
    lateinit var clipboardState: ClipboardState

    fun createPendingIntent(action: A): PendingIntent {
        val id = AndroidUtils.nextId()
        val intent = createIntent(action)
        log("createPendingIntent :: className={}", intent.component?.className)
        return if (intent.component!!.className.endsWith("Service")) {
            PendingIntent.getService(app, id, intent, IntentUtils.getPendingIntentFlags())
        } else {
            PendingIntent.getActivity(app, id, intent, IntentUtils.getPendingIntentFlags())
        }
    }

    fun createIntent(action: A): Intent {
        val intent = createNewIntent(action)
        val actionSize = action.getSize()
        if (actionSize == IntentAction.SIZE_NOT_SERIALIZABLE || actionSize > appConfig.getNotificationIntentMaxSize()) {
            log("create not serializable action :: id={}, size={}", actionId, actionSize)
            Analytics.onNotSerializableAction(actionId, actionSize)
            intent.putExtra(ATTR_ACTION_ID, cacheAction(action))
        } else {
            intent.putExtra(ATTR_ACTION, action)
        }
        intent.putExtra(ATTR_ACTION_TIME, UUID.randomUUID().toString())
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.action = actionId
        return intent
    }

    protected open fun createNewIntent(action: A): Intent {
        return if (clipboardState.isClipboardActivated()) {
            Intent(app, ClipboardService::class.java)
        } else {
            Intent(app, ContextActionsActivity::class.java)
        }
    }

    protected fun closeSystemDialogs() = app.closeSystemDialogs()
    protected fun log(message: String, vararg args: Any?) = L.log(this, message, *args)

    open fun canHandleAction(action: IntentAction): Boolean = action.javaClass == actionClass
    open fun canHandleIntent(intent: Intent): Boolean = intent.action == actionId
    open fun handleIntent(context: Context, intent: Intent, callback: () -> Unit = {}) {
        val action =
            if (intent.hasExtra(ATTR_ACTION_ID)) {
                val id = intent.getIntExtra(ATTR_ACTION_ID, 0)
                notSerializableActions.get(id)
            } else {
                intent.getSerializableExtra(ATTR_ACTION)
            }
        if (action is IntentAction && canHandleAction(action)) {
            log("handle action :: {}", action)
            handleAction(context, action as A, callback)
        } else {
            log("unknown action :: {}", action)
            callback.invoke()
        }
    }

    open fun handleAction(context: Context, action: A, callback: () -> Unit = {}) {
        callback.invoke()
    }

    companion object {
        private const val ATTR_ACTION = "attr_action"
        private const val ATTR_ACTION_TIME = "attr_action_time"
        private const val ATTR_ACTION_ID = "attr_action_id_not_serializable"
        private val notSerializableActionsSeed = AtomicInteger()
        private val notSerializableActions = SparseArray<IntentAction>()
        fun clearActionCache() = notSerializableActions.clear()
        fun cacheAction(action: IntentAction): Int {
            val id = notSerializableActionsSeed.incrementAndGet()
            notSerializableActions.put(id, action)
            return id
        }
    }

}