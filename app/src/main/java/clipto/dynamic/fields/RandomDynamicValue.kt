package clipto.dynamic.fields

import clipto.dynamic.DynamicField
import clipto.dynamic.DynamicValue
import clipto.dynamic.models.RandomType

class RandomDynamicValue : DynamicValue(ID) {

    var options: List<String> = emptyList()
    var type: String = RandomType.DIGIT.type
        set(value) {
            field = value
            val labelType = RandomType.byId(label)
            val randomType = RandomType.byIdOrDefault(value)
            if (label.isNullOrEmpty() || (labelType != null && labelType != randomType)) {
                label = value
            }
        }

    override fun getFieldValueUnsafe(): String = RandomType.byIdOrDefault(type).valueProvider.invoke(options)
    override fun apply(from: DynamicField) {
        if (from is RandomDynamicValue) {
            options = from.options
        }
    }

    override fun hasValue(): Boolean = true
    override fun clear() = Unit

    companion object {
        const val ID = "random"
    }

}