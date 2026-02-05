package clipto.extensions

import clipto.domain.TimePeriod
import com.wb.clipboard.R

fun TimePeriod.getTitleRes(): Int {
    return when (this) {
        TimePeriod.CUSTOM_INTERVAL -> R.string.where_date_custom
        TimePeriod.CUSTOM_DATE -> R.string.where_date_custom
        TimePeriod.TODAY -> R.string.where_date_today
        TimePeriod.TOMORROW -> R.string.where_date_tomorrow
        TimePeriod.YESTERDAY -> R.string.where_date_yesterday
        TimePeriod.LAST_7_DAYS -> R.string.where_date_last_7_days
        TimePeriod.LAST_30_DAYS -> R.string.where_date_last_30_days
        TimePeriod.LAST_90_DAYS -> R.string.where_date_last_90_days
    }
}