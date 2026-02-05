package clipto.dynamic.fields

import clipto.common.misc.FormatUtils
import clipto.dynamic.DynamicField
import clipto.dynamic.DynamicValue

class ReferenceDynamicValue(var ref: DynamicField? = null) : DynamicField(ID) {

    var refName: String? = null

    var intrinsic: Boolean = false

    override fun getFieldLabel(): String {
        val labelRef = label
        if (labelRef != null) {
            return labelRef
        }
        if (intrinsic) {
            return ref?.getFieldLabel() ?: FormatUtils.UNKNOWN
        }
        return "= ${ref?.getFieldLabel() ?: FormatUtils.UNKNOWN}"
    }

    override fun getFieldValueUnsafe(): String? = ref?.getFieldValue()
    override fun hasValue(): Boolean = ref?.hasValue() == true
    override fun apply(from: DynamicField) = Unit
    override fun clear() = Unit

    companion object {
        const val ID = "formref"
    }

}