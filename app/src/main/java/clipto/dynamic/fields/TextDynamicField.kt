package clipto.dynamic.fields

import clipto.dynamic.DynamicField

class TextDynamicField : DynamicField(ID) {

    var value: String? = null

    var maxLength: Int? = null

    var clipboard: Boolean = false

    var multiLine: Boolean = false

    fun getMaxLines(): Int = if (multiLine) Integer.MAX_VALUE else 1

    fun getMaxLength(): Int = maxLength ?: Integer.MAX_VALUE

    override fun getFieldValueUnsafe(): String? = value

    override fun hasValue(): Boolean = value != null

    override fun apply(from: DynamicField) {
        if (from is TextDynamicField) {
            value = from.value
        }
    }

    override fun clear() {
        value = null
    }

    companion object {
        const val ID = "formtext"
    }

}