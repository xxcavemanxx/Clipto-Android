package clipto.api.data

import clipto.domain.PublicStatus
import clipto.domain.SnippetKit

data class UpdateSnippetKitRequest(
    val kit: SnippetKit,
    val message: String?,
    val status: PublicStatus?,
    val language: String?,
    val country: String?,
    val categoryId: String?
)