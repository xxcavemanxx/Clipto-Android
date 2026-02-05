package clipto.domain

import android.graphics.Color
import androidx.core.graphics.ColorUtils
import clipto.common.extensions.notNull
import clipto.common.extensions.reversed
import clipto.dao.objectbox.model.FilterBox
import clipto.extensions.createSnippetKit
import clipto.extensions.createTagWithId
import clipto.extensions.log

data class Filters(
    val all: Filter,
    val untagged: Filter,
    val starred: Filter,
    val clipboard: Filter,
    val deleted: Filter,
    val last: Filter,
    val folders: Filter,
    val snippets: Filter,
    val texpander: Filter,
    val groupTags: Filter,
    val groupNotes: Filter,
    val groupFiles: Filter,
    val groupFolders: Filter,
    val groupFilters: Filter,
    val groupSnippets: Filter,
    val filtersByUid: MutableMap<String, Filter>,
    var initial: Boolean
) {

    fun getFilteredNotesCount() = last.notesCount

    fun hasFilteredNotes() = getFilteredNotesCount() > 0

    fun hasTags() = getTags().isNotEmpty()

    fun hasActiveFilter(): Boolean = last.hasActiveFilter()

    fun getByUid(uid: String?): Filter? = filtersByUid[uid]

    fun getTags() = filtersByUid.values.filter { it.isTag() && it.name != null }

    fun getSortedTags() = sorted(groupTags, getTags())

    fun getSnippetKits() = filtersByUid.values.filter { it.isSnippetKit() && it.name != null }

    fun getSortedSnippetKits() = sorted(groupSnippets, getSnippetKits())

    fun getNamedFilters() = filtersByUid.values.filter { it.isNamedFilter() }

    fun getSortedNamedFilters() = sorted(groupFilters, getNamedFilters())

    fun getCategories() = listOf(all, starred, untagged, clipboard, snippets, deleted)

    fun getSortedCategories() = sorted(groupNotes, getCategories())

    fun findFilterByTagId(uid: String?, stubIfNotFound: Boolean = false): Filter? =
        filtersByUid[uid]
            ?.takeIf { it.isTag() }
            ?: stubIfNotFound
                .takeIf { it && uid != null && filtersByUid[uid] == null }
                ?.let {
                    uid!!
                    log("findFilterByTagId :: return stub :: {}", uid)
                    val filter = Filter.createTagWithId(uid)
                    filtersByUid[uid] = filter
                    filter
                }

    fun findFilterBySnippetKitId(uid: String?, stubIfNotFound: Boolean = false): Filter? =
        filtersByUid[uid]
            ?.takeIf { it.isSnippetKit() }
            ?: stubIfNotFound
                .takeIf { it && uid != null && filtersByUid[uid] == null }
                ?.let {
                    uid!!
                    log("findFilterBySnippetKitId :: return stub :: {}", uid)
                    val filter = Filter.createSnippetKit(uid)
                    filtersByUid[uid] = filter
                    filter
                }

    fun findFilterBySnippetKit(kit: SnippetKit): Filter? = filtersByUid.values.firstOrNull { it.snippetKit?.id == kit.id }

    fun findFilterByTagName(name: String) = filtersByUid.values.find { it.isTag() && it.name == name }

    fun findFilterByName(name: String) = filtersByUid.values.find { it.isNamedFilter() && it.name == name }

    fun findFilterBySnippetSetName(name: String) = filtersByUid.values.find { it.isSnippetKit() && it.name == name }

    fun findGroup(filter: Filter): Filter? {
        return when {
            filter.isTag() -> groupTags
            filter.isNamedFilter() -> groupFilters
            filter.isSnippetKit() -> groupSnippets
            filter.isNotesCategory() -> groupNotes
            filter.isFolder() -> groupFolders
            else -> null
        }
    }

    fun findActive(template: Filter = last): Filter {
        val activeFilter = getByUid(template.activeFilterId)
        return when {
            activeFilter != null -> activeFilter
            template.isNamedFilter() -> template
            template.isTag(this) -> template.tagIds.singleOrNull()?.let { getByUid(it) } ?: template
            template.isSnippetKit(this) -> template.snippetSetIds.singleOrNull()?.let { getByUid(it) } ?: template
            template.isGroup() -> template
            template.isContext() -> template
            template.isStarred() -> starred
            template.isUntagged() -> untagged
            template.isClipboard() -> clipboard
            template.isDeleted() -> deleted
            template.isSnippets() -> snippets
            template.isAll() -> all
            template.isLast() -> {
                if (template.isFolder()) {
                    FilterBox().apply(template).also { it.type = Filter.Type.FOLDER }
                } else {
                    template
                }
            }
            else -> template
        }
    }

    fun isActive(filter: Filter, deepCheck: Boolean = false): Boolean {
        if (filter == last || filter.isSame(last)) {
            return true
        }
        return when {
            last.isFolder() && filter.isFolder() -> last.folderId == filter.folderId
            deepCheck && last.activeFilterId != null && last.activeFilterId == filter.uid -> true
            !deepCheck && last.activeFilterId != null -> last.activeFilterId == filter.uid
            filter.isTag() && last.tagIds.contains(filter.uid) -> true
            last.starred && filter.isStarred() -> true
            last.untagged && filter.isUntagged() -> true
            last.clipboard && filter.isClipboard() -> true
            last.recycled && filter.isDeleted() -> true
            last.snippets && filter.isSnippets() -> true
            filter.isAll() && last.isAll() -> true
            else -> false
        }
    }

    fun getAll(): List<Filter> {
        val list = mutableListOf<Filter>()
        list.add(all)
        list.add(starred)
        list.add(untagged)
        list.add(clipboard)
        list.add(snippets)
        list.addAll(getSortedTags())
        list.add(deleted)
        return list
    }

    companion object {
        private val NAME_COMPARATOR = Comparator<Filter> { f1, f2 -> f1.name.notNull().compareTo(f2.name.notNull()) }
        private val NOTES_COUNT_COMPARATOR = kotlin.Comparator<Filter> { f1, f2 -> f1.notesCount.compareTo(f2.notesCount) }
        private val COLOR_COMPARATOR = Comparator<Filter> { f1, f2 ->
            val f1Color = f1.color?.let { Color.parseColor(it) } ?: Color.TRANSPARENT
            val f2Color = f2.color?.let { Color.parseColor(it) } ?: Color.TRANSPARENT
            val f1Luminance = ColorUtils.calculateLuminance(f1Color)
            val f2Luminance = ColorUtils.calculateLuminance(f2Color)
            f1Luminance.compareTo(f2Luminance)
        }

        private fun createManualComparator(ids: List<String>) = Comparator<Filter> { f1, f2 ->
            val idx1 = ids.indexOf(f1.uid)
            val idx2 = ids.indexOf(f2.uid)
            idx1.compareTo(idx2)
        }

        fun sorted(group: Filter, filters: List<Filter>): List<Filter> {
            val comparator =
                when (group.sortBy) {
                    SortBy.NOTES_COUNT_ASC -> NOTES_COUNT_COMPARATOR.then(NAME_COMPARATOR)
                    SortBy.NOTES_COUNT_DESC -> reversed(NOTES_COUNT_COMPARATOR).then(NAME_COMPARATOR)

                    SortBy.COLOR_ASC -> COLOR_COMPARATOR.then(NAME_COMPARATOR)
                    SortBy.COLOR_DESC -> reversed(COLOR_COMPARATOR).then(NAME_COMPARATOR)

                    SortBy.MANUAL_ASC -> createManualComparator(group.tagIds).then(NAME_COMPARATOR)
                    SortBy.MANUAL_DESC -> reversed(createManualComparator(group.tagIds)).then(NAME_COMPARATOR)

                    SortBy.NAME_ASC -> NAME_COMPARATOR
                    SortBy.NAME_DESC -> reversed(NAME_COMPARATOR)

                    else -> NAME_COMPARATOR
                }
            return filters.sortedWith(comparator)
        }
    }

}