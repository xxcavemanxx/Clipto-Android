package clipto.dynamic.values

import clipto.common.misc.AndroidUtils
import clipto.dynamic.DynamicValueContext
import clipto.dynamic.DynamicValueType

class IpAddressValue : AbstractDynamicValue(DynamicValueType.IP_ADDRESS.id) {

    override fun getValueUnsafe(context: DynamicValueContext): CharSequence {
        return AndroidUtils.getIpAddress(true)
    }

}