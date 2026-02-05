package clipto.flow.domain

data class Error(
    val id: String,
    val message: String,
    val details: String? = null
)