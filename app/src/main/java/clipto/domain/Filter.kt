package clipto.domain

import clipto.common.extensions.notNull
import clipto.common.extensions.toNullIfEmpty
import clipto.common.misc.IdUtils
import clipto.dao.objectbox.model.toBox
import com.google.gson.annotations.SerializedName
import java.util.*

abstract class Filter {

    // basic
    open var objectType: ObjectType = ObjectType.INTERNAL

    open var uid: String? = null
    open var name: String? = null
    open var type: Type = Type.CUSTOM
    open var createDate: Date? = null
    open var updateDate: Date? = null
    open var syncDate: Date? = null
    open var notesCount: Long = 0
    open var filesCount: Long = 0
    open var hideHint: Boolean = false

    // visual
    open var limit: Int? = null
    open var color: String? = null
    open var sortBy: SortBy = SortBy.CREATE_DATE_DESC
    open var listStyle: ListStyle = ListStyle.DEFAULT

    // advanced
    open var description: String? = null
    open var autoRuleByTextIn: String? = null
    open var autoRulesEnabled: Boolean = false
    open var pinStarredEnabled: Boolean = false
    open var excludeWithCustomAttributes: Boolean = true

    open var tagIds: List<String> = emptyList()
    open var tagIdsWhereType: WhereType = WhereType.ANY_OF

    open var snippetSetIds: List<String> = emptyList()
    open var snippetSetIdsWhereType: WhereType = WhereType.ANY_OF

    open var textTypeIn: List<TextType> = emptyList()

    open var locatedInWhereType: WhereType = WhereType.ANY_OF
    open var textLike: String? = null
    open var starred: Boolean = false
    open var untagged: Boolean = false
    open var clipboard: Boolean = false
    open var recycled: Boolean = false
    open var snippets: Boolean = false

    open var showOnlyWithAttachments = false
    open var showOnlyWithPublicLink = false
    open var showOnlyNotSynced = false

    open var createDateFrom: Date? = null
    open var createDateTo: Date? = null
    open var createDatePeriod: TimePeriod? = null

    open var updateDateFrom: Date? = null
    open var updateDateTo: Date? = null
    open var updateDatePeriod: TimePeriod? = null

    open var activeFilterId: String? = null

    // public
    open var snippetKit: SnippetKit? = null

    // files
    open var folderId: String? = null
    open var fileTypes: List<FileType> = emptyList()
    open var fileIds: List<String> = emptyList()
    open var fileIdsWhereType: WhereType = WhereType.NONE_OF

    fun getTitle() = name.notNull()

    fun isFolder(): Boolean = folderId != null

    fun isManualSorting(): Boolean = sortBy == SortBy.MANUAL_ASC || sortBy == SortBy.MANUAL_DESC

    fun hasActiveFilter(): Boolean {
        if (activeFilterId != null && activeFilterId != Type.ALL.createUid()) {
            return true
        }
        return starred ||
                untagged ||
                clipboard ||
                recycled ||
                snippets ||
                tagIds.isNotEmpty() ||
                snippetSetIds.isNotEmpty() ||
                !textLike.isNullOrBlank() ||
                !doesNotHaveAdvancedFiltering(checkAll = true)
    }

    fun updateColor(color: String?) {
        this.color = color
    }

    open fun withKit(kit: SnippetKit) {
        snippetKit = kit
        name = kit.name
        color = kit.color
        sortBy = kit.sortBy
        listStyle = kit.listStyle
        description = kit.description
    }

    open fun apply(from: Filter): Filter {
        uid = from.uid
        name = from.name
        type = from.type
        objectType = from.objectType
        snippetKit = from.snippetKit
        description = from.description
        createDate = from.createDate
        updateDate = from.updateDate
        notesCount = from.notesCount
        hideHint = from.hideHint
        return withFilter(from)
    }

