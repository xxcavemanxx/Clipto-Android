package clipto.dynamic.fields

import clipto.dynamic.DynamicField
import clipto.dynamic.DynamicValue

class UnknownDynamicField(id: String) : DynamicField(id) {

    override fun getFieldValueUnsafe(): String? = null

    override fun apply(from: DynamicField) = Unit

    override fun hasValue(): Boolean = false

    override fun clear() = Unit

    companion object {
        const val ID = "formunknown"
    }

}