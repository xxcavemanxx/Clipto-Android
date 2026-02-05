package clipto.dynamic

data class FormField(
    val field: DynamicField,
    val startIndex: Int,
    val endIndex: Int,
    val index: Int
) {

    fun getPlaceholder(): String = field.getFieldPlaceholder()
    fun getFieldValue(): String? = field.getFieldValue()
    fun getFieldLabel(): String = field.getFieldLabel()

    fun isSnippet(): Boolean = field.isSnippet() || field.isLegacySnippet()
    fun isTextToggle(): Boolean = field.isTextToggle()
    fun isUserInput(): Boolean = field.isUserInput()
    fun isReference(): Boolean = field.isReference()

    fun apply(from: FormField) = field.apply(from.field)

}