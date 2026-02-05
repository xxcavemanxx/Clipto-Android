package clipto.dynamic.fields

import clipto.dynamic.DynamicField

class TextToggleDynamicField : DynamicField(ID) {

    var checked: Boolean = false

    var text: String? = null

    override fun getFieldValueUnsafe(): String? = text?.takeIf { checked }

    override fun hasValue(): Boolean = checked

    override fun apply(from: DynamicField) {
        if (from is TextToggleDynamicField) {
            checked = from.checked
            text = from.text
        }
    }

    override fun clear() {
        checked = false
    }

    companion object {
        const val ID = "formtoggle"
    }

}