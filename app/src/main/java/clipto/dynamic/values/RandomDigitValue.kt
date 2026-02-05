package clipto.dynamic.values

import clipto.dynamic.DynamicValueType
import clipto.common.misc.IdUtils
import clipto.dynamic.DynamicValueContext

class RandomDigitValue : AbstractDynamicValue(DynamicValueType.RANDOM_DIGIT.id) {

    override fun getValueUnsafe(context: DynamicValueContext): CharSequence {
        val symbols = "0123456789"
        return symbols[IdUtils.rand.nextInt(symbols.length)].toString()
    }

}