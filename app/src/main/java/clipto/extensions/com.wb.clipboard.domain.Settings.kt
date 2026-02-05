package clipto.extensions

import android.content.Context
import androidx.annotation.StringRes
import clipto.common.misc.AndroidUtils
import clipto.common.misc.ThemeUtils
import clipto.domain.*
import com.wb.clipboard.R

fun SortBy.toExt() = when (this) {
    SortBy.DELETE_DATE_DESC -> SortByExt.DELETE_DATE_DESC
    SortBy.CREATE_DATE_DESC -> SortByExt.CREATE_DATE_DESC
    SortBy.MODIFY_DATE_DESC -> SortByExt.MODIFY_DATE_DESC
    SortBy.USAGE_DATE_DESC -> SortByExt.USAGE_DATE_DESC
    SortBy.USAGE_COUNT_DESC -> SortByExt.USAGE_COUNT_DESC
    SortBy.TITLE_DESC -> SortByExt.TITLE_DESC
    SortBy.TEXT_DESC -> SortByExt.TEXT_DESC
    SortBy.TAGS_DESC -> SortByExt.TAGS_DESC
    SortBy.SIZE_DESC -> SortByExt.SIZE_DESC
    SortBy.CHARACTERS_DESC -> SortByExt.CHARACTERS_DESC
    SortBy.NAME_DESC -> SortByExt.NAME_DESC
    SortBy.NOTES_COUNT_DESC -> SortByExt.NOTES_COUNT_DESC
    SortBy.COLOR_DESC -> SortByExt.COLOR_DESC
    SortBy.MANUAL_DESC -> SortByExt.MANUAL_DESC

    SortBy.DELETE_DATE_ASC -> SortByExt.DELETE_DATE_ASC
    SortBy.CREATE_DATE_ASC -> SortByExt.CREATE_DATE_ASC
    SortBy.MODIFY_DATE_ASC -> SortByExt.MODIFY_DATE_ASC
    SortBy.USAGE_DATE_ASC -> SortByExt.USAGE_DATE_ASC
    SortBy.USAGE_COUNT_ASC -> SortByExt.USAGE_COUNT_ASC
    SortBy.TITLE_ASC -> SortByExt.TITLE_ASC
    SortBy.TEXT_ASC -> SortByExt.TEXT_ASC
    SortBy.TAGS_ASC -> SortByExt.TAGS_ASC
    SortBy.SIZE_ASC -> SortByExt.SIZE_ASC
    SortBy.CHARACTERS_ASC -> SortByExt.CHARACTERS_ASC
    SortBy.NAME_ASC -> SortByExt.NAME_ASC
    SortBy.NOTES_COUNT_ASC -> SortByExt.NOTES_COUNT_ASC
    SortBy.COLOR_ASC -> SortByExt.COLOR_ASC
    SortBy.MANUAL_ASC -> SortByExt.MANUAL_ASC
}

fun SwipeAction.toColor(context: Context) = when (this) {
    SwipeAction.TAG -> ThemeUtils.getColor(context, R.attr.swipeActionStarred)
    SwipeAction.DELETE -> ThemeUtils.getColor(context, R.attr.swipeActionDelete)
    SwipeAction.STAR -> ThemeUtils.getColor(context, R.attr.swipeActionStarred)
    SwipeAction.COPY -> ThemeUtils.getColor(context, R.attr.swipeActionCopy)
    SwipeAction.NONE -> 0
}

fun SwipeAction.toTitle() = when (this) {
    SwipeAction.TAG -> R.string.clip_info_label_tags_edit
    SwipeAction.DELETE -> R.string.main_swipe_actions_delete
    SwipeAction.STAR -> R.string.main_swipe_actions_fav
    SwipeAction.COPY -> R.string.main_swipe_actions_copy
    SwipeAction.NONE -> R.string.main_swipe_actions_none
}

fun SwipeAction.toIcon(clip: Clip? = null) = when (this) {
    SwipeAction.TAG -> R.drawable.swipe_action_tag
    SwipeAction.DELETE -> R.drawable.swipe_action_delete
    SwipeAction.STAR -> clip?.let { if (it.fav) R.drawable.swipe_action_fav_false else R.drawable.swipe_action_fav_true }
        ?: R.drawable.swipe_action_fav_true
    SwipeAction.COPY -> clip?.let { if (it.isActive) R.drawable.swipe_action_clear else R.drawable.swipe_action_copy }
        ?: R.drawable.swipe_action_copy
    SwipeAction.NONE -> 0
}

