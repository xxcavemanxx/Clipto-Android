package clipto.dynamic.values

import clipto.dynamic.DynamicValueContext
import clipto.dynamic.DynamicValueType
import java.util.*

class DayOfMonthWithLeadingZeroValue : AbstractDynamicValue(
        DynamicValueType.DAY_OF_MONTH_ZERO.id
) {

    override fun getValueUnsafe(context: DynamicValueContext): CharSequence {
        val date = Calendar.getInstance()
        date.time = Date()
        val value = date.get(Calendar.DAY_OF_MONTH)
        if (value < 10) {
            return "0$value"
        }
        return value.toString()
    }

}