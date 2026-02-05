package clipto.store.clipboard.notification

import androidx.core.app.NotificationCompat
import clipto.domain.Clip
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultNotificationProvider @Inject constructor() : AbstractNotificationProvider() {

    override fun modify(builder: NotificationCompat.Builder, clip: Clip?) = Unit

}