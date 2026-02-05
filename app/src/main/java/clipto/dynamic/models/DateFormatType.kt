package clipto.dynamic.models

import android.content.res.Resources
import clipto.common.misc.FormatUtils
import com.wb.clipboard.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

enum class DateFormatType(val titleRes: Int, private val format: () -> String?) {

    DATE_TIME(R.string.dynamic_field_date_format_date_time, {
        val locale = getLocale()
        val format = SimpleDateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale) as SimpleDateFormat
        format.toLocalizedPattern()
    }),

    DATE(R.string.dynamic_field_date_format_date, {
        val locale = getLocale()
        val format = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM, locale) as SimpleDateFormat
        format.toLocalizedPattern()
    }),

    TIME(R.string.dynamic_field_date_format_time, {
        val locale = getLocale()
        val format = SimpleDateFormat.getTimeInstance(DateFormat.MEDIUM, locale) as SimpleDateFormat
        format.toLocalizedPattern()
    }),

    YEAR(R.string.dynamic_field_date_format_year, {
        "yyyy"
    }),

    YEAR_SHORT(R.string.dynamic_field_date_format_year_short, {
        "yy"
    }),

    MONTH(R.string.dynamic_field_date_format_month, {
        "MMMM"
    }),

    MONTH_NUMBER(R.string.dynamic_field_date_format_month_number, {
        "M"
    }),

    MONTH_NUMBER_ZERO(R.string.dynamic_field_date_format_month_number_zero, {
        "MM"
    }),

    DAY_OF_MONTH(R.string.dynamic_field_date_format_day_of_month, {
        "d"
    }),

    DAY_OF_MONTH_ZERO(R.string.dynamic_field_date_format_day_of_month_zero, {
        "dd"
    }),

    HOUR_12(R.string.dynamic_field_date_format_hour_12, {
        "h"
    }),

    HOUR_12_ZERO(R.string.dynamic_field_date_format_hour_12_zero, {
        "hh"
    }),

    HOUR_24(R.string.dynamic_field_date_format_hour_24, {
        "H"
    }),

    HOUR_24_ZERO(R.string.dynamic_field_date_format_hour_24_zero, {
        "HH"
    }),

    MINUTE(R.string.dynamic_field_date_format_minute, {
        "m"
    }),

    MINUTE_ZERO(R.string.dynamic_field_date_format_minute_zero, {
        "mm"
    }),

    SECOND(R.string.dynamic_field_date_format_second, {
        "s"
    }),

    SECOND_ZERO(R.string.dynamic_field_date_format_second_zero, {
        "ss"
    }),

    AM_PM(R.string.dynamic_field_date_format_am_pm, {
        "aa"
    }),

    DAY_OF_WEEK(R.string.dynamic_field_date_format_day_of_week, {
        "EEEE"
    }),

    TIME_ZONE(R.string.dynamic_field_date_format_time_zone, {
        "zzz"
    }),
    ;

    private var formatCached: String? = null

    fun getFormatPattern(): String? {
        val formatValue = formatCached
        if (formatValue == null) {
            formatCached = runCatching(format).getOrNull()
        }
        return formatCached
    }

    fun formatDate(date: Date?): String? = FormatUtils.formatDate(date, format.invoke()).takeIf { it.isNotBlank() }

    companion object {
        fun getLocale(): Locale = runCatching { Resources.getSystem().configuration.locale }.getOrDefault(Locale.ROOT)

        fun getByPattern(pattern: String?): DateFormatType? = values().find { it.getFormatPattern() == pattern }
    }

}