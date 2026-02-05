package clipto.extensions

import android.content.Context
import android.graphics.Typeface
import android.text.style.StyleSpan
import clipto.common.misc.FormatUtils
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.domain.TimeInterval
import clipto.domain.TimePeriod
import com.wb.clipboard.R

fun TimeInterval.getLabel(context: Context): CharSequence? {
    return when (period) {
        TimePeriod.TODAY -> {
            context.getString(R.string.where_date_today)
        }
        TimePeriod.TOMORROW -> {
            context.getString(R.string.where_date_tomorrow)
        }
        TimePeriod.YESTERDAY -> {
            context.getString(R.string.where_date_yesterday)
        }
        TimePeriod.LAST_7_DAYS -> {
            context.getString(R.string.where_date_last_7_days)
        }
        TimePeriod.LAST_30_DAYS -> {
            context.getString(R.string.where_date_last_30_days)
        }
        TimePeriod.LAST_90_DAYS -> {
            context.getString(R.string.where_date_last_90_days)
        }
        TimePeriod.CUSTOM_INTERVAL -> {
            return when {
                from != null && to != null -> {
                    SimpleSpanBuilder()
                            .append(context.getString(R.string.where_from))
                            .append(" ")
                            .append(FormatUtils.formatDateTimeShort(from), StyleSpan(Typeface.BOLD))
                            .append("\n")
                            .append(context.getString(R.string.where_to))
                            .append(" ")
                            .append(FormatUtils.formatDateTimeShort(to), StyleSpan(Typeface.BOLD))
                            .build()
                }
                from != null -> {
                    SimpleSpanBuilder()
                            .append(context.getString(R.string.where_after))
                            .append(" ")
                            .append(FormatUtils.formatDateTimeShort(from), StyleSpan(Typeface.BOLD))
                            .build()
                }
                to != null -> {
                    SimpleSpanBuilder()
                            .append(context.getString(R.string.where_before))
                            .append(" ")
                            .append(FormatUtils.formatDateTimeShort(to), StyleSpan(Typeface.BOLD))
                            .build()
                }
                else -> null
            }
        }
        TimePeriod.CUSTOM_DATE -> {
            return FormatUtils.formatDateTimeShort(from)
        }
    }
}