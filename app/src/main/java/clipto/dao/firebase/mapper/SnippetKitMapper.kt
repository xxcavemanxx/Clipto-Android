package clipto.dao.firebase.mapper

import clipto.dao.firebase.FirebaseDaoHelper
import clipto.domain.*

object SnippetKitMapper {

    fun toMap(from: SnippetKit?): Map<String, Any?>? {
        if (from == null) {
            return null
        }
        return mapOf(
            FirebaseDaoHelper.ATTR_SNIPPET_KIT_ID to from.id,
            FirebaseDaoHelper.ATTR_SNIPPET_KIT_NAME to from.name,
            FirebaseDaoHelper.ATTR_SNIPPET_KIT_CREATED to from.created,
            FirebaseDaoHelper.ATTR_SNIPPET_KIT_UPDATED to from.updated,
            FirebaseDaoHelper.ATTR_SNIPPET_KIT_INSTALLS to from.installs,
            FirebaseDaoHelper.ATTR_SNIPPET_KIT_USER_ID to from.userId,
            FirebaseDaoHelper.ATTR_SNIPPET_KIT_USER_NAME to from.userName,
            FirebaseDaoHelper.ATTR_SNIPPET_KIT_COLOR to from.color,
            FirebaseDaoHelper.ATTR_SNIPPET_KIT_COUNTRY to from.country,
            FirebaseDaoHelper.ATTR_SNIPPET_KIT_LANGUAGE to from.language,
            FirebaseDaoHelper.ATTR_SNIPPET_KIT_FILTER_ID to from.filterId,
            FirebaseDaoHelper.ATTR_SNIPPET_KIT_SNIPPETS_COUNT to from.snippetsCount,
            FirebaseDaoHelper.ATTR_SNIPPET_KIT_PUBLIC_LINK to from.publicLink,
            FirebaseDaoHelper.ATTR_SNIPPET_KIT_DESCRIPTION to from.description,
            FirebaseDaoHelper.ATTR_SNIPPET_KIT_PUBLIC_STATUS to from.publicStatus.id,
            FirebaseDaoHelper.ATTR_SNIPPET_KIT_UPDATE_REASON to from.updateReason,
            FirebaseDaoHelper.ATTR_SNIPPET_KIT_CATEGORY_ID to from.categoryId,
            FirebaseDaoHelper.ATTR_SNIPPET_KIT_SHARABLE to from.sharable,
            FirebaseDaoHelper.ATTR_SNIPPET_KIT_SORT_BY to from.sortBy.id,
            FirebaseDaoHelper.ATTR_SNIPPET_KIT_LIST_STYLE to from.listStyle.id
        )
    }

    fun fromMap(from: Any?, silentCatch: Boolean = true): SnippetKit? {
        if (from == null) {
            return null
        }
        if (from !is Map<*, *>) {
            return null
        }
        val safeBlock = runCatching {
            val kit = SnippetKit(
                id = from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_ID]?.toString()!!,
                hash = from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_HASH]?.toString(),
                name = from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_NAME].toString(),
                created = DateMapper.toDate(from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_CREATED])!!,
                updated = DateMapper.toDate(from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_UPDATED]),
                installs = from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_INSTALLS]?.let { it as Number }?.toInt() ?: 0,
                userId = from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_USER_ID]?.toString()!!,
                userName = from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_USER_NAME]?.toString(),
                color = from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_COLOR]?.toString(),
                country = from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_COUNTRY]?.toString(),
                language = from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_LANGUAGE]?.toString(),
                filterId = from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_FILTER_ID]?.toString()!!,
                snippetsCount = from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_SNIPPETS_COUNT]?.let { it as Number }?.toInt() ?: 0,
                publicLink = from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_PUBLIC_LINK]?.toString(),
                description = from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_DESCRIPTION]?.toString(),
                publicStatus = PublicStatus.byId(from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_PUBLIC_STATUS]?.let { it as Number }?.toInt()),
                updateReason = from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_UPDATE_REASON]?.toString(),
                categoryId = from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_CATEGORY_ID]?.toString(),
                sharable = from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_SHARABLE]?.let { it as Boolean } ?: false,
                sortBy = SortBy.byId(from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_SORT_BY]?.let { it as Number }?.toInt()),
                listStyle = ListStyle.byId(from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_LIST_STYLE]?.let { it as Number }?.toInt())
            )
            kit.snippets = from[FirebaseDaoHelper.ATTR_SNIPPET_KIT_SNIPPETS]
                ?.takeIf { it is List<*> }
                ?.let { it as List<*> }
                ?.mapNotNull { SnippetMapper.fromMap(kit, it) }
                ?: emptyList()
            kit
        }
        return if (silentCatch) {
            safeBlock.getOrNull()
        } else {
            safeBlock.getOrThrow()
        }
    }

}