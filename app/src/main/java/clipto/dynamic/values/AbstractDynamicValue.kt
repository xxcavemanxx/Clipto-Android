package clipto.dynamic.values

import clipto.analytics.Analytics
import clipto.dynamic.DynamicValueContext
import clipto.dynamic.IDynamicValue

abstract class AbstractDynamicValue(private val id: String) : IDynamicValue {

    override fun getValue(context: DynamicValueContext): CharSequence {
        return try {
            getValueUnsafe(context)
        } catch (e: Exception) {
            Analytics.onError("DynamicValue:getValueUnsafe", e)
            id
        }
    }

    override fun getKey(): String = id

    abstract fun getValueUnsafe(context: DynamicValueContext): CharSequence

}