package clipto.dynamic.fields

import clipto.dynamic.DynamicField

class NumberDynamicField : DynamicField(ID) {

    var value: Int? = null

    var minValue: Int? = null

    var maxValue: Int? = null

    override fun getFieldValueUnsafe(): String? = value?.toString()

    override fun apply(from: DynamicField) {
        if (from is NumberDynamicField) {
            value = from.value
        }
    }

    override fun hasValue(): Boolean = value != null

    override fun clear() {
        value = null
    }

    fun getRangeLabel(): String {
        val min = minValue
        val max = maxValue
        return when {
            min != null && max != null -> "($min - $max)"
            min != null -> "(>= $min)"
            max != null -> "(<= $max)"
            else -> ""
        }
    }

    companion object {
        const val ID = "formnumber"
    }

}