package clipto.dynamic.values

import clipto.dynamic.DynamicValueContext
import clipto.dynamic.DynamicValueType
import java.util.*

class Hour12Value : AbstractDynamicValue(DynamicValueType.HOUR12.id) {

    override fun getValueUnsafe(context: DynamicValueContext): CharSequence {
        val date = Calendar.getInstance()
        date.time = Date()
        return date.get(Calendar.HOUR).toString()
    }

}