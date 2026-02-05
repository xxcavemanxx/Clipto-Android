package clipto.dao.firebase.mapper

import clipto.analytics.Analytics
import clipto.common.misc.GsonUtils
import clipto.dao.firebase.FirebaseDaoHelper
import com.google.firebase.Timestamp
import java.util.*

object DateMapper {

    fun toTimestamp(date: Date?, withNulls: Boolean = false): Any? {
        return if (date != null) {
            return try {
                Timestamp(date)
            } catch (th: Throwable) {
                Analytics.onError("error_to_timestamp", th)
                FirebaseDaoHelper.getServerTimestamp()
            }
        } else if (!withNulls) {
            FirebaseDaoHelper.getServerTimestamp()
        } else {
            null
        }
    }

    fun toDate(any: Any?): Date? {
        if (any == null) {
            return null
        }
        if (any is String) {
            return GsonUtils.parseDate(any)
        }
        if (any is Timestamp) {
            return any.toDate()
        }
        if (any is Map<*, *>) {
            val seconds = any["_seconds"]
            val nanoseconds = any["_nanoseconds"]
            if (seconds is Number && nanoseconds is Number) {
                return Timestamp(seconds.toLong(), nanoseconds.toInt()).toDate()
            }
        }
        return null
    }

}