package clipto.store.clipboard.notification

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import clipto.analytics.Analytics
import clipto.config.IAppConfig
import clipto.domain.Clip
import clipto.domain.NotificationStyle
import clipto.store.app.AppState
import clipto.store.clipboard.ClipboardState
import com.wb.clipboard.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardNotificationManager @Inject constructor(
    private val app: Application,
    private val appState: AppState,
    private val appConfig: IAppConfig,
    private val clipboardState: ClipboardState,
    private val defaultNotificationProvider: DefaultNotificationProvider,
    private val historyNotificationProvider: HistoryNotificationProvider,
    private val actionsNotificationProvider: ActionsNotificationProvider,
    private val controlsNotificationProvider: ControlsNotificationProvider
) {

    private val notificationManager by lazy { app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private val channelId: String by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, app.getString(R.string.settings_track_clipboard_title), NotificationManager.IMPORTANCE_DEFAULT)
                channel.lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
                if (appConfig.canSilentlyVibrateOnClipboardChanges()) {
                    channel.vibrationPattern = longArrayOf(0L)
                    channel.enableVibration(true)
                }
                channel.setShowBadge(false)
                notificationManager.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            Analytics.onError("error_create_notification_channel", e)
        }
        CHANNEL_ID
    }

    fun notify(service: Service, clip: Clip?) {
        val provider = when (appState.getSettings().notificationStyle) {
            NotificationStyle.ACTIONS -> actionsNotificationProvider
            NotificationStyle.CONTROLS -> controlsNotificationProvider
            NotificationStyle.HISTORY -> historyNotificationProvider
            else -> defaultNotificationProvider
        }
        appState.getTheme()
        val notification = provider.provide(channelId, clip)
        service.startForeground(NOTIFICATION_ID, notification)
    }

    fun notify(service: Service) {
        val clip = clipboardState.clip.getValue()
        notify(service, clip)
    }

    fun cancel() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    companion object {
        private const val CHANNEL_ID = "track_clipboard"
        private const val NOTIFICATION_ID = 1234
    }

}