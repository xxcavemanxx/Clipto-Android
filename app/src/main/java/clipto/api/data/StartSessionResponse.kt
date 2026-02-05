package clipto.api.data

import clipto.domain.UserRole

data class StartSessionResponse(
        val invitedCount: Int?,
        val userRole: UserRole = UserRole.USER
)