package clipto.dynamic.values

import clipto.dynamic.DynamicValueContext
import clipto.dynamic.DynamicValueType
import java.util.*

class DayOfMonthValue : AbstractDynamicValue(DynamicValueType.DAY_OF_MONTH.id) {

    override fun getValueUnsafe(context: DynamicValueContext): CharSequence {
        val date = Calendar.getInstance()
        date.time = Date()
        return date.get(Calendar.DAY_OF_MONTH).toString()
    }

}