    fun withFilter(from: Filter, withAllAttrs: Boolean = false): Filter {
        if (withAllAttrs) {
            type = from.type
            name = from.name
            objectType = from.objectType
            description = from.description
            createDate = from.createDate
            updateDate = from.updateDate
            snippetKit = from.snippetKit
            hideHint = from.hideHint
        }

        limit = from.limit
        color = from.color
        sortBy = from.sortBy
        listStyle = from.listStyle

        autoRulesEnabled = from.autoRulesEnabled
        autoRuleByTextIn = from.autoRuleByTextIn
        pinStarredEnabled = from.pinStarredEnabled
        excludeWithCustomAttributes = from.excludeWithCustomAttributes

        tagIds = from.tagIds
        tagIdsWhereType = from.tagIdsWhereType

        snippetSetIds = from.snippetSetIds
        snippetSetIdsWhereType = from.snippetSetIdsWhereType

        textLike = from.textLike
        starred = from.starred
        untagged = from.untagged
        clipboard = from.clipboard
        recycled = from.recycled
        snippets = from.snippets

        locatedInWhereType = from.locatedInWhereType

        showOnlyWithPublicLink = from.showOnlyWithPublicLink
        showOnlyWithAttachments = from.showOnlyWithAttachments
        showOnlyNotSynced = from.showOnlyNotSynced

        createDatePeriod = from.createDatePeriod
        createDateFrom = from.createDateFrom
        createDateTo = from.createDateTo

        updateDatePeriod = from.updateDatePeriod
        updateDateFrom = from.updateDateFrom
        updateDateTo = from.updateDateTo

        textTypeIn = from.textTypeIn

        activeFilterId = from.activeFilterId

        folderId = from.folderId
        fileIds = from.fileIds
        fileTypes = from.fileTypes
        fileIdsWhereType = from.fileIdsWhereType

        return this
    }

    open fun withSnapshot(from: Snapshot): Filter {
        sortBy = from.sortBy
        tagIds = from.tagIds
        textLike = from.textLike
        starred = from.starred
        untagged = from.untagged
        listStyle = from.listStyle
        clipboard = from.clipboard
        recycled = from.recycled
        snippets = from.snippets
        activeFilterId = from.filterId
        tagIdsWhereType = from.tagIdsWhereType
        showOnlyWithPublicLink = from.showOnlyWithPublicLink
        showOnlyWithAttachments = from.showOnlyWithAttachments
        locatedInWhereType = from.locatedInWhereType
        showOnlyNotSynced = from.showOnlyNotSynced
        textTypeIn = from.textTypeIn
        createDateFrom = from.createDateFrom
        createDateTo = from.createDateTo
        createDatePeriod = from.createDatePeriod
        updateDateTo = from.updateDateTo
        updateDateFrom = from.updateDateFrom
        updateDatePeriod = from.updateDatePeriod
        snippetSetIds = from.snippetSetIds
        snippetSetIdsWhereType = from.snippetSetIdsWhereType
        folderId = from.folderIds.firstOrNull()
        fileIds = from.fileIds
        fileTypes = from.fileTypes
        fileIdsWhereType = from.fileIdsWhereType
        return this
    }

    private fun doesNotHaveAdvancedFiltering(checkAll: Boolean = false, noTags: Boolean = false, noSnippets: Boolean = false): Boolean {
        return textLike.isNullOrEmpty()
                && !showOnlyWithAttachments
                && !showOnlyWithPublicLink
                && !showOnlyNotSynced
                && textTypeIn.isEmpty()
                && createDatePeriod == null
                && createDateTo == null
                && createDateFrom == null
                && updateDatePeriod == null
                && updateDateTo == null
                && updateDateFrom == null
                && (!noTags || tagIds.isEmpty())
                && (!noSnippets || snippetSetIds.isEmpty())
                && (locatedInWhereType == WhereType.ANY_OF || (checkAll && !starred && !snippets && !untagged && !clipboard && !recycled))
                && !isFolder()
                && fileTypes.isEmpty()
                && fileIds.isEmpty()

    }

    fun isNew(): Boolean = uid.isNullOrBlank() || toBox().localId == 0L

    fun hasNotes(): Boolean = notesCount > 0L

