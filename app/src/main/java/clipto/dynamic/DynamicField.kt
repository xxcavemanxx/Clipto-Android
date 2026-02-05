package clipto.dynamic

import clipto.analytics.Analytics
import clipto.common.misc.FormatUtils
import clipto.dynamic.fields.*
import clipto.dynamic.values.NoteTextValue

abstract class DynamicField(val id: String) : Cloneable {

    var label: String? = null
    var prefix: String? = null
    var suffix: String? = null
    var required: Boolean = false

    lateinit var defaultLabel: String
    lateinit var placeholder: String

    fun getFieldValue(): String? = runCatching { getFieldValueUnsafe() }
        .map { value ->
            when {
                !value.isNullOrEmpty() && !prefix.isNullOrEmpty() && !suffix.isNullOrEmpty() -> "$prefix$value$suffix"
                !value.isNullOrEmpty() && !prefix.isNullOrEmpty() -> "$prefix$value"
                !value.isNullOrEmpty() && !suffix.isNullOrEmpty() -> "$value$suffix"
                else -> value
            }
        }
        .onFailure { Analytics.onError("getFieldValue", it) }
        .getOrDefault(FormatUtils.UNKNOWN)

    fun getFieldPlaceholder(): String = placeholder
    open fun getFieldLabel(): String = label ?: defaultLabel
    fun isSnippet(): Boolean = this is SnippetDynamicValue
    fun isLegacySnippet(): Boolean = this is LegacyValueDynamicField && dynamicValue is NoteTextValue
    fun isTextToggle(): Boolean = this is TextToggleDynamicField
    fun isReference(): Boolean = this is ReferenceDynamicValue
    fun isLegacy(): Boolean = this is LegacyValueDynamicField
    fun isUnknown(): Boolean = this is UnknownDynamicField
    fun isUserInput(): Boolean = this !is DynamicValue

    protected abstract fun getFieldValueUnsafe(): String?
    abstract fun apply(from: DynamicField)
    abstract fun hasValue(): Boolean
    abstract fun clear()

    public override fun clone(): Any = super.clone()

    companion object {
        const val BRACE_OPEN = "{"
        const val BRACE_CLOSE = "}"
        const val PLACEHOLDER_OPEN = "{{"
        const val PLACEHOLDER_CLOSE = "}}"
        val PLACEHOLDERS = listOf(PLACEHOLDER_OPEN, PLACEHOLDER_CLOSE)

        // common
        const val ATTR_ID = "id"
        const val ATTR_LABEL = "label"
        const val ATTR_VALUE = "value"
        const val ATTR_LEVEL = "level"
        const val ATTR_REQUIRED = "required"
        const val ATTR_OPTIONS = "options"
        const val ATTR_TYPE = "type"
        const val ATTR_PREFIX = "prefix"
        const val ATTR_SUFFIX = "suffix"

        // text
        const val ATTR_DEFAULT_CLIPBOARD = "clipboard"
        const val ATTR_MAX_LENGTH = "maxLength"
        const val ATTR_MULTI_LINE = "multiLine"

        // number
        const val ATTR_MIN_VALUE = "minValue"
        const val ATTR_MAX_VALUE = "maxValue"

        // select
        const val ATTR_FORMATTER = "formatter"
        const val ATTR_USER_INPUT = "userInput"
        const val ATTR_MULTIPLE = "multiple"

        const val ATTR_OPTION_LABEL = "l"
        const val ATTR_OPTION_VALUE = "v"

        // date
        const val ATTR_DATE_FORMAT = "format"

        // snippet
        const val ATTR_SNIPPET_REF = "ref"

        // toggle
        const val ATTR_TOGGLE_CHECKED = "checked"
        const val ATTR_TOGGLE_TEXT = "text"

        // reference
        const val ATTR_REFERENCE_REF = "ref"
        const val ATTR_REFERENCE_INTRINSIC = "intrinsic"

        fun getId(map: Map<String, Any>): String? {
            return map[ATTR_ID]?.toString()
        }

        fun getId(placeholder: String): String {
            return placeholder.removePrefix(PLACEHOLDER_OPEN).removeSuffix(PLACEHOLDER_CLOSE).trim()
        }

        fun getPlaceholder(id: String): String {
            return "$PLACEHOLDER_OPEN $id $PLACEHOLDER_CLOSE"
        }

        fun isDynamic(text: CharSequence?): Boolean {
            val indexOfOpen = text?.indexOf(PLACEHOLDER_OPEN) ?: return false
            val indexOfClose = text.indexOf(PLACEHOLDER_CLOSE, indexOfOpen)
            return indexOfOpen in 0 until indexOfClose
        }

        fun isStronglyDynamic(text: CharSequence?): Boolean {
            return isDynamic(text) && text?.contains('\n') == false
        }

        fun canBeDynamic(text: CharSequence?): Boolean {
            return text?.indexOf(PLACEHOLDER_OPEN) != -1 || text.indexOf(PLACEHOLDER_CLOSE) != -1
        }

        fun canBeInvalid(text: CharSequence?): Boolean {
            return text?.indexOf(BRACE_OPEN) != -1 || text.indexOf(BRACE_CLOSE) != -1
        }

    }
}