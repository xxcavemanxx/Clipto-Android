package clipto.dynamic.fields

import clipto.common.misc.FormatUtils
import clipto.dynamic.DynamicField
import clipto.dynamic.DynamicValue
import java.util.*

class DateTimeDynamicValue : DynamicValue(ID) {

    var format: String? = null

    override fun getFieldValueUnsafe(): String? = FormatUtils.formatDate(Date(), format).takeIf { it.isNotBlank() }
    override fun apply(from: DynamicField) = Unit
    override fun hasValue(): Boolean = true
    override fun clear() = Unit

    companion object {
        const val ID = "date"
    }

}