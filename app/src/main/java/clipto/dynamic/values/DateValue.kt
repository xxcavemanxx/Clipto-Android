package clipto.dynamic.values

import clipto.common.misc.FormatUtils
import clipto.dynamic.DynamicValueContext
import clipto.dynamic.DynamicValueType
import java.util.*

class DateValue : AbstractDynamicValue(DynamicValueType.DATE.id) {

    override fun getValueUnsafe(context: DynamicValueContext): CharSequence {
        return FormatUtils.formatDate(Date())
    }

}