package clipto.dynamic.values

import clipto.dynamic.DynamicValueContext
import clipto.dynamic.DynamicValueType
import java.text.DateFormatSymbols
import java.util.*

class DayOfWeekValue : AbstractDynamicValue(DynamicValueType.DAY_OF_WEEK.id) {

    override fun getValueUnsafe(context: DynamicValueContext): CharSequence {
        val date = Calendar.getInstance()
        date.time = Date()
        return DateFormatSymbols.getInstance().weekdays[date.get(Calendar.DAY_OF_WEEK)]
    }

}