package clipto.domain

import clipto.extensions.withDate
import clipto.extensions.withDays
import clipto.extensions.withSeconds
import clipto.extensions.withoutTime
import com.google.gson.annotations.SerializedName
import java.util.*

enum class TimePeriod(val id: Int, val code: String? = null) {
    @SerializedName("0")
    TODAY(0, "today") {
        override fun toInterval(from: Date?, to: Date?): TimeInterval {
            return TimeInterval(
                period = this,
                from = Calendar.getInstance().withDate().withoutTime().time
            )
        }
    },

    @SerializedName("1")
    YESTERDAY(1, "yesterday") {
        override fun toInterval(from: Date?, to: Date?): TimeInterval {
            return TimeInterval(
                period = this,
                from = Calendar.getInstance().withDate().withoutTime().withDays(-1).time,
                to = Calendar.getInstance().withDate().withoutTime().withSeconds(-1).time
            )
        }
    },

    @SerializedName("2")
    LAST_7_DAYS(2) {
        override fun toInterval(from: Date?, to: Date?): TimeInterval {
            return TimeInterval(
                period = this,
                from = Calendar.getInstance().withDate().withoutTime().withDays(-7).time
            )
        }
    },

    @SerializedName("3")
    LAST_30_DAYS(3) {
        override fun toInterval(from: Date?, to: Date?): TimeInterval {
            return TimeInterval(
                period = this,
                from = Calendar.getInstance().withDate().withoutTime().withDays(-30).time
            )
        }
    },

    @SerializedName("4")
    LAST_90_DAYS(4) {
        override fun toInterval(from: Date?, to: Date?): TimeInterval {
            return TimeInterval(
                period = this,
                from = Calendar.getInstance().withDate().withoutTime().withDays(-90).time
            )
        }
    },

    @SerializedName("5")
    CUSTOM_INTERVAL(5) {
        override fun toInterval(from: Date?, to: Date?): TimeInterval? {
            return when {
                from == null && to == null -> null
                from != null && to != null && from.after(to) -> TimeInterval(this, to, from)
                else -> TimeInterval(this, from, to)
            }
        }
    },

    @SerializedName("6")
    CUSTOM_DATE(6) {
        override fun toInterval(from: Date?, to: Date?): TimeInterval? {
            return when {
                from == null && to == null -> null
                else -> TimeInterval(this, from, null)
            }
        }
    },

    @SerializedName("7")
    TOMORROW(7, "tomorrow") {
        override fun toInterval(from: Date?, to: Date?): TimeInterval {
            return TimeInterval(
                period = this,
                from = Calendar.getInstance().withDate().withoutTime().withDays(1).time
            )
        }
    }
    ;

    abstract fun toInterval(from: Date? = null, to: Date? = null): TimeInterval?

    companion object {
        val periods = arrayOf(
            CUSTOM_INTERVAL,
            TODAY,
            YESTERDAY,
            LAST_7_DAYS,
            LAST_30_DAYS,
            LAST_90_DAYS
        )

        val datePeriods = arrayOf(
            YESTERDAY,
            TODAY,
            TOMORROW
        )

        fun byId(id: Int?): TimePeriod? = when (id) {
            TODAY.id -> TODAY
            YESTERDAY.id -> YESTERDAY
            LAST_7_DAYS.id -> LAST_7_DAYS
            LAST_30_DAYS.id -> LAST_30_DAYS
            LAST_90_DAYS.id -> LAST_90_DAYS
            CUSTOM_INTERVAL.id -> CUSTOM_INTERVAL
            CUSTOM_DATE.id -> CUSTOM_DATE
            TOMORROW.id -> TOMORROW
            else -> null
        }

        fun byCode(code: String?): TimePeriod? = when (code) {
            TODAY.code -> TODAY
            YESTERDAY.code -> YESTERDAY
            TOMORROW.code -> TOMORROW
            else -> null
        }
    }
}