fun NotificationStyle.getTitleRes() = when (this) {
    NotificationStyle.NULL -> R.string.theme_default
    NotificationStyle.DEFAULT -> R.string.theme_default
    NotificationStyle.CONTROLS -> R.string.notification_style_controls
    NotificationStyle.HISTORY -> R.string.notification_style_history
    NotificationStyle.ACTIONS -> R.string.notification_style_actions
}

fun ListStyle.getTitleRes() = when (this) {
    ListStyle.DEFAULT -> R.string.list_style_default
    ListStyle.COMPACT -> R.string.list_style_compact
    ListStyle.COMFORTABLE -> R.string.list_style_comfortable
    ListStyle.PREVIEW -> R.string.list_style_preview
    ListStyle.GRID -> R.string.list_style_grid
    else -> R.string.list_style_default
}

fun ListStyle.getLayoutRes() = when (this) {
    ListStyle.DEFAULT -> R.layout.item_clip
    ListStyle.COMPACT -> R.layout.item_clip_compact
    ListStyle.COMFORTABLE -> R.layout.item_clip_comfortable
    ListStyle.PREVIEW -> R.layout.item_clip_preview
    ListStyle.GRID -> R.layout.item_clip_grid
    else -> R.layout.item_clip
}

fun NotificationStyle.isAvailable() = when (this) {
    NotificationStyle.NULL -> false
    NotificationStyle.DEFAULT -> true
    NotificationStyle.CONTROLS -> true
    NotificationStyle.HISTORY -> !AndroidUtils.isPreMarshmallow()
    NotificationStyle.ACTIONS -> !AndroidUtils.isPreMarshmallow()
}

enum class SortByExt(val sortBy: SortBy, val sortByOpposite: SortBy, @StringRes val titleRes: Int, val desc: Boolean = false) {

    DELETE_DATE_DESC(SortBy.DELETE_DATE_DESC, SortBy.DELETE_DATE_ASC, R.string.sort_order_delete_date, true),
    CREATE_DATE_DESC(SortBy.CREATE_DATE_DESC, SortBy.CREATE_DATE_ASC, R.string.sort_order_create_date, true),
    MODIFY_DATE_DESC(SortBy.MODIFY_DATE_DESC, SortBy.MODIFY_DATE_ASC, R.string.sort_order_modify_date, true),
    USAGE_DATE_DESC(SortBy.USAGE_DATE_DESC, SortBy.USAGE_DATE_ASC, R.string.sort_order_usage_date, true),
    USAGE_COUNT_DESC(SortBy.USAGE_COUNT_DESC, SortBy.USAGE_COUNT_ASC, R.string.sort_order_usage_count, true),
    TITLE_DESC(SortBy.TITLE_DESC, SortBy.TITLE_ASC, R.string.sort_order_title, true),
    TEXT_DESC(SortBy.TEXT_DESC, SortBy.TEXT_ASC, R.string.sort_order_text, true),
    TAGS_DESC(SortBy.TAGS_DESC, SortBy.TAGS_ASC, R.string.sort_order_tags, true),
    SIZE_DESC(SortBy.SIZE_DESC, SortBy.SIZE_ASC, R.string.sort_order_size, true),
    CHARACTERS_DESC(SortBy.CHARACTERS_DESC, SortBy.CHARACTERS_ASC, R.string.clip_attr_charsCount, true),
    NAME_DESC(SortBy.NAME_DESC, SortBy.NAME_ASC, R.string.sort_order_name, true),
    NOTES_COUNT_DESC(SortBy.NOTES_COUNT_DESC, SortBy.NOTES_COUNT_ASC, R.string.sort_order_notes_count, true),
    COLOR_DESC(SortBy.COLOR_DESC, SortBy.COLOR_ASC, R.string.sort_order_color, true),
    MANUAL_DESC(SortBy.MANUAL_DESC, SortBy.MANUAL_ASC, R.string.sort_order_manual, true),

