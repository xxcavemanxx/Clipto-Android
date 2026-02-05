package clipto.dynamic.values

import clipto.common.misc.IdUtils
import clipto.dynamic.DynamicValueContext
import clipto.dynamic.DynamicValueType

class RandomLatinCharacterValue : AbstractDynamicValue(DynamicValueType.RANDOM_LATIN.id) {

    private val symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

    override fun getValueUnsafe(context: DynamicValueContext): CharSequence {
        return symbols[IdUtils.rand.nextInt(symbols.length)].toString()
    }

}