package clipto.dynamic

import clipto.dynamic.values.*
import com.wb.clipboard.R

enum class DynamicValueType(val id: String, val titleRes: Int, protected val createDynamicValue: (id: String, level: Int) -> IDynamicValue) {

    PLATFORM("platform", R.string.clip_add_dynamic_value_platform, { _, _ -> PlatformInfoValue() }),
    IP_ADDRESS("ip_address", R.string.clip_add_dynamic_value_ip, { _, _ -> IpAddressValue() }),
    TIME("time", R.string.clip_add_dynamic_value_time, { _, _ -> TimeValue() }),
    DATE("date", R.string.clip_add_dynamic_value_date, { _, _ -> DateValue() }),
    DATE_TIME("date_time", R.string.clip_add_dynamic_value_date_time, { _, _ -> DateTimeValue() }),
    DAY_OF_WEEK("day_of_week", R.string.clip_add_dynamic_value_day_of_week, { _, _ -> DayOfWeekValue() }),
    DAY_OF_MONTH("day_of_month", R.string.clip_add_dynamic_value_day_of_month, { _, _ -> DayOfMonthValue() }),
    DAY_OF_MONTH_ZERO("day_of_month_zero", R.string.clip_add_dynamic_value_day_of_month_zero, { _, _ -> DayOfMonthWithLeadingZeroValue() }),
    YEAR("year", R.string.clip_add_dynamic_value_year, { _, _ -> YearValue() }),
    YEAR_SHORT("year_short", R.string.clip_add_dynamic_value_year_short, { _, _ -> YearShortValue() }),
    HOUR24("hour24", R.string.clip_add_dynamic_value_hour24, { _, _ -> Hour24Value() }),
    HOUR12("hour12", R.string.clip_add_dynamic_value_hour12, { _, _ -> Hour12Value() }),
    MINUTES("minutes", R.string.clip_add_dynamic_value_minutes, { _, _ -> MinutesValue() }),
    SECONDS("seconds", R.string.clip_add_dynamic_value_seconds, { _, _ -> SecondsValue() }),
    AM_PM("am_pm", R.string.clip_add_dynamic_value_am_pm, { _, _ -> AmPmValue() }),
    TIMEZONE("timezone", R.string.clip_add_dynamic_value_timezone, { _, _ -> TimeZoneValue() }),
    MONTH("month", R.string.clip_add_dynamic_value_month, { _, _ -> MonthValue() }),
    MONTH_NUMBER("month_number", R.string.clip_add_dynamic_value_month_number, { _, _ -> MonthNumberValue() }),
    MONTH_NUMBER_ZERO("month_number_zero", R.string.clip_add_dynamic_value_month_number_zero, { _, _ -> MonthNumberWithLeadingZeroValue() }),
    RANDOM_DIGIT("random_digit", R.string.clip_add_dynamic_value_random_digit, { _, _ -> RandomDigitValue() }),
    RANDOM_LATIN("random_latin", R.string.clip_add_dynamic_value_random_latin, { _, _ -> RandomLatinCharacterValue() }),

    SNIPPET("snippet:", R.string.clip_add_snippet_title, { id, level -> NoteTextValue(id, level) }) {
        override fun isValid(id: String): Boolean = id.startsWith(this.id)
        override fun getPlaceholderValue(params: String?): String = DynamicField.getPlaceholder("$id${params}")
        override fun getValue(id: String): String = id.substring(this.id.length).trim()
        override fun getDynamicValue(id: String, level: Int): IDynamicValue = createDynamicValue.invoke(id, level)
    }
    ;

    private var cached: IDynamicValue? = null
    open fun getDynamicValue(id: String, level: Int): IDynamicValue {
        val instance: IDynamicValue = cached ?: createDynamicValue.invoke(id, level)
        cached = instance
        return instance
    }

    open fun isValid(id: String): Boolean = id == this.id
    open fun getPlaceholderValue(params: String? = null) = DynamicField.getPlaceholder(id)
    open fun getValue(id: String) = id

    companion object {
        val commonValues: Map<String, DynamicValueType> = listOf(
                DATE_TIME,
                DATE,
                TIME,
                DAY_OF_WEEK,
                DAY_OF_MONTH,
                DAY_OF_MONTH_ZERO,
                MONTH,
                MONTH_NUMBER,
                MONTH_NUMBER_ZERO,
                YEAR,
                YEAR_SHORT,
                HOUR24,
                HOUR12,
                MINUTES,
                SECONDS,
                AM_PM,
                TIMEZONE,
                IP_ADDRESS,
                PLATFORM,
                RANDOM_DIGIT,
                RANDOM_LATIN
        ).map { it.id to it }.toMap(LinkedHashMap())
    }

}