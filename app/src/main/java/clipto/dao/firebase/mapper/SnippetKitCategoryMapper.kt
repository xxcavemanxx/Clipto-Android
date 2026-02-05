package clipto.dao.firebase.mapper

import clipto.domain.SnippetKitCategory

object SnippetKitCategoryMapper {

    fun fromMap(from: Any?): SnippetKitCategory? {
        if (from == null) {
            return null
        }
        if (from !is Map<*, *>) {
            return null
        }
        return SnippetKitCategory(
            id = from["id"]?.toString()!!,
            name = from["name"]?.toString()!!,
            parentId = from["parentId"]?.toString(),
            description = from["description"]?.toString()
        )
    }

}