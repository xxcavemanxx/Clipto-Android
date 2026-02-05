package clipto.presentation.runes.keyboard_companion

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import clipto.analytics.Analytics
import com.wb.clipboard.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanionNotificationManager @Inject constructor(private val app: Application) {

    private val notificationManager: NotificationManager by lazy { app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val contentTitle by lazy { app.getString(R.string.runes_texpander_title) }
    private val contentText by lazy { app.getString(R.string.runes_texpander_action_show) }
    private val channelId by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                        CHANNEL_ID,
                        app.getString(R.string.runes_texpander_title),
                        NotificationManager.IMPORTANCE_LOW)
                channel.lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
                channel.setShowBadge(false)
                notificationManager.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            Analytics.onError("error_create_companion_channel", e)
        }
        CHANNEL_ID
    }

    fun start() {
        CompanionService.createShowIntent(app).send()
    }

    fun hide() {
        runCatching { notificationManager.cancel(NOTIFICATION_ID) }
    }

    fun show() {
        runCatching {
            val actionIntent = CompanionService.createShowIntent(app)
            val notification =
                    NotificationCompat.Builder(app, channelId)
                            .setSmallIcon(R.drawable.rune_keyboard_companion)
                            .setContentTitle(contentTitle)
                            .setContentText(contentText)
                            .setContentIntent(actionIntent)
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                            .setCategory(NotificationCompat.CATEGORY_SERVICE)
                            .setNotificationSilent()
                            .setChannelId(channelId)
                            .build()
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }


    companion object {
        private const val CHANNEL_ID = "keyboard_companion"
        private const val NOTIFICATION_ID = 1300
    }

}
