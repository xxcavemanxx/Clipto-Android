package clipto.repository.data

import clipto.domain.SnippetKit
import clipto.domain.UserRole

data class SnippetKitData(
        val kit: SnippetKit,
        val accessRole: UserRole
)
