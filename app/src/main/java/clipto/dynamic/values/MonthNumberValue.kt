package clipto.dynamic.values

import clipto.dynamic.DynamicValueContext
import clipto.dynamic.DynamicValueType
import java.util.*

class MonthNumberValue : AbstractDynamicValue(DynamicValueType.MONTH_NUMBER.id) {

    override fun getValueUnsafe(context: DynamicValueContext): CharSequence {
        val date = Calendar.getInstance()
        date.time = Date()
        return (date.get(Calendar.MONTH) + 1).toString()
    }

}