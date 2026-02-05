package clipto.store.clipboard.notification

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import clipto.AppContainer
import clipto.action.intent.IntentActionFactory
import clipto.action.intent.provider.*
import clipto.common.extensions.getFirst
import clipto.common.logging.L
import clipto.common.misc.AndroidUtils
import clipto.common.misc.IntentUtils
import clipto.common.misc.ThemeUtils
import clipto.config.IAppConfig
import clipto.dao.objectbox.ClipBoxDao
import clipto.dao.objectbox.SettingsBoxDao
import clipto.domain.Clip
import clipto.domain.NotificationStyle
import clipto.extensions.*
import clipto.store.clipboard.ClipboardState
import clipto.store.clipboard.data.toStackItem
import com.wb.clipboard.R
import javax.inject.Inject

abstract class AbstractNotificationProvider : INotificationProvider {

    @Inject
    lateinit var app: Application

    @Inject
    lateinit var clipBoxDao: ClipBoxDao

    @Inject
    lateinit var settingsBoxDao: SettingsBoxDao

    @Inject
    lateinit var appConfig: IAppConfig

    @Inject
    lateinit var clipboardState: ClipboardState

    @Inject
    lateinit var intentActionFactory: IntentActionFactory

    private val notificationUseApplicationStyle by lazy { NotificationStyle.HISTORY.isAvailable() && appConfig.notificationUseApplicationStyle() }

    protected fun createLaunchIntent(): PendingIntent {
        val launchIntent = Intent(app, AppContainer::class.java)
        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return PendingIntent.getActivity(app, AndroidUtils.nextId(), launchIntent, IntentUtils.getPendingIntentFlags())
    }

    override fun provide(channelId: String, clip: Clip?): Notification {
        intentActionFactory.clearCache()
        return NotificationCompat.Builder(app, channelId)
            .setContentTitle(clip?.text?.getFirst(appConfig.getNotificationTextMaxSize()) ?: app.getString(R.string.notification_title_empty))
            .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.drawable.app_icon)
            .setContentIntent(createLaunchIntent())
            .apply {
                modify(this, clip)
                if (!settingsBoxDao.get().clipboardUseDefaultNotificationSound) {
                    setNotificationSilent()
                }
            }
            .setChannelId(channelId)
            .build()
    }

    protected abstract fun modify(builder: NotificationCompat.Builder, clip: Clip?)

    protected fun bindControls(remoteViews: RemoteViews) {
        if (clipboardState.isClipboardSupportedBySomehow()) {
            if (clipboardState.canTakeNoteFromClipboard()) {
                val intent = intentActionFactory.getPendingIntent(PauseClipboardProvider.Action())
                remoteViews.setOnClickPendingIntent(R.id.actionStartStop, intent)
                remoteViews.setImageViewResource(R.id.actionStartStop, R.drawable.notification_action_pause)
            } else {
                val intent = intentActionFactory.getPendingIntent(ResumeClipboardProvider.Action())
                remoteViews.setOnClickPendingIntent(R.id.actionStartStop, intent)
                remoteViews.setImageViewResource(R.id.actionStartStop, R.drawable.notification_action_start)
            }
            applyActionColor(remoteViews, R.id.actionStartStop)
            remoteViews.setViewVisibility(R.id.actionStartStop, View.VISIBLE)
        } else {
            remoteViews.setViewVisibility(R.id.actionStartStop, View.GONE)
        }

        val intent = intentActionFactory.getPendingIntent(ShowClipProvider.Action())
        remoteViews.setOnClickPendingIntent(R.id.actionClipboard, intent)
        remoteViews.setImageViewResource(R.id.actionClipboard, R.drawable.notification_action_take)
        applyActionColor(remoteViews, R.id.actionClipboard)

        val addIntent = intentActionFactory.getPendingIntent(AppNewNoteProvider.Action())
        remoteViews.setOnClickPendingIntent(R.id.actionAdd, addIntent)
        applyActionColor(remoteViews, R.id.actionAdd)

        val searchIntent = intentActionFactory.getPendingIntent(AppSearchNotesProvider.Action())
        remoteViews.setOnClickPendingIntent(R.id.actionSearch, searchIntent)
        applyActionColor(remoteViews, R.id.actionSearch)

        applyBackgroundColor(remoteViews)
    }

    protected fun setActionEnabled(remoteViews: RemoteViews, id: Int, enabled: Boolean) {
        if (enabled) {
            applyActionColor(remoteViews, id)
        } else {
            applyActionColor(remoteViews, id, app.getTextInactiveColor())
        }
        remoteViews.setBoolean(id, "setEnabled", enabled)
    }

    protected fun applyBackgroundColor(remoteViews: RemoteViews) {
        if (!notificationUseApplicationStyle) return
        val backgroundColor = ThemeUtils.getColor(app, R.attr.colorNotificationBackground)
        remoteViews.setInt(R.id.notificationBackgroundView, "setBackgroundColor", backgroundColor)
    }

    protected fun applyTextColor(remoteViews: RemoteViews, id: Int) {
        if (!notificationUseApplicationStyle) return
        val color = app.getTextColorPrimary()
        remoteViews.setTextColor(id, color)
    }

    protected fun applyActionColor(remoteViews: RemoteViews, id: Int) {
        val color = app.getTextColorSecondary()
        applyActionColor(remoteViews, id, color)
    }

    protected fun applyActionAccentColor(remoteViews: RemoteViews, id: Int) {
        val color = app.getActionIconColorHighlight()
        applyActionColor(remoteViews, id, color)
    }

    private fun applyActionColor(remoteViews: RemoteViews, id: Int, color: Int) {
        if (!notificationUseApplicationStyle) return
        remoteViews.setInt(id, "setColorFilter", color)
    }

    protected fun updateHistoryLimit(limit: Int, updateThreshold: Int = limit - 1) {
        val currentStack = clipboardState.historyStack.requireValue()
        val currentStackItem = clipboardState.notification.getValue()?.text?.toStackItem()
        if (currentStack.size <= updateThreshold) {
            clipboardState.onBackground {
                runCatching {
                    if (currentStack.size < clipBoxDao.getTrackedHistoryCount()) {
                        val newStack = clipBoxDao.getTrackedHistory(limit * 2)
                            .map { it.toStackItem() }
                            .distinct()
                            .let { stack ->
                                if (stack.size > limit) {
                                    stack.subList(0, limit)
                                } else {
                                    stack
                                }
                            }
                            .toMutableList()
                        if (currentStackItem != null && !newStack.contains(currentStackItem)) {
                            newStack.add(0, currentStackItem)
                        }
                        log("update stack requested :: {} -> {}", currentStack.size, newStack.size)
                        if (currentStack.size < newStack.size || !newStack.containsAll(currentStack)) {
                            log("update stack confirmed :: {} -> {}", currentStack.size, newStack.size)
                            if (clipboardState.historyStack.setValue(newStack)) {
                                clipboardState.refreshNotification()
                            }
                        }
                    }
                }
            }
        }
    }

    protected fun log(message: String, vararg args: Any?) = L.log(this, message, *args)

}