    fun isContext(): Boolean = type == Type.CONTEXT

    fun isLast(): Boolean = type == Type.LAST

    fun isGroupTags(): Boolean = type == Type.GROUP_TAGS

    fun isGroupFiles(): Boolean = type == Type.GROUP_FILES

    fun isGroupFolders(): Boolean = type == Type.GROUP_FOLDERS

    fun isGroupNotes(): Boolean = type == Type.GROUP_NOTES

    fun isGroupFilters(): Boolean = type == Type.GROUP_FILTERS

    fun isGroupSnippets(): Boolean = type == Type.GROUP_SNIPPETS

    fun isGroup(): Boolean = isGroupTags() || isGroupNotes() || isGroupFilters() || isGroupSnippets() || isGroupFiles() || isGroupFolders()

    fun isNamedFilter(): Boolean = type == Type.NAMED

    fun isNotesCategory(): Boolean = type in listOf(Type.ALL, Type.STARRED, Type.UNTAGGED, Type.CLIPBOARD, Type.SNIPPETS, Type.DELETED)

    fun hasActiveAutoRule(): Boolean = isTag() && autoRulesEnabled && !autoRuleByTextIn.isNullOrBlank()

    fun isNameEditable(): Boolean = isNamedFilter() || isTag() || isSnippetKit()

    fun isAll(): Boolean {
        if (type == Type.ALL) {
            return true
        }
        return !starred &&
                !untagged &&
                !clipboard &&
                !recycled &&
                !snippets &&
                !isNamedFilter() &&
                !isGroup() &&
                doesNotHaveAdvancedFiltering(noTags = true, noSnippets = true)
    }

    fun isStarred(): Boolean {
        if (type == Type.STARRED) {
            return true
        }
        return starred &&
                !untagged &&
                !clipboard &&
                !recycled &&
                !snippets &&
                doesNotHaveAdvancedFiltering(noTags = true, noSnippets = true)
    }

    fun isSnippets(): Boolean {
        if (type == Type.SNIPPETS) {
            return true
        }
        return snippets &&
                !starred &&
                !untagged &&
                !clipboard &&
                !recycled &&
                doesNotHaveAdvancedFiltering(noTags = true, noSnippets = true)
    }

    fun isUntagged(): Boolean {
        if (type == Type.UNTAGGED) {
            return true
        }
        return untagged &&
                !starred &&
                !clipboard &&
                !recycled &&
                !snippets &&
                doesNotHaveAdvancedFiltering(noTags = true, noSnippets = true)
    }

    fun isClipboard(): Boolean {
        if (type == Type.CLIPBOARD) {
            return true
        }
        return clipboard &&
                !untagged &&
                !starred &&
                !recycled &&
                !snippets &&
                doesNotHaveAdvancedFiltering(noTags = true, noSnippets = true)
    }

    fun isDeleted(): Boolean {
        if (type == Type.DELETED) {
            return true
        }
        return recycled &&
                !untagged &&
                !starred &&
                !clipboard &&
                !snippets &&
                doesNotHaveAdvancedFiltering(noTags = true, noSnippets = true)
    }

    fun isTag(filters: Filters? = null): Boolean {
        if (type == Type.TAG) {
            return true
        }
        if (filters != null) {
            return !recycled &&
                    !untagged &&
                    !starred &&
                    !clipboard &&
                    !snippets &&
                    tagIds.singleOrNull()?.let { filters.findFilterByTagId(it) } != null &&
                    doesNotHaveAdvancedFiltering(noSnippets = true)
        }
        return false
    }

    fun isSnippetKit(filters: Filters? = null): Boolean {
        if (type == Type.SNIPPET_KIT) {
            return true
        }
        if (filters != null) {
            return !recycled &&
                    !untagged &&
                    !starred &&
                    !clipboard &&
                    !snippets &&
                    snippetSetIds.singleOrNull()?.let { filters.findFilterBySnippetKitId(it) } != null &&
                    doesNotHaveAdvancedFiltering(noTags = true)
        }
        return false
    }

