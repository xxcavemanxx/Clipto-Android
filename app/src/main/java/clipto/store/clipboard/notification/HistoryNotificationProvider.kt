package clipto.store.clipboard.notification

import android.graphics.Typeface
import android.text.style.StyleSpan
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import clipto.action.intent.provider.AppViewNoteProvider
import clipto.action.intent.provider.ClearCopyProvider
import clipto.action.intent.provider.CopyProvider
import clipto.common.extensions.getFirst
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.domain.Clip
import clipto.store.clipboard.data.HistoryStackItem
import clipto.store.clipboard.data.toStackItem
import com.wb.clipboard.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryNotificationProvider @Inject constructor() : AbstractNotificationProvider() {

    override fun modify(builder: NotificationCompat.Builder, clip: Clip?) {
        val notificationLayout = RemoteViews(app.packageName, R.layout.layout_notification_controls)
        bindControls(notificationLayout)
        builder.setCustomContentView(notificationLayout)

        val currentItem = clip?.toStackItem()
        val historyStack = prepareHistoryStack(currentItem)
        val notificationLayoutBig = RemoteViews(app.packageName, R.layout.layout_notification_history)
        notificationLayoutBig.removeAllViews(R.id.notification_list)
        historyStack.forEachIndexed { index, s ->
            if (index < HISTORY_LIMIT) {
                val notificationItem = RemoteViews(app.packageName, R.layout.item_notification_history)
                val title = s.text.getFirst(appConfig.getNotificationTextMaxSize())

                if (s == currentItem) {
                    val text: CharSequence = SimpleSpanBuilder().append(title, StyleSpan(Typeface.BOLD)).build()
                    notificationItem.setTextViewText(R.id.titleView, text)
                    val intent = intentActionFactory.getPendingIntent(ClearCopyProvider.Action())
                    notificationItem.setOnClickPendingIntent(R.id.actionCopy, intent)
                    notificationItem.setImageViewResource(R.id.actionCopy, R.drawable.notification_action_clear)
                } else {
                    notificationItem.setTextViewText(R.id.titleView, title)
                    val intent = intentActionFactory.getPendingIntent(CopyProvider.Action(s.text, s.id))
                    notificationItem.setOnClickPendingIntent(R.id.actionCopy, intent)
                    notificationItem.setImageViewResource(R.id.actionCopy, R.drawable.notification_action_copy)
                }
                val intent = intentActionFactory.getPendingIntent(AppViewNoteProvider.Action(s.text, s.id))
                notificationItem.setOnClickPendingIntent(R.id.contentView, intent)

                applyTextColor(notificationItem, R.id.titleView)
                applyActionColor(notificationItem, R.id.actionCopy)

                notificationLayoutBig.addView(R.id.notification_list, notificationItem)
            }
        }
        bindControls(notificationLayoutBig)
        builder.setCustomBigContentView(notificationLayoutBig)

        updateHistoryLimit(HISTORY_LIMIT)
    }

    private fun prepareHistoryStack(currentItem: HistoryStackItem?): List<HistoryStackItem> {
        val historyStack = clipboardState.historyStack.requireValue()
        currentItem?.also { item ->
            if (item.isNew()) {
                item.id = historyStack.find { it.text == item.text }?.id ?: 0L
            }
            val newHistory = LinkedHashSet<HistoryStackItem>(HISTORY_LIMIT)
            newHistory.add(item)
            historyStack.forEachIndexed { _, s ->
                if (!s.isNew() && newHistory.size < HISTORY_LIMIT) {
                    newHistory.add(s)
                }
            }
            clipboardState.historyStack.setValue(newHistory.toList())
        }
        return clipboardState.historyStack.requireValue()
    }

    companion object {
        private const val HISTORY_LIMIT = 4
    }

}