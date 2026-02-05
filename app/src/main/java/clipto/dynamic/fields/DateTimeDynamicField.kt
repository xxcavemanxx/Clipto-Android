package clipto.dynamic.fields

import clipto.common.misc.FormatUtils
import clipto.domain.TimePeriod
import clipto.dynamic.DynamicField
import java.util.*

class DateTimeDynamicField : DynamicField(ID) {

    var value: String? = null

    var date: Date? = null

    var format: String? = null

    override fun getFieldValueUnsafe(): String? =
        FormatUtils.formatDate(date ?: TimePeriod.byCode(value)?.toInterval()?.from, format)
            .takeIf { it.isNotBlank() }

    override fun apply(from: DynamicField) {
        if (from is DateTimeDynamicField) {
            date = from.date
        }
    }

    override fun hasValue(): Boolean = date != null || value != null

    override fun clear() {
        date = null
    }

    companion object {
        const val ID = "formdate"
    }

}