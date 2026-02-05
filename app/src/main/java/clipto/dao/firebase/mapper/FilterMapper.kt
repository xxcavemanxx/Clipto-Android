package clipto.dao.firebase.mapper

import clipto.analytics.Analytics
import clipto.common.extensions.castToBoolean
import clipto.common.extensions.castToInt
import clipto.common.extensions.castToListOfInts
import clipto.common.extensions.castToListOfStrings
import clipto.config.IAppConfig
import clipto.dao.firebase.FirebaseDaoHelper
import clipto.dao.objectbox.model.FilterBox
import clipto.domain.*
import clipto.store.app.AppState
import com.google.firebase.firestore.QueryDocumentSnapshot
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterMapper @Inject constructor(
    private val appState: AppState,
    private val appConfig: IAppConfig,
) {

    fun isDeleted(doc: QueryDocumentSnapshot): Boolean = doc[FirebaseDaoHelper.ATTR_FILTER_DELETED].castToBoolean() ?: false

    fun fromDocChange(from: QueryDocumentSnapshot): FilterBox? = runCatching {
        FilterBox().also { filter ->
            filter.uid = from.id
            filter.name = from.getString(FirebaseDaoHelper.ATTR_FILTER_NAME)
            filter.description = from.getString(FirebaseDaoHelper.ATTR_FILTER_DESCRIPTION)
            filter.type = Filter.Type.byId(from.getLong(FirebaseDaoHelper.ATTR_FILTER_TYPE)?.toInt())
            filter.objectType = ObjectType.byId(from.getLong(FirebaseDaoHelper.ATTR_FILTER_OBJECT_TYPE)?.toInt())
            filter.limit = from.getLong(FirebaseDaoHelper.ATTR_FILTER_LIMIT)?.toInt()
            filter.color = from.getString(FirebaseDaoHelper.ATTR_FILTER_COLOR)
            filter.sortBy = SortBy.byId(from.getLong(FirebaseDaoHelper.ATTR_FILTER_SORT_BY)?.toInt())
            filter.listStyle = ListStyle.byId(from.getLong(FirebaseDaoHelper.ATTR_FILTER_LIST_STYLE)?.toInt())
            filter.autoRulesEnabled = from.getBoolean(FirebaseDaoHelper.ATTR_FILTER_AUTO_RULE_ENABLED) ?: false
            filter.pinStarredEnabled = from.getBoolean(FirebaseDaoHelper.ATTR_FILTER_PIN_STARRED_ENABLED) ?: false
            filter.autoRuleByTextIn = from.getString(FirebaseDaoHelper.ATTR_FILTER_AUTO_RULE_TEXT)
            filter.excludeWithCustomAttributes = from.getBoolean(FirebaseDaoHelper.ATTR_FILTER_EXCLUDE_WITH_CUSTOM_ATTRS) ?: true
            filter.tagIds = from.get(FirebaseDaoHelper.ATTR_FILTER_TAG_IDS).castToListOfStrings()
            filter.tagIdsWhereType = Filter.WhereType.byId(from.getLong(FirebaseDaoHelper.ATTR_FILTER_TAG_IDS_WHERE_TYPE)?.toInt())
            filter.snippetSetIds = from.get(FirebaseDaoHelper.ATTR_FILTER_SNIPPET_SETS_IDS).castToListOfStrings()
            filter.snippetSetIdsWhereType = Filter.WhereType.byId(from.getLong(FirebaseDaoHelper.ATTR_FILTER_SNIPPET_SETS_WHERE_TYPE)?.toInt())
            filter.locatedInWhereType = Filter.WhereType.byId(from.getLong(FirebaseDaoHelper.ATTR_FILTER_LOCATED_IN_WHERE_TYPE)?.toInt())
            filter.showOnlyWithPublicLink = from.getBoolean(FirebaseDaoHelper.ATTR_FILTER_SHOW_ONLY_WITH_PUBLIC_LINKS) ?: false
            filter.showOnlyWithAttachments = from.getBoolean(FirebaseDaoHelper.ATTR_FILTER_SHOW_ONLY_WITH_ATTACHMENTS) ?: false
            filter.showOnlyNotSynced = from.getBoolean(FirebaseDaoHelper.ATTR_FILTER_SHOW_ONLY_NOT_SYNCED) ?: false
            filter.textTypeIn = from.get(FirebaseDaoHelper.ATTR_FILTER_TEXT_TYPE_IN).castToListOfInts().map { TextType.byId(it) }
            filter.createDatePeriod = TimePeriod.byId(from.getLong(FirebaseDaoHelper.ATTR_FILTER_CREATE_DATE_PERIOD)?.toInt())
            filter.createDateFrom = from.getDate(FirebaseDaoHelper.ATTR_FILTER_CREATE_DATE_FROM)
            filter.createDateTo = from.getDate(FirebaseDaoHelper.ATTR_FILTER_CREATE_DATE_TO)
            filter.updateDatePeriod = TimePeriod.byId(from.getLong(FirebaseDaoHelper.ATTR_FILTER_UPDATE_DATE_PERIOD)?.toInt())
            filter.updateDateFrom = from.getDate(FirebaseDaoHelper.ATTR_FILTER_UPDATE_DATE_FROM)
            filter.updateDateTo = from.getDate(FirebaseDaoHelper.ATTR_FILTER_UPDATE_DATE_TO)
            filter.textLike = from.getString(FirebaseDaoHelper.ATTR_FILTER_TEXT_LIKE)
            filter.starred = from.getBoolean(FirebaseDaoHelper.ATTR_FILTER_STARRED) ?: false
            filter.untagged = from.getBoolean(FirebaseDaoHelper.ATTR_FILTER_UNTAGGED) ?: false
            filter.clipboard = from.getBoolean(FirebaseDaoHelper.ATTR_FILTER_CLIPBOARD) ?: false
            filter.recycled = from.getBoolean(FirebaseDaoHelper.ATTR_FILTER_RECYCLED) ?: false
            filter.snippets = from.getBoolean(FirebaseDaoHelper.ATTR_FILTER_SNIPPETS) ?: false
            filter.hideHint = from.getBoolean(FirebaseDaoHelper.ATTR_FILTER_HIDE_HINT) ?: false
            filter.snippetKit = SnippetKitMapper.fromMap(from.get(FirebaseDaoHelper.ATTR_FILTER_SNIPPET_KIT))
        }
    }.onFailure { Analytics.onError("FilterMapper", it) }.getOrNull()

    fun toMap(from: Filter, deleted: Boolean = false): Map<String, Any?> {
        val map = mutableMapOf(
            FirebaseDaoHelper.ATTR_API_VERSION to appConfig.getApiVersion(),
            FirebaseDaoHelper.ATTR_CHANGE_TIMESTAMP to FirebaseDaoHelper.getServerTimestamp(),
            FirebaseDaoHelper.ATTR_DEVICE_ID to appState.getInstanceId(),
            FirebaseDaoHelper.ATTR_FILTER_NAME to from.name?.takeIf { from.isNameEditable() },
            FirebaseDaoHelper.ATTR_FILTER_DESCRIPTION to from.description,
            FirebaseDaoHelper.ATTR_FILTER_TYPE to from.type.id,
            FirebaseDaoHelper.ATTR_FILTER_OBJECT_TYPE to from.objectType.id,
            FirebaseDaoHelper.ATTR_FILTER_LIMIT to from.limit,
            FirebaseDaoHelper.ATTR_FILTER_COLOR to from.color,
            FirebaseDaoHelper.ATTR_FILTER_SORT_BY to from.sortBy.id,
            FirebaseDaoHelper.ATTR_FILTER_LIST_STYLE to from.listStyle.id,
            FirebaseDaoHelper.ATTR_FILTER_AUTO_RULE_ENABLED to from.autoRulesEnabled,
            FirebaseDaoHelper.ATTR_FILTER_PIN_STARRED_ENABLED to from.pinStarredEnabled,
            FirebaseDaoHelper.ATTR_FILTER_AUTO_RULE_TEXT to from.autoRuleByTextIn,
            FirebaseDaoHelper.ATTR_FILTER_EXCLUDE_WITH_CUSTOM_ATTRS to from.excludeWithCustomAttributes,
            FirebaseDaoHelper.ATTR_FILTER_TAG_IDS to from.tagIds,
            FirebaseDaoHelper.ATTR_FILTER_TAG_IDS_WHERE_TYPE to from.tagIdsWhereType.id,
            FirebaseDaoHelper.ATTR_FILTER_LOCATED_IN_WHERE_TYPE to from.locatedInWhereType.id,
            FirebaseDaoHelper.ATTR_FILTER_SNIPPET_SETS_IDS to from.snippetSetIds,
            FirebaseDaoHelper.ATTR_FILTER_SNIPPET_SETS_WHERE_TYPE to from.snippetSetIdsWhereType.id,
            FirebaseDaoHelper.ATTR_FILTER_SHOW_ONLY_WITH_PUBLIC_LINKS to from.showOnlyWithPublicLink,
            FirebaseDaoHelper.ATTR_FILTER_SHOW_ONLY_WITH_ATTACHMENTS to from.showOnlyWithAttachments,
            FirebaseDaoHelper.ATTR_FILTER_SHOW_ONLY_NOT_SYNCED to from.showOnlyNotSynced,
            FirebaseDaoHelper.ATTR_FILTER_TEXT_TYPE_IN to from.textTypeIn.map { it.typeId },
            FirebaseDaoHelper.ATTR_FILTER_CREATE_DATE_FROM to from.createDateFrom,
            FirebaseDaoHelper.ATTR_FILTER_CREATE_DATE_TO to from.createDateTo,
            FirebaseDaoHelper.ATTR_FILTER_CREATE_DATE_PERIOD to from.createDatePeriod?.id,
            FirebaseDaoHelper.ATTR_FILTER_UPDATE_DATE_FROM to from.updateDateFrom,
            FirebaseDaoHelper.ATTR_FILTER_UPDATE_DATE_TO to from.updateDateTo,
            FirebaseDaoHelper.ATTR_FILTER_UPDATE_DATE_PERIOD to from.updateDatePeriod?.id,
            FirebaseDaoHelper.ATTR_FILTER_TEXT_LIKE to from.textLike,
            FirebaseDaoHelper.ATTR_FILTER_STARRED to from.starred,
            FirebaseDaoHelper.ATTR_FILTER_UNTAGGED to from.untagged,
            FirebaseDaoHelper.ATTR_FILTER_CLIPBOARD to from.clipboard,
            FirebaseDaoHelper.ATTR_FILTER_RECYCLED to from.recycled,
            FirebaseDaoHelper.ATTR_FILTER_SNIPPETS to from.snippets,
            FirebaseDaoHelper.ATTR_FILTER_HIDE_HINT to from.hideHint,
            FirebaseDaoHelper.ATTR_FILTER_UPDATED to FirebaseDaoHelper.getServerTimestamp(),
            FirebaseDaoHelper.ATTR_FILTER_SNIPPET_KIT to SnippetKitMapper.toMap(from.snippetKit),
            FirebaseDaoHelper.ATTR_FILTER_DELETED to deleted
        )
        return FirebaseDaoHelper.normalizeMap(map, true)
    }

    fun fromMap(from: Map<String, Any?>?): Filter? = runCatching {
        if (from == null) {
            null
        } else {
            FilterBox().apply {
                uid = from["id"]?.toString()
                name = from[FirebaseDaoHelper.ATTR_FILTER_NAME]?.toString()
                color = from[FirebaseDaoHelper.ATTR_FILTER_COLOR]?.toString()
                sortBy = SortBy.byId(from[FirebaseDaoHelper.ATTR_FILTER_SORT_BY].castToInt())
                listStyle = ListStyle.byId(from[FirebaseDaoHelper.ATTR_FILTER_LIST_STYLE].castToInt())
                description = from[FirebaseDaoHelper.ATTR_FILTER_DESCRIPTION]?.toString()
                objectType = ObjectType.byId(from[FirebaseDaoHelper.ATTR_FILTER_OBJECT_TYPE].castToInt())
                snippetKit = SnippetKitMapper.fromMap(from[FirebaseDaoHelper.ATTR_FILTER_SNIPPET_KIT])
            }
        }
    }.getOrNull()
}