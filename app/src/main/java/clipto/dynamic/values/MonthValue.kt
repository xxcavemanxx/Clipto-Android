package clipto.dynamic.values

import clipto.dynamic.DynamicValueContext
import clipto.dynamic.DynamicValueType
import java.text.DateFormatSymbols
import java.util.*

class MonthValue : AbstractDynamicValue(DynamicValueType.MONTH.id) {

    override fun getValueUnsafe(context: DynamicValueContext): CharSequence {
        val date = Calendar.getInstance()
        date.time = Date()
        return DateFormatSymbols.getInstance().months[date.get(Calendar.MONTH)]
    }

}