package clipto.dao.objectbox

import clipto.common.extensions.threadLocal
import clipto.dao.objectbox.model.FilterBox
import clipto.dao.objectbox.model.FilterBox_
import clipto.dao.objectbox.model.toBox
import clipto.domain.*
import clipto.extensions.createTag
import clipto.extensions.getTitle
import clipto.extensions.log
import clipto.extensions.normalize
import dagger.Lazy
import io.objectbox.kotlin.inValues
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterBoxDao @Inject constructor(
    settingsBoxDao: Lazy<SettingsBoxDao>
) : AbstractBoxDao<FilterBox>() {

    private val _filters: Filters by lazy {
        val allFilters = box.all
        val initial = allFilters.isEmpty()

        val createdFilters = mutableListOf<FilterBox>()
        val transactionDate = Date()

        val all = allFilters.find { it.type == Filter.Type.ALL }
            ?: FilterBox().apply {
                uid = Filter.Type.ALL.createUid()
                sortBy = SortBy.CREATE_DATE_DESC
                createDate = transactionDate
                updateDate = transactionDate
                type = Filter.Type.ALL
            }.also {
                createdFilters.add(it)
            }

        val starred = allFilters.find { it.type == Filter.Type.STARRED }
            ?: FilterBox().apply {
                uid = Filter.Type.STARRED.createUid()
                sortBy = SortBy.CREATE_DATE_DESC
                createDate = transactionDate
                updateDate = transactionDate
                type = Filter.Type.STARRED
                starred = true
            }.also {
                createdFilters.add(it)
            }

        val untagged = allFilters.find { it.type == Filter.Type.UNTAGGED }
            ?: FilterBox().apply {
                uid = Filter.Type.UNTAGGED.createUid()
                sortBy = SortBy.CREATE_DATE_DESC
                createDate = transactionDate
                updateDate = transactionDate
                type = Filter.Type.UNTAGGED
                untagged = true
            }.also {
                createdFilters.add(it)
            }

        val snippets = allFilters.find { it.type == Filter.Type.SNIPPETS }
            ?: FilterBox().apply {
                uid = Filter.Type.SNIPPETS.createUid()
                sortBy = SortBy.CREATE_DATE_DESC
                createDate = transactionDate
                updateDate = transactionDate
                type = Filter.Type.SNIPPETS
                snippets = true
            }.also {
                createdFilters.add(it)
            }

        val clipboard = allFilters.find { it.type == Filter.Type.CLIPBOARD }
            ?: FilterBox().apply {
                uid = Filter.Type.CLIPBOARD.createUid()
                sortBy = SortBy.USAGE_DATE_DESC
                createDate = transactionDate
                updateDate = transactionDate
                type = Filter.Type.CLIPBOARD
                clipboard = true
            }.also {
                createdFilters.add(it)
            }
        if (clipboard.limit == null || clipboard.limit == 0) {
            clipboard.limit = appConfig.limitClipboardNotesDefault()
        }

        val deleted = allFilters.find { it.type == Filter.Type.DELETED }
            ?: FilterBox().apply {
                uid = Filter.Type.DELETED.createUid()
                sortBy = SortBy.DELETE_DATE_DESC
                createDate = transactionDate
                updateDate = transactionDate
                type = Filter.Type.DELETED
                recycled = true
            }.also {
                createdFilters.add(it)
            }
        if (deleted.limit == null || deleted.limit == 0) {
            deleted.limit = appConfig.limitDeletedNotesDefault()
        }

        val last = allFilters.find { it.type == Filter.Type.LAST }
            ?: FilterBox().apply {
                uid = Filter.Type.LAST.createUid()
                createDate = transactionDate
                updateDate = transactionDate
                type = Filter.Type.LAST
            }.also {
                createdFilters.add(it)
            }

        val texpander = allFilters.find { it.type == Filter.Type.TEXPANDER }
            ?: FilterBox().apply {
                uid = Filter.Type.TEXPANDER.createUid()
                sortBy = SortBy.CREATE_DATE_DESC
                createDate = transactionDate
                updateDate = transactionDate
                type = Filter.Type.TEXPANDER
            }.also {
                createdFilters.add(it)
            }

        val groupNotes = allFilters.find { it.type == Filter.Type.GROUP_NOTES }
            ?: FilterBox().apply {
                uid = Filter.Type.GROUP_NOTES.createUid()
                sortBy = SortBy.MANUAL_ASC
                createDate = transactionDate
                updateDate = transactionDate
                pinStarredEnabled = true
                type = Filter.Type.GROUP_NOTES
                tagIds = listOf(
                    Filter.Type.ALL.createUid(),
                    Filter.Type.STARRED.createUid(),
                    Filter.Type.UNTAGGED.createUid(),
                    Filter.Type.CLIPBOARD.createUid(),
                    Filter.Type.SNIPPETS.createUid(),
                    Filter.Type.DELETED.createUid()
                )
            }.also {
                createdFilters.add(it)
            }

        val groupFilters = allFilters.find { it.type == Filter.Type.GROUP_FILTERS }
            ?: FilterBox().apply {
                uid = Filter.Type.GROUP_FILTERS.createUid()
                sortBy = SortBy.NAME_ASC
                createDate = transactionDate
                updateDate = transactionDate
                type = Filter.Type.GROUP_FILTERS
                tagIds = Filters.sorted(this, allFilters.filter { it.isNamedFilter() }).mapNotNull { it.uid }
            }.also {
                createdFilters.add(it)
            }

        val groupSnippets = allFilters.find { it.type == Filter.Type.GROUP_SNIPPETS }
            ?: FilterBox().apply {
                uid = Filter.Type.GROUP_SNIPPETS.createUid()
                sortBy = SortBy.NAME_ASC
                createDate = transactionDate
                updateDate = transactionDate
                type = Filter.Type.GROUP_SNIPPETS
                tagIds = Filters.sorted(this, allFilters.filter { it.isSnippetKit() }).mapNotNull { it.uid }
            }.also {
                createdFilters.add(it)
            }

        val groupTags = allFilters.find { it.type == Filter.Type.GROUP_TAGS }
            ?: FilterBox().apply {
                uid = Filter.Type.GROUP_TAGS.createUid()
                sortBy = SortBy.NAME_ASC
                createDate = transactionDate
                updateDate = transactionDate
                type = Filter.Type.GROUP_TAGS
                tagIds = Filters.sorted(this, allFilters.filter { it.isTag() }).mapNotNull { it.uid }
            }.also {
                createdFilters.add(it)
            }

        val groupFiles = allFilters.find { it.type == Filter.Type.GROUP_FILES }
            ?: FilterBox().apply {
                uid = Filter.Type.GROUP_FILES.createUid()
                sortBy = SortBy.NAME_ASC
                createDate = transactionDate
                updateDate = transactionDate
                type = Filter.Type.GROUP_FILES
            }.also {
                createdFilters.add(it)
            }

        val groupFolders = allFilters.find { it.type == Filter.Type.GROUP_FOLDERS }
            ?: FilterBox().apply {
                uid = Filter.Type.GROUP_FOLDERS.createUid()
                sortBy = SortBy.NAME_ASC
                createDate = transactionDate
                updateDate = transactionDate
                type = Filter.Type.GROUP_FOLDERS
            }.also {
                createdFilters.add(it)
            }

        val folders = allFilters.find { it.type == Filter.Type.FOLDERS }
            ?: FilterBox().apply {
                uid = Filter.Type.FOLDERS.createUid()
                sortBy = SortBy.CREATE_DATE_DESC
                listStyle = ListStyle.FOLDERS
                createDate = transactionDate
                updateDate = transactionDate
                type = Filter.Type.FOLDERS
            }.also {
                createdFilters.add(it)
            }

        val filters = Filters(
            all = all.normalize(),
            untagged = untagged.normalize(),
            starred = starred.normalize(),
            clipboard = clipboard.normalize(),
            deleted = deleted.normalize(),
            last = last.normalize(),
            folders = folders.normalize(),
            snippets = snippets.normalize(),
            texpander = texpander.normalize(),
            groupTags = groupTags.normalize(),
            groupNotes = groupNotes.normalize(),
            groupFilters = groupFilters.normalize(),
            groupSnippets = groupSnippets.normalize(),
            groupFiles = groupFiles.normalize(),
            groupFolders = groupFolders.normalize(),
            filtersByUid = createdFilters.plus(allFilters)
                .filter { !it.uid.isNullOrBlank() }
                .associateBy { it.normalize().uid!! }
                .toMutableMap(),
            initial = initial
        )

        if (createdFilters.isNotEmpty()) {
            box.put(createdFilters)
        }

        filters.getTags().forEach { it.hideHint = true }
        filters.getCategories().forEach { it.name = it.getTitle(app) }
        if (!settingsBoxDao.get().get().restoreFilterOnStart) last.withFilter(all)

        filters
    }

    private val queryFilterByUid = threadLocal { box.query().equal(FilterBox_.uid, "").build() }

    override fun getType(): Class<FilterBox> = FilterBox::class.java

    override fun clear() {
        val filteredTypes = arrayOf(
            Filter.Type.TAG,
            Filter.Type.LINK,
            Filter.Type.NAMED,
            Filter.Type.CONTEXT,
            Filter.Type.CUSTOM,
            Filter.Type.SNIPPET_KIT
        )
        box.query()
            .inValues(FilterBox_.type, filteredTypes.map { it.id }.toIntArray())
            .build()
            .remove()
        getFilters().filtersByUid
            .filterValues { filteredTypes.contains(it.type) }
            .forEach { getFilters().filtersByUid.remove(it.key) }
        getFilters().last.withFilter(getFilters().all)
    }

    fun getFilters(): Filters = _filters

    fun getAll(): List<FilterBox> = box.all

    fun getByUid(uid: String?): FilterBox? {
        if (uid == null) return null
        return queryFilterByUid.get()!!.setParameter(FilterBox_.uid, uid).findFirst()
    }

    fun getOrSaveByName(name: String?): Filter? {
        if (name == null) return null
        val filter = getFilters().findFilterByTagName(name)
        if (filter != null) {
            return filter
        }
        val tagFilter = Filter.createTag(name)
        save(tagFilter.toBox())
        return tagFilter
    }

    fun save(filter: FilterBox) {
        val id = filter.uid ?: throw IllegalStateException("Please report this issue to the developer. Probably he implemented something wrongly.")
        filter.notesCount = maxOf(0L, filter.notesCount)
        filter.updateDate = Date()
        box.put(filter)
        log("FilterRepository :: save filter :: {} - {} - {}", id, filter.name, filter.type)
        getFilters().filtersByUid.getOrPut(id) { filter }.withFilter(filter, withAllAttrs = true)
    }

    fun remove(filter: FilterBox) {
        val id = filter.uid ?: return
        box.query().equal(FilterBox_.uid, id).build().remove()
        getFilters().filtersByUid.remove(id)
    }

    fun update(prev: AttributedObject?, next: AttributedObject?) {
        // tags
        val prevClipTags = prev?.tagIds ?: emptyList()
        val newClipTags = next?.tagIds ?: emptyList()
        if (prev?.isDeleted() != true) {
            prevClipTags.forEach { tagId ->
                if (!newClipTags.contains(tagId)) {
                    getFilters().findFilterByTagId(tagId, stubIfNotFound = true)?.let { tag ->
                        tag.notesCount -= 1
                        save(tag.toBox())
                    }
                }
            }
        }
        if (next?.isDeleted() != true) {
            newClipTags.forEach { tagId ->
                if (!prevClipTags.contains(tagId)) {
                    getFilters().findFilterByTagId(tagId, stubIfNotFound = true)?.let { tag ->
                        tag.notesCount += 1
                        save(tag.toBox())
                    }
                }
            }
        }

        // snippet kits
        val prevClipKits = prev?.snippetSetsIds ?: emptyList()
        val newClipKits = next?.snippetSetsIds ?: emptyList()
        if (prev?.isDeleted() != true) {
            prevClipKits.forEach { kitId ->
                if (!newClipKits.contains(kitId)) {
                    getFilters().findFilterBySnippetKitId(kitId, stubIfNotFound = true)?.let { kit ->
                        kit.notesCount -= 1
                        save(kit.toBox())
                    }
                }
            }
        }
        if (next?.isDeleted() != true) {
            newClipKits.forEach { kitId ->
                if (!prevClipKits.contains(kitId)) {
                    getFilters().findFilterBySnippetKitId(kitId, stubIfNotFound = true)?.let { kit ->
                        kit.notesCount += 1
                        save(kit.toBox())
                    }
                }
            }
        }

        // folders

    }

}