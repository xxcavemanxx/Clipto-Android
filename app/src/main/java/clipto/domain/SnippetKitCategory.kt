package clipto.domain

import java.io.Serializable

data class SnippetKitCategory(
        val id: String,
        val name: String,
        val parentId: String?,
        val description: String?
) : Serializable