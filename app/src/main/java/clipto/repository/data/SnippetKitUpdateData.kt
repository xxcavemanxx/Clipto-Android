package clipto.repository.data

import clipto.domain.PublicStatus

data class SnippetKitUpdateData(
    val message: String? = null,
    val status: PublicStatus? = null,
    val language: String? = null,
    val country: String? = null,
    val categoryId:String? = null
)
