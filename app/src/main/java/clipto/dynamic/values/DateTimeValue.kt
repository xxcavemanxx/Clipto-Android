package clipto.dynamic.values

import clipto.dynamic.DynamicValueType
import clipto.common.misc.FormatUtils
import clipto.dynamic.DynamicValueContext
import java.util.*

class DateTimeValue : AbstractDynamicValue(DynamicValueType.DATE_TIME.id) {

    override fun getValueUnsafe(context: DynamicValueContext): CharSequence {
        return FormatUtils.formatDateTime(Date())
    }

}