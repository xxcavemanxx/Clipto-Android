package clipto.store.clipboard.notification

import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import clipto.domain.Clip
import com.wb.clipboard.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ControlsNotificationProvider @Inject constructor() : AbstractNotificationProvider() {

    override fun modify(builder: NotificationCompat.Builder, clip: Clip?) {
        val notificationLayout = RemoteViews(app.packageName, R.layout.layout_notification_controls)
        bindControls(notificationLayout)
        builder.setCustomContentView(notificationLayout)
    }

}