package clipto.dynamic.values

import clipto.dynamic.DynamicValueContext
import clipto.dynamic.DynamicValueType
import java.text.DateFormatSymbols
import java.util.*

class AmPmValue : AbstractDynamicValue(DynamicValueType.AM_PM.id) {

    override fun getValueUnsafe(context: DynamicValueContext): CharSequence {
        val date = Calendar.getInstance()
        date.time = Date()
        val value = date.get(Calendar.AM_PM)
        return DateFormatSymbols.getInstance().amPmStrings[value]
    }

}