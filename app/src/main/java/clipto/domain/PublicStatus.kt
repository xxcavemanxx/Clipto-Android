package clipto.domain

import java.io.Serializable

enum class PublicStatus(val id: Int, val actionRequired: Boolean = false):Serializable {

    PRIVATE(id = 0),

    PUBLISHED(id = 1),

    BLOCKED(id = 2, actionRequired = true),

    IN_REVIEW(id = 3),

    REJECTED(id = 4, actionRequired = true),

    NOT_FOUND(id = 5),

    DELETED(id = 6),

    RESTRICTED(id = 7)
    ;

    companion object {
        fun byId(id: Int?): PublicStatus =
                when (id) {
                    PRIVATE.id -> PRIVATE
                    PUBLISHED.id -> PUBLISHED
                    BLOCKED.id -> BLOCKED
                    IN_REVIEW.id -> IN_REVIEW
                    REJECTED.id -> REJECTED
                    NOT_FOUND.id -> NOT_FOUND
                    DELETED.id -> DELETED
                    RESTRICTED.id -> RESTRICTED
                    else -> PRIVATE
                }
    }

}