    fun isSame(second: Filter): Boolean {
        val first = this
        return first.name == second.name
                && first.uid == second.uid
                && first.objectType == second.objectType
                && first.type == second.type
                && first.limit == second.limit
                && first.color == second.color
                && first.sortBy == second.sortBy
                && first.hideHint == second.hideHint
                && first.listStyle == second.listStyle
                && first.autoRulesEnabled == second.autoRulesEnabled
                && first.autoRuleByTextIn == second.autoRuleByTextIn
                && first.pinStarredEnabled == second.pinStarredEnabled
                && first.excludeWithCustomAttributes == second.excludeWithCustomAttributes
                && first.textLike == second.textLike
                && first.starred == second.starred
                && first.untagged == second.untagged
                && first.clipboard == second.clipboard
                && first.recycled == second.recycled
                && first.snippets == second.snippets
                && first.tagIds == second.tagIds
                && first.tagIdsWhereType == second.tagIdsWhereType
                && first.snippetSetIds == second.snippetSetIds
                && first.snippetSetIdsWhereType == second.snippetSetIdsWhereType
                && first.showOnlyWithPublicLink == second.showOnlyWithPublicLink
                && first.showOnlyWithAttachments == second.showOnlyWithAttachments
                && first.showOnlyNotSynced == second.showOnlyNotSynced
                && first.textTypeIn == second.textTypeIn
                && first.createDateFrom == second.createDateFrom
                && first.createDateTo == second.createDateTo
                && first.createDatePeriod == second.createDatePeriod
                && first.updateDateFrom == second.updateDateFrom
                && first.updateDateTo == second.updateDateTo
                && first.updateDatePeriod == second.updateDatePeriod
                && first.locatedInWhereType == second.locatedInWhereType
                && first.description == second.description
                && first.snippetKit.isSame(second.snippetKit)
                && first.folderId == second.folderId
                && first.fileIds == second.fileIds
                && first.fileIdsWhereType == second.fileIdsWhereType
                && first.fileTypes == second.fileTypes
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Filter

        if (uid != other.uid) return false

        return true
    }

    override fun hashCode(): Int {
        return uid?.hashCode() ?: 0
    }

    enum class Type(val id: Int, val orderBy: Int = 0) {
        ALL(id = 0) {
            override fun createUid(): String = "all"
        },
        UNTAGGED(id = 1, orderBy = 2) {
            override fun createUid(): String = "untagged"
        },
        STARRED(id = 2, orderBy = 1) {
            override fun createUid(): String = "starred"
        },
        CLIPBOARD(id = 3, orderBy = 3) {
            override fun createUid(): String = "clipboard"
        },
        DELETED(id = 4, orderBy = 5) {
            override fun createUid(): String = "deleted"
        },
        LAST(id = 5) {
            override fun createUid(): String = "last"
        },
        CONTEXT(id = 6) {
            override fun createUid(): String = "context"
        },
        SNIPPETS(id = 11, orderBy = 4) {
            override fun createUid(): String = "snippets"
        },
        TEXPANDER(id = 12) {
            override fun createUid(): String = "texpander"
        },
        GROUP_NOTES(id = 13) {
            override fun createUid(): String = "notes"
        },
        GROUP_FILTERS(id = 14) {
            override fun createUid(): String = "filters"
        },
        GROUP_TAGS(id = 15) {
            override fun createUid(): String = "tags"
        },
        GROUP_SNIPPETS(id = 16) {
            override fun createUid(): String = "group_snippets"
        },
        GROUP_FILES(id = 18) {
            override fun createUid(): String = "group_files"
        },
        GROUP_FOLDERS(id = 19) {
            override fun createUid(): String = "group_folders"
        },
        FOLDERS(id = 21) {
            override fun createUid(): String = "folders"
        },
        TAG(id = 7, orderBy = 6),
        LINK(id = 8),
        CUSTOM(id = 9),
        NAMED(id = 10),
        SNIPPET_KIT(id = 17),
        FOLDER(id = 20),
        ;

        open fun createUid() = IdUtils.autoId()

