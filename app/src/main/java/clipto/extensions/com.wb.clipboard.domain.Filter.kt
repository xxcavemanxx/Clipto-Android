package clipto.extensions

import android.content.Context
import clipto.common.extensions.toNullIfEmpty
import clipto.common.misc.IdUtils
import clipto.common.misc.ThemeUtils
import clipto.dao.objectbox.model.FilterBox
import clipto.domain.Filter
import clipto.domain.ListStyle
import clipto.domain.ObjectType
import com.wb.clipboard.R
import kotlin.math.max

fun Filter.Companion.createTag(tagName: String? = null, useOldIdGenerator: Boolean = false): Filter {
    return FilterBox().apply {
        type = Filter.Type.TAG
        hideHint = true
        if (tagName != null) {
            val id = if (useOldIdGenerator) IdUtils.autoId(tagName) else IdUtils.autoId()
            tagIds = listOf(id)
            name = tagName
            uid = id
        }
    }
}

fun Filter.Companion.createTagWithId(id: String = IdUtils.autoId()): Filter {
    return FilterBox().apply {
        type = Filter.Type.TAG
        tagIds = listOf(id)
        hideHint = true
        uid = id
    }
}

fun Filter.Companion.createSnippetKit(id: String = IdUtils.autoId()): Filter {
    return FilterBox().apply {
        type = Filter.Type.SNIPPET_KIT
        snippetSetIds = listOf(id)
        hideHint = true
        uid = id
    }
}

fun Filter.getIconColor(context: Context, active: Boolean = false): Int =
    color?.let { ThemeUtils.getColor(context, it) }
        ?: run {
            if (active) {
                ThemeUtils.getColor(context, R.attr.actionIconColorHighlight)
            } else {
                ThemeUtils.getColor(context, android.R.attr.textColorSecondary)
            }
        }

fun Filter.getIndicatorRes(userId: String?): Int? {
    val kit = snippetKit
    return when {
        hasActiveAutoRule() -> R.drawable.ic_auto_rule
        kit != null && kit.isMy(userId) && kit.isActionRequired() -> R.drawable.ic_status_error
        kit != null && kit.isMy(userId) && kit.isPublished() -> R.drawable.ic_status_public
        kit != null && kit.isMy(userId) && kit.isInReview() -> R.drawable.ic_status_pending
        else -> null
    }
}

fun Filter.getTagChipColor(context: Context, withDefault: Boolean = false): Int? = color
    ?.let { ThemeUtils.getColor(context, it) }
    ?: run {
        if (withDefault) ThemeUtils.getColor(context, android.R.attr.textColorPrimary) else null
    }

fun Filter.getIconRes() =
    when (type) {
        Filter.Type.ALL -> R.drawable.filter_all
        Filter.Type.STARRED -> R.drawable.filter_starred
        Filter.Type.UNTAGGED -> R.drawable.filter_untagged
        Filter.Type.CLIPBOARD -> R.drawable.filter_clipboard
        Filter.Type.DELETED -> R.drawable.action_delete
        Filter.Type.SNIPPETS -> R.drawable.filter_snippet
        Filter.Type.CUSTOM -> R.drawable.action_filter
        Filter.Type.LAST -> R.drawable.action_filter
        Filter.Type.NAMED -> R.drawable.action_filter
        Filter.Type.GROUP_FILTERS -> R.drawable.action_filter
        Filter.Type.GROUP_NOTES -> R.drawable.filter_group_notes
        Filter.Type.GROUP_FILES -> R.drawable.filter_group_files
        Filter.Type.GROUP_FOLDERS -> R.drawable.filter_group_folders
        Filter.Type.GROUP_TAGS -> R.drawable.filter_tag_outline
        Filter.Type.GROUP_SNIPPETS -> R.drawable.filter_snippet_set
        Filter.Type.SNIPPET_KIT -> R.drawable.filter_snippet_set
        Filter.Type.FOLDER -> R.drawable.filter_group_folders
        else -> if (color != null) R.drawable.filter_tag else R.drawable.filter_tag_outline
    }

fun Filter.getTitle(context: Context): String =
    when (type) {
        Filter.Type.ALL -> context.getString(R.string.main_filter_all)
        Filter.Type.STARRED -> context.getString(R.string.filter_label_fav)
        Filter.Type.UNTAGGED -> context.getString(R.string.main_filter_no_tags)
        Filter.Type.CLIPBOARD -> context.getString(R.string.runes_clipboard_title)
        Filter.Type.DELETED -> context.getString(R.string.filter_deleted_title)
        Filter.Type.SNIPPETS -> context.getString(R.string.filter_label_snippets)
        Filter.Type.CUSTOM -> context.getString(R.string.main_filter_filtered)
        Filter.Type.LAST -> context.getString(R.string.main_filter_filtered)
        Filter.Type.GROUP_FILTERS -> context.getString(R.string.main_filter_filters)
        Filter.Type.GROUP_FILES -> context.getString(R.string.main_filter_files)
        Filter.Type.GROUP_FOLDERS -> context.getString(R.string.main_filter_folders)
        Filter.Type.GROUP_NOTES -> context.getString(R.string.main_filter_notes)
        Filter.Type.GROUP_TAGS -> context.getString(R.string.main_filter_tags)
        Filter.Type.GROUP_SNIPPETS -> context.getString(R.string.clip_details_tab_snippet_kits)
        else -> getTitle()
    }

