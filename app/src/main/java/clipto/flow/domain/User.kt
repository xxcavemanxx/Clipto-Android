package clipto.flow.domain

data class User(
    val id: String,
    val name: String,
    val email: String? = null,
    val country: String? = null
)