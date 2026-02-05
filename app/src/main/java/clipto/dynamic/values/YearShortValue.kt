package clipto.dynamic.values

import clipto.dynamic.DynamicValueContext
import clipto.dynamic.DynamicValueType
import java.util.*

class YearShortValue : AbstractDynamicValue(DynamicValueType.YEAR_SHORT.id) {

    override fun getValueUnsafe(context: DynamicValueContext): CharSequence {
        val date = Calendar.getInstance()
        date.time = Date()
        return date.get(Calendar.YEAR).toString().takeLast(2)
    }

}