fun Filter.getEditHint(context: Context): String? =
    when (type) {
        Filter.Type.SNIPPET_KIT -> context.getString(R.string.snippet_kit_hint_name)
        Filter.Type.NAMED -> context.getString(R.string.filter_hint_name)
        Filter.Type.TAG -> context.getString(R.string.tag_new_name)
        else -> null
    }

fun Filter.getEmptyLabel(context: Context): String =
    when (type) {
        Filter.Type.ALL -> context.getString(R.string.main_list_empty_description_all)
        Filter.Type.TAG -> context.getString(R.string.main_list_empty_description_tags)
        Filter.Type.STARRED -> context.getString(R.string.main_list_empty_description_starred)
        Filter.Type.UNTAGGED -> context.getString(R.string.main_list_empty_description_no_tags)
        Filter.Type.CLIPBOARD -> context.getString(R.string.main_list_empty_description_clipboard)
        Filter.Type.DELETED -> context.getString(R.string.main_list_empty_description_deleted)
        Filter.Type.SNIPPETS -> context.getString(R.string.main_list_empty_description_snippets)
        Filter.Type.CUSTOM -> context.getString(R.string.main_list_empty_description_filtered)
        Filter.Type.LAST -> context.getString(R.string.main_list_empty_description_filtered)
        Filter.Type.FOLDER -> context.getString(R.string.folder_error_empty)
        else -> context.getString(R.string.main_list_empty_description_filtered)
    }

fun Filter.getHint(context: Context): String? {
    val descriptionRef = description
    if (!descriptionRef.isNullOrBlank()) {
        return descriptionRef
    }
    return when (type) {
        Filter.Type.ALL -> context.getString(R.string.main_list_empty_description_all)
        Filter.Type.TAG -> context.getString(R.string.hint_tag)
        Filter.Type.STARRED -> context.getString(R.string.main_list_empty_description_starred)
        Filter.Type.UNTAGGED -> context.getString(R.string.main_list_empty_description_no_tags)
        Filter.Type.CLIPBOARD -> context.getString(R.string.main_list_empty_description_clipboard)
        Filter.Type.DELETED -> context.getString(R.string.main_list_empty_description_deleted)
        Filter.Type.SNIPPETS -> context.getString(R.string.main_list_empty_description_snippets)
        Filter.Type.GROUP_NOTES -> context.getString(R.string.hint_notes)
        Filter.Type.GROUP_FILES -> context.getString(R.string.hint_files)
        Filter.Type.GROUP_FOLDERS -> context.getString(R.string.hint_folders)
        Filter.Type.GROUP_FILTERS -> context.getString(R.string.hint_filters)
        Filter.Type.GROUP_TAGS -> context.getString(R.string.hint_tags)
        Filter.Type.GROUP_SNIPPETS -> context.getString(R.string.hint_snippets)
        Filter.Type.NAMED -> context.getString(R.string.hint_filter)
        Filter.Type.SNIPPET_KIT -> context.getString(R.string.hint_snippet)
        Filter.Type.CUSTOM -> context.getString(R.string.hint_filter_custom)
        Filter.Type.LAST -> context.getString(R.string.hint_filter_custom)
        else -> null
    }
}

fun Filter.normalize() = this.apply {
    val uidRef = uid
    if (!isNamedFilter() && !isLast()) {
        textLike = null
    }
    if (isTag() && uidRef != null) {
        snippetSetIds = emptyList()
        tagIds = listOf(uidRef)
    } else if (isSnippetKit() && uidRef != null) {
        snippetSetIds = listOf(uidRef)
        tagIds = emptyList()
    }
    val sortRules = SortByExt.getItems(this)
    if (!sortRules.any { it.sortBy == sortBy || it.sortByOpposite == sortBy }) {
        sortBy = sortRules.first().sortBy
    }
    listStyle = ListStyle.getStyle(this)
    notesCount = max(0, notesCount)
}

fun Filter.anonymize() = this.apply {
    objectType = ObjectType.INTERNAL
    type = Filter.Type.CUSTOM
    autoRulesEnabled = false
    description = null
    snippetKit = null
    name = null
    uid = null
    if (this is FilterBox) {
        localId = 0
    }
}

fun Filter.getCreateDateRangeLabel(context: Context): CharSequence? {
    val period = createDatePeriod ?: return null
    val interval = period.toInterval(createDateFrom, createDateTo)
    return interval?.getLabel(context)
}

fun Filter.getUpdateDateRangeLabel(context: Context): CharSequence? {
    val period = updateDatePeriod ?: return null
    val interval = period.toInterval(updateDateFrom, updateDateTo)
    return interval?.getLabel(context)
}

fun Filter.asNew(type: Filter.Type = Filter.Type.NAMED) = FilterBox().apply(this).anonymize().apply {
    this.pinStarredEnabled = false
    this.autoRulesEnabled = false
    this.autoRuleByTextIn = null
    this.description = null
    this.type = type
}