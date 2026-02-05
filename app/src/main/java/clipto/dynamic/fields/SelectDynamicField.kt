package clipto.dynamic.fields

import android.content.Context
import android.text.style.RelativeSizeSpan
import clipto.common.extensions.toNullIfEmpty
import clipto.common.misc.StringIgnoreEmptyAdapter
import clipto.common.misc.StringIgnoreNullOrBlankAdapter
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.dynamic.DynamicField
import clipto.dynamic.models.ValueFormatterType
import clipto.extensions.getTextColorPrimarySpan
import clipto.extensions.getTextColorSecondarySpan
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

class SelectDynamicField : DynamicField(ID) {

    var multiple: Boolean = false

    var userInput: Boolean = false

    var formatter: String = ValueFormatterType.COMMA.separator

    var options: List<Option> = emptyList()

    var values: List<Option> = emptyList()

    override fun getFieldValueUnsafe(): String? = values
        .mapNotNull { it.value ?: it.title }
        .let {
            val predefined = ValueFormatterType.getByPlaceholder(formatter)
            predefined?.formatter?.invoke(it) ?: it.joinToString(formatter)
        }
        .toNullIfEmpty()

    override fun apply(from: DynamicField) {
        if (from is SelectDynamicField) {
            values = from.values
        }
    }

    override fun hasValue(): Boolean = values.isNotEmpty()

    override fun clear() {
        values = emptyList()
    }

    data class Option(
        @SerializedName(ATTR_OPTION_LABEL)
        @JsonAdapter(StringIgnoreNullOrBlankAdapter::class)
        val title: String? = null,
        @SerializedName(ATTR_OPTION_VALUE)
        @JsonAdapter(StringIgnoreEmptyAdapter::class)
        val value: String? = null
    ) {

        fun getLabel(context: Context): CharSequence? =
            when {
                title != null && value != null -> {
                    SimpleSpanBuilder()
                        .append(title, context.getTextColorPrimarySpan())
                        .append("\n")
                        .append(value, context.getTextColorSecondarySpan(), RelativeSizeSpan(0.7f))
                        .build()
                }
                title != null -> title
                else -> value
            }

    }

    companion object {
        const val ID = "formselect"
    }

}