package clipto.dynamic.values

import clipto.dynamic.DynamicValueType
import clipto.common.misc.FormatUtils
import clipto.dynamic.DynamicValueContext
import java.util.*

class TimeValue : AbstractDynamicValue(DynamicValueType.TIME.id) {

    override fun getValueUnsafe(context: DynamicValueContext): CharSequence {
        return FormatUtils.formatTime(Date())
    }

}