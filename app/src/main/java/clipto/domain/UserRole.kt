package clipto.domain

enum class UserRole(val id: String) {

    USER("user"),

    ADMIN("admin");

    companion object {
        fun byId(id: String?): UserRole =
                when (id) {
                    USER.id -> USER
                    ADMIN.id -> ADMIN
                    else -> USER
                }
    }

}