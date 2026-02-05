package clipto.dynamic

data class DynamicTextRequestResponse(
        val id: Long = System.currentTimeMillis(),
        val config: DynamicValueConfig,
        val fields: List<FormField>,
        val text: CharSequence,
        val canceled:Boolean = false
)