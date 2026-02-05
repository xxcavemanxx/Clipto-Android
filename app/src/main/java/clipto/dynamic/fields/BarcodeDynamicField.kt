package clipto.dynamic.fields

import clipto.common.extensions.toNullIfEmpty
import clipto.dynamic.DynamicField
import clipto.dynamic.models.ValueFormatterType

class BarcodeDynamicField : DynamicField(ID) {

    var multiple: Boolean = false

    var formatter: String = ValueFormatterType.COMMA.separator

    var values: List<String> = emptyList()

    override fun getFieldValueUnsafe(): String? = values
            .let {
                val predefined = ValueFormatterType.getByPlaceholder(formatter)
                predefined?.formatter?.invoke(it) ?: it.joinToString(formatter)
            }
            .toNullIfEmpty()

    override fun apply(from: DynamicField) {
        if (from is BarcodeDynamicField) {
            values = from.values
        }
    }

    override fun hasValue(): Boolean = values.isNotEmpty()

    override fun clear() {
        values = emptyList()
    }

    companion object {
        const val ID = "formbarcode"
    }

}