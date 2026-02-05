package clipto.dynamic.fields

import clipto.dynamic.*

class LegacyValueDynamicField(
        val context: DynamicValueContext,
        val dynamicType: DynamicValueType,
        val dynamicValue: IDynamicValue,
        id: String
) : DynamicValue(id) {

    var value: String? = null

    override fun apply(from: DynamicField) {
        if (from is LegacyValueDynamicField) {
            value = from.value
        }
    }

    override fun hasValue(): Boolean = true

    override fun clear() {
        value = null
    }

    override fun getFieldValueUnsafe(): String? {
        val valueRef = value
        if (valueRef == null) {
            value = dynamicValue.getValue(context).toString()
        }
        return value
    }
}