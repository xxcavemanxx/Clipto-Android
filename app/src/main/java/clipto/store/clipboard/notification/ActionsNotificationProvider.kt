package clipto.store.clipboard.notification

import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import clipto.action.intent.provider.*
import clipto.common.extensions.getFirst
import clipto.common.extensions.notNull
import clipto.common.extensions.toLinkifiedSpannable
import clipto.domain.Clip
import clipto.domain.FastAction
import clipto.extensions.getId
import clipto.store.app.AppState
import clipto.store.clipboard.data.HistoryStackItem
import clipto.store.clipboard.data.toStackItem
import com.wb.clipboard.R
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class ActionsNotificationProvider @Inject constructor(
    val appState: AppState
) : AbstractNotificationProvider() {

    override fun modify(builder: NotificationCompat.Builder, clip: Clip?) {
        // actions small
        val notificationLayout = RemoteViews(app.packageName, R.layout.layout_notification_actions)
        bindActions(notificationLayout, clip)
        builder.setCustomContentView(notificationLayout)

        val clipText = clip?.text

        // actions big
        if (clipText != null) {
            val notificationLayoutBig = RemoteViews(app.packageName, R.layout.layout_notification_actions_big)
            notificationLayoutBig.removeAllViews(R.id.row1)
            notificationLayoutBig.removeAllViews(R.id.row2)
            notificationLayoutBig.removeAllViews(R.id.row3)
            notificationLayoutBig.removeAllViews(R.id.row4)
            val maxRows = 4
            val actionsInRow = 4
            val actions: MutableList<FastAction.ClipAction?> = appState.getVisibleClipActions(clipText.toLinkifiedSpannable())
                .filter { it.action != FastAction.MORE }
                .let { it.subList(0, min(actionsInRow * maxRows, it.size)) }
                .toMutableList()
            val rows = min(maxRows, actions.size / actionsInRow + 1)
            for (i in actions.size until rows * actionsInRow) {
                actions.add(null)
            }
            actions.forEachIndexed { index, clipAction ->
                val layoutId = when {
                    index < actionsInRow -> R.id.row1
                    index < actionsInRow * 2 -> R.id.row2
                    index < actionsInRow * 3 -> R.id.row3
                    else -> R.id.row4
                }
                val actionItem = RemoteViews(app.packageName, R.layout.item_notification_action)

                if (clipAction != null) {
                    val action = clipAction.action
                    if (action.isSmartAction()) {
                        applyActionAccentColor(actionItem, R.id.actionView)
                    } else {
                        applyActionColor(actionItem, R.id.actionView)
                    }
                    applyTextColor(actionItem, R.id.titleView)

                    val intentAction = FastActionProvider.Action(text = clipText, actionId = action.id, id = clip.getId())
                    val intent = intentActionFactory.getPendingIntent(intentAction)
                    actionItem.setImageViewResource(R.id.actionView, action.getIconRes())
                    actionItem.setOnClickPendingIntent(R.id.actionView, intent)
                    actionItem.setOnClickPendingIntent(R.id.contentView, intent)
                    actionItem.setTextViewText(R.id.titleView, clipAction.label)
                } else {
                    actionItem.setOnClickPendingIntent(R.id.contentView, createLaunchIntent())
                }

                notificationLayoutBig.addView(layoutId, actionItem)
            }
            notificationLayoutBig.setViewVisibility(R.id.row1, if (actions.isNotEmpty()) View.VISIBLE else View.GONE)
            notificationLayoutBig.setViewVisibility(R.id.row2, if (actions.size > actionsInRow) View.VISIBLE else View.GONE)
            notificationLayoutBig.setViewVisibility(R.id.row3, if (actions.size > actionsInRow * 2) View.VISIBLE else View.GONE)
            notificationLayoutBig.setViewVisibility(R.id.row4, if (actions.size > actionsInRow * 3) View.VISIBLE else View.GONE)
            bindActions(notificationLayoutBig, clip)
            builder.setCustomBigContentView(notificationLayoutBig)
        }

        updateHistoryLimit(HISTORY_LIMIT, 1)
    }

    private fun bindActions(remoteViews: RemoteViews, clip: Clip?) {
        val current = clip?.text ?: app.getString(R.string.notification_title_empty)
        remoteViews.setTextViewText(R.id.titleView, current.getFirst(appConfig.getNotificationTextMaxSize()))
        val currentItem = clip?.toStackItem()
        val history = prepareHistoryStack(currentItem)
        val indexOfCurrent = history.indexOf(currentItem)
        val nextItem: HistoryStackItem?
        val prevItem: HistoryStackItem?
        if (indexOfCurrent == -1) {
            nextItem = history.getOrNull(0)
            prevItem = history.getOrNull(history.size - 1)?.takeIf { it != currentItem }
        } else {
            nextItem = history.getOrNull(indexOfCurrent + 1)
                ?: history.getOrNull(0)?.takeIf { it != currentItem }
            prevItem = history.getOrNull(indexOfCurrent - 1)
                ?: history.getOrNull(history.size - 1)?.takeIf { it != currentItem }
        }
        applyTextColor(remoteViews, R.id.titleView)

        // prev
        if (prevItem == null) {
            setActionEnabled(remoteViews, R.id.actionPrev, false)
        } else {
            val intent = intentActionFactory.getPendingIntent(SetClipProvider.Action(prevItem.text, prevItem.id))
            remoteViews.setOnClickPendingIntent(R.id.actionPrev, intent)
            setActionEnabled(remoteViews, R.id.actionPrev, true)
        }

        // next
        if (nextItem == null) {
            setActionEnabled(remoteViews, R.id.actionNext, false)
        } else {
            val intent = intentActionFactory.getPendingIntent(SetClipProvider.Action(nextItem.text, nextItem.id))
            remoteViews.setOnClickPendingIntent(R.id.actionNext, intent)
            setActionEnabled(remoteViews, R.id.actionNext, true)
        }

        // divider
        remoteViews.setViewVisibility(R.id.actionDivider, if (nextItem == null && prevItem == null) View.GONE else View.VISIBLE)

        // counter
        val counterText = if (indexOfCurrent == -1) "" else (indexOfCurrent + 1).toString()
        remoteViews.setTextViewText(R.id.actionCounterView, counterText)
        applyTextColor(remoteViews, R.id.actionCounterView)

        // delete
        if (currentItem == null) {
            remoteViews.setViewVisibility(R.id.actionDelete, View.GONE)
        } else {
            val textToSetOnDelete = history.getOrNull(indexOfCurrent + 1) ?: prevItem ?: nextItem
            val intentAction = DeleteClipProvider.Action(
                deleteText = currentItem.text,
                deleteId = currentItem.id,
                setText = textToSetOnDelete?.text,
                setId = textToSetOnDelete?.id ?: 0L
            )
            val intent = intentActionFactory.getPendingIntent(intentAction)
            remoteViews.setOnClickPendingIntent(R.id.actionDelete, intent)
            remoteViews.setViewVisibility(R.id.actionDelete, View.VISIBLE)
            applyActionColor(remoteViews, R.id.actionDelete)
        }

        // open note
        val viewNoteIntent = intentActionFactory.getPendingIntent(AppViewNoteProvider.Action(currentItem?.text.notNull(), currentItem?.id ?: 0))
        remoteViews.setOnClickPendingIntent(R.id.titleView, viewNoteIntent)

        // controls
        val intent = intentActionFactory.getPendingIntent(ShowClipProvider.Action())
        remoteViews.setOnClickPendingIntent(R.id.actionClipboard, intent)
        remoteViews.setImageViewResource(R.id.actionClipboard, R.drawable.notification_action_take_24)
        applyActionColor(remoteViews, R.id.actionClipboard)

        if (clipboardState.isClipboardSupportedBySomehow()) {
            if (clipboardState.canTakeNoteFromClipboard()) {
                val pauseResumeIntent = intentActionFactory.getPendingIntent(PauseClipboardProvider.Action())
                remoteViews.setOnClickPendingIntent(R.id.actionStartStop, pauseResumeIntent)
                remoteViews.setImageViewResource(R.id.actionStartStop, R.drawable.notification_action_pause)
            } else {
                val pauseResumeIntent = intentActionFactory.getPendingIntent(ResumeClipboardProvider.Action())
                remoteViews.setOnClickPendingIntent(R.id.actionStartStop, pauseResumeIntent)
                remoteViews.setImageViewResource(R.id.actionStartStop, R.drawable.notification_action_start)
            }
            applyActionColor(remoteViews, R.id.actionStartStop)
            remoteViews.setViewVisibility(R.id.actionStartStop, View.VISIBLE)
        } else {
            remoteViews.setViewVisibility(R.id.actionStartStop, View.GONE)
        }

        applyBackgroundColor(remoteViews)
    }

    private fun prepareHistoryStack(currentItem: HistoryStackItem?): List<HistoryStackItem> {
        val historyStack = clipboardState.historyStack.requireValue()
        currentItem?.also { item ->
            if (item.isNew()) {
                item.id = historyStack.find { it.text == item.text }?.id ?: 0L
            }
            if (!historyStack.contains(item)) {
                val newHistory = LinkedHashSet<HistoryStackItem>(HISTORY_LIMIT)
                newHistory.add(item)
                historyStack.forEachIndexed { _, s ->
                    if (!s.isNew() && newHistory.size < HISTORY_LIMIT) {
                        newHistory.add(s)
                    }
                }
                clipboardState.historyStack.setValue(newHistory.toList())
            } else {
                clipboardState.historyStack.setValue(historyStack.filter { !it.isNew() })
            }
        }
        return clipboardState.historyStack.requireValue()
    }

    companion object {
        private const val HISTORY_LIMIT = 10
    }

}