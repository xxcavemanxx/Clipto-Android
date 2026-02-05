package clipto.dao.firebase.mapper

import clipto.common.extensions.castToInt
import clipto.common.extensions.castToListOfStrings
import clipto.common.extensions.notNull
import clipto.dao.firebase.FirebaseDaoHelper
import clipto.domain.Snippet
import clipto.domain.SnippetKit
import clipto.domain.TextType

object SnippetMapper {

    fun fromMap(kit: SnippetKit, from: Any?): Snippet? {
        if (from == null) {
            return null
        }
        if (from !is Map<*, *>) {
            return null
        }
        return Snippet(
            filterId = kit.filterId,
            userId = kit.userId.notNull(),
            id = from[FirebaseDaoHelper.ATTR_SNIPPET_ID]?.toString()!!,
            text = from[FirebaseDaoHelper.ATTR_SNIPPET_TEXT]?.toString(),
            title = from[FirebaseDaoHelper.ATTR_SNIPPET_TITLE]?.toString(),
            created = DateMapper.toDate(from[FirebaseDaoHelper.ATTR_SNIPPET_CREATED])!!,
            updated = DateMapper.toDate(from[FirebaseDaoHelper.ATTR_SNIPPET_UPDATED]),
            description = from[FirebaseDaoHelper.ATTR_SNIPPET_DESCRIPTION]?.toString(),
            abbreviation = from[FirebaseDaoHelper.ATTR_SNIPPET_ABBREVIATION]?.toString(),
            fileIds = from[FirebaseDaoHelper.ATTR_SNIPPET_FILE_IDS].castToListOfStrings(),
            textType = TextType.byId(from[FirebaseDaoHelper.ATTR_SNIPPET_TEXT_TYPE].castToInt())
        )
    }

}