    DELETE_DATE_ASC(SortBy.DELETE_DATE_ASC, SortBy.DELETE_DATE_DESC, R.string.sort_order_delete_date),
    CREATE_DATE_ASC(SortBy.CREATE_DATE_ASC, SortBy.CREATE_DATE_DESC, R.string.sort_order_create_date),
    MODIFY_DATE_ASC(SortBy.MODIFY_DATE_ASC, SortBy.MODIFY_DATE_DESC, R.string.sort_order_modify_date),
    USAGE_DATE_ASC(SortBy.USAGE_DATE_ASC, SortBy.USAGE_DATE_DESC, R.string.sort_order_usage_date),
    USAGE_COUNT_ASC(SortBy.USAGE_COUNT_ASC, SortBy.USAGE_COUNT_DESC, R.string.sort_order_usage_count),
    TITLE_ASC(SortBy.TITLE_ASC, SortBy.TITLE_DESC, R.string.sort_order_title),
    TEXT_ASC(SortBy.TEXT_ASC, SortBy.TEXT_DESC, R.string.sort_order_text),
    TAGS_ASC(SortBy.TAGS_ASC, SortBy.TAGS_DESC, R.string.sort_order_tags),
    SIZE_ASC(SortBy.SIZE_ASC, SortBy.SIZE_DESC, R.string.sort_order_size),
    CHARACTERS_ASC(SortBy.CHARACTERS_ASC, SortBy.CHARACTERS_DESC, R.string.clip_attr_charsCount),
    NAME_ASC(SortBy.NAME_ASC, SortBy.NAME_DESC, R.string.sort_order_name),
    NOTES_COUNT_ASC(SortBy.NOTES_COUNT_ASC, SortBy.NOTES_COUNT_DESC, R.string.sort_order_notes_count),
    COLOR_ASC(SortBy.COLOR_ASC, SortBy.COLOR_DESC, R.string.sort_order_color),
    MANUAL_ASC(SortBy.MANUAL_ASC, SortBy.MANUAL_DESC, R.string.sort_order_manual),
    ;

    companion object {
        private val sortByRulesForNotes = arrayOf(
            CREATE_DATE_DESC,
            MODIFY_DATE_DESC,
            USAGE_DATE_DESC,
            USAGE_COUNT_DESC,
            TITLE_ASC,
            TEXT_ASC,
            TAGS_ASC,
            SIZE_DESC,
            CHARACTERS_DESC
        )
        private val sortByRulesForRecycleBin = arrayOf(
            DELETE_DATE_DESC,
            CREATE_DATE_DESC,
            MODIFY_DATE_DESC,
            USAGE_DATE_DESC,
            USAGE_COUNT_DESC,
            TITLE_ASC,
            TEXT_ASC,
            TAGS_ASC,
            SIZE_DESC,
            CHARACTERS_DESC
        )
        private val sortByRulesForTagsGroup = arrayOf(
            NAME_ASC,
            NOTES_COUNT_DESC,
            COLOR_ASC,
            MANUAL_ASC
        )
        private val sortByRulesForSnippetsGroup = arrayOf(
            NAME_ASC,
            NOTES_COUNT_DESC,
            COLOR_ASC,
            MANUAL_ASC
        )
        private val sortByRulesForFiltersGroup = arrayOf(
            NAME_ASC,
            COLOR_ASC,
            MANUAL_ASC
        )
        private val sortByRulesForFilesGroup = arrayOf(
            NAME_ASC,
            MANUAL_ASC
        )
        private val sortByRulesForFoldersGroup = arrayOf(
            NAME_ASC,
            MANUAL_ASC
        )
        private val sortByRulesForNotesGroup = arrayOf(
            NAME_ASC,
            NOTES_COUNT_DESC,
            MANUAL_ASC
        )
        private val sortByRulesForSnippetKit = arrayOf(
            TITLE_ASC,
            TEXT_ASC,
            CREATE_DATE_DESC,
            MODIFY_DATE_DESC
        )
        private val sortByRulesForFolder = arrayOf(
            CREATE_DATE_DESC,
            MODIFY_DATE_DESC,
            SIZE_DESC,
            NAME_ASC
        )

        fun getItems(filter: Filter) =
            when {
                filter.isDeleted() -> sortByRulesForRecycleBin
                filter.isGroupTags() -> sortByRulesForTagsGroup
                filter.isGroupNotes() -> sortByRulesForNotesGroup
                filter.isGroupFolders() -> sortByRulesForFoldersGroup
                filter.isGroupFiles() -> sortByRulesForFilesGroup
                filter.isGroupFilters() -> sortByRulesForFiltersGroup
                filter.isGroupSnippets() -> sortByRulesForSnippetsGroup
                filter.isSnippetKit() -> sortByRulesForSnippetKit
                filter.isFolder() -> sortByRulesForFolder
                else -> sortByRulesForNotes
            }
    }

}