package clipto.api.data

import clipto.domain.LicenseType

data class CheckSessionResponse(
        val syncLimit: Int?,
        val plan: LicenseType?,
        val syncSubscriptionId: String?,
        val syncSubscriptionToken: String?
)