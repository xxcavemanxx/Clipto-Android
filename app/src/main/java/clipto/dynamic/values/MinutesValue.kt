package clipto.dynamic.values

import clipto.dynamic.DynamicValueContext
import clipto.dynamic.DynamicValueType
import java.util.*

class MinutesValue : AbstractDynamicValue(DynamicValueType.MINUTES.id) {

    override fun getValueUnsafe(context: DynamicValueContext): CharSequence {
        val date = Calendar.getInstance()
        date.time = Date()
        return date.get(Calendar.MINUTE).toString()
    }

}