package clipto.dynamic.values

import clipto.dynamic.DynamicValueContext
import clipto.dynamic.DynamicValueType
import java.util.*

class MonthNumberWithLeadingZeroValue : AbstractDynamicValue(
        DynamicValueType.MONTH_NUMBER_ZERO.id
) {

    override fun getValueUnsafe(context: DynamicValueContext): CharSequence {
        val date = Calendar.getInstance()
        date.time = Date()
        val value = (date.get(Calendar.MONTH) + 1)
        if (value < 10) {
            return "0$value"
        }
        return value.toString()
    }

}