        companion object {
            private val types = enumValues<Type>().sortedBy { it.id }.toTypedArray()
            fun byId(id: Int?) = types.getOrElse(id ?: 0) { CUSTOM }
        }
    }

    data class Snapshot(
        val filterId: String? = null,
        val tagIds: List<String> = emptyList(),
        val tagIdsWhereType: WhereType = WhereType.ANY_OF,
        val locatedInWhereType: WhereType = WhereType.ANY_OF,
        val starred: Boolean = false,
        val untagged: Boolean = false,
        val clipboard: Boolean = false,
        val recycled: Boolean = false,
        val snippets: Boolean = false,
        var pinStarred: Boolean = false,
        val pinSnippets: Boolean = false,
        var textLike: String? = null,
        val sortBy: SortBy = SortBy.CREATE_DATE_DESC,
        val showOnlyWithPublicLink: Boolean = false,
        val showOnlyWithAttachments: Boolean = false,
        val showOnlyNotSynced: Boolean = false,
        val textTypeIn: List<TextType> = emptyList(),
        val createDateFrom: Date? = null,
        val createDateTo: Date? = null,
        val createDatePeriod: TimePeriod? = null,
        val updateDateFrom: Date? = null,
        val updateDateTo: Date? = null,
        val updateDatePeriod: TimePeriod? = null,
        val cleanupRequest: Boolean = false,
        val snippetSetIds: List<String> = emptyList(),
        val snippetSetIdsWhereType: WhereType = WhereType.ANY_OF,
        val listStyle: ListStyle = ListStyle.DEFAULT,

        val folderIds: List<String> = emptyList(),
        val fileIds: List<String> = emptyList(),
        val fileIdsWhereType: WhereType = WhereType.NONE_OF,
        val fileTypes: List<FileType> = emptyList(),
        val fileTypesWhereType: WhereType = WhereType.ANY_OF,

        val clipIds: List<String> = emptyList(),
        val clipIdsWhereType: WhereType = WhereType.NONE_OF,
    ) {

        fun isFolder(): Boolean = folderIds.isNotEmpty()
        fun isFolderFlat():Boolean = folderIds.size > 1

        fun copy(from: Filter): Snapshot = copy(
            filterId = from.uid,
            tagIds = from.tagIds,
            tagIdsWhereType = from.tagIdsWhereType,
            textLike = from.textLike,
            starred = from.starred,
            untagged = from.untagged,
            clipboard = from.clipboard,
            recycled = from.recycled,
            snippets = from.snippets,
            sortBy = from.sortBy,
            showOnlyWithPublicLink = from.showOnlyWithPublicLink,
            showOnlyWithAttachments = from.showOnlyWithAttachments,
            showOnlyNotSynced = from.showOnlyNotSynced,
            textTypeIn = from.textTypeIn,
            createDateFrom = from.createDateFrom,
            createDateTo = from.createDateTo,
            createDatePeriod = from.createDatePeriod,
            updateDateFrom = from.updateDateFrom,
            updateDateTo = from.updateDateTo,
            updateDatePeriod = from.updateDatePeriod,
            locatedInWhereType = from.locatedInWhereType,
            snippetSetIds = from.snippetSetIds,
            snippetSetIdsWhereType = from.snippetSetIdsWhereType,
            listStyle = ListStyle.getStyle(from),
            folderIds = from.folderId?.let { listOf(it) } ?: emptyList(),
            fileIds = from.fileIds,
            fileTypes = from.fileTypes,
            fileIdsWhereType = from.fileIdsWhereType
        )
    }

    enum class WhereType(val id: Int) {

        @SerializedName("0")
        ANY_OF(0),

        @SerializedName("1")
        ALL_OF(1),

        @SerializedName("2")
        NONE_OF(2)
        ;

        companion object {
            fun byId(id: Int?): WhereType {
                return when (id) {
                    ALL_OF.id -> ALL_OF
                    NONE_OF.id -> NONE_OF
                    else -> ANY_OF
                }
            }
        }

    }

    companion object

}