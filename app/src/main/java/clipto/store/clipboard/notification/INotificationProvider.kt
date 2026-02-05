package clipto.store.clipboard.notification

import android.app.Notification
import clipto.domain.Clip

interface INotificationProvider {

    fun provide(channelId: String, clip: Clip?): Notification

}