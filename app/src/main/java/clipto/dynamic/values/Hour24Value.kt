package clipto.dynamic.values

import clipto.dynamic.DynamicValueContext
import clipto.dynamic.DynamicValueType
import java.util.*

class Hour24Value : AbstractDynamicValue(DynamicValueType.HOUR24.id) {

    override fun getValueUnsafe(context: DynamicValueContext): CharSequence {
        val date = Calendar.getInstance()
        date.time = Date()
        return date.get(Calendar.HOUR_OF_DAY).toString()
    }

}