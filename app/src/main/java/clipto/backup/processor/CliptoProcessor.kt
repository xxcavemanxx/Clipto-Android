package clipto.backup.processor

import android.content.ContentResolver
import android.net.Uri
import clipto.analytics.Analytics
import clipto.backup.BackupItemType
import clipto.backup.BackupProcessor
import clipto.backup.IBackupProcessor
import clipto.common.misc.GsonUtils
import clipto.dao.objectbox.model.ClipBox
import clipto.dao.objectbox.model.FilterBox
import clipto.domain.*
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.scopes.ViewModelScoped
import java.io.FileInputStream
import java.io.FileWriter
import java.util.*
import javax.inject.Inject

@ViewModelScoped
class CliptoProcessor @Inject constructor() : BackupProcessor() {

    override fun backup(contentResolver: ContentResolver, uri: Uri, list: List<Clip>, types: List<BackupItemType>): IBackupProcessor.BackupStats {
        val withNotes = types.contains(BackupItemType.NOTES)
        val withTags = types.contains(BackupItemType.TAGS)
        val withFilters = types.contains(BackupItemType.FILTERS)
        val withSettings = types.contains(BackupItemType.SETTINGS)
        val withSnippetKits = types.contains(BackupItemType.SNIPPET_KITS)

        val clips = list.filter { withNotes || (withSnippetKits && it.snippetSetsIds.isNotEmpty()) }
        val filters = mutableListOf<Filter>()

        // settings
        val settings: Settings? =
            if (withSettings) {
                Settings().apply(settingsBoxDao.get())
            } else {
                null
            }

        // tags
        if (withTags) {
            val tags = appState.getFilters().getSortedTags()
            filters.addAll(tags)
        } else {
            val tags = clips
                .map { it.tagIds }
                .flatten()
                .distinct()
                .mapNotNull { appState.getFilters().findFilterByTagId(it) }
            filters.addAll(tags)
        }

        // filters
        if (withFilters) {
            val namedFilters = appState.getFilters().getNamedFilters()
            filters.addAll(namedFilters)
        }

        // snippet kits
        if (withSnippetKits) {
            val kits = appState.getFilters().getSortedSnippetKits()
            filters.addAll(kits)
        } else {
            val kits = clips
                .map { it.snippetSetsIds }
                .flatten()
                .distinct()
                .mapNotNull { appState.getFilters().findFilterBySnippetKitId(it) }
            filters.addAll(kits)
        }

        val backup = CliptoBackup(
            filters = filters.map { map(it) },
            notes = clips.map { map(it) },
            settings = settings,
            created = Date()
        )
        contentResolver.openFileDescriptor(uri, "w")?.use {
            FileWriter(it.fileDescriptor).use { writer ->
                GsonUtils.get().toJson(backup, writer)
            }
        }

        return IBackupProcessor.BackupStats(
            notes = clips.size,
            settings = settings != null,
            tags = filters.count { it.isTag() },
            filters = filters.count { it.isNamedFilter() },
            snippetKits = filters.count { it.isSnippetKit() }
        )
    }

    override fun restore(contentResolver: ContentResolver, uri: Uri): IBackupProcessor.BackupStats {
        var tagsCount = 0
        var notesCount = 0
        var filtersCount = 0
        var snippetKitsCount = 0
        var withSettings = false
        val restoredClips = mutableListOf<Clip>()
        contentResolver.openFileDescriptor(uri, "r")?.use {
            FileInputStream(it.fileDescriptor).use { stream ->
                val reader = stream.reader()
                val backupJson = GsonUtils.get().fromJson(reader, CliptoBackup::class.java)
                if (backupJson.isEmpty()) {
                    return IBackupProcessor.BackupStats()
                }
                val transactionDate = Date()
                txHelper.inTx {
                    if (backupJson.settings != null) {
                        settingsBoxDao.update { settings ->
                            settings.apply(backupJson.settings)
                            true
                        }
                        withSettings = true
                    }

                    val allClips = clipBoxDao.getAllClips()
                    backupJson.filters.forEach {
                        val filter = filterBoxDao.getByUid(it.uid) ?: it.toFilter()
                        if (filter.name != null) {
                            filterBoxDao.save(filter)
                            when {
                                filter.isTag() -> tagsCount++
                                filter.isNamedFilter() -> filtersCount++
                                filter.isSnippetKit() -> snippetKitsCount++
                            }
                        }
                    }
                    backupJson.notes.forEach { note ->
                        val tagIds = note.tags?.mapNotNull { tag -> filterBoxDao.getOrSaveByName(tag)?.uid }
                        val clip = note.toClip()
                        if (!tagIds.isNullOrEmpty()) {
                            clip.tagIds = tagIds
                        }
                        if (restore(allClips, clip, transactionDate)) {
                            restoredClips.add(clip)
                            notesCount++
                        }
                    }
                }
                if (notesCount > 0) {
                    Analytics.onRestoreFromClipto()
                }
            }
        }
        return IBackupProcessor.BackupStats(
            notes = notesCount,
            tags = tagsCount,
            filters = filtersCount,
            snippetKits = snippetKitsCount,
            settings = withSettings,
            clips = restoredClips
        )
    }

    private fun map(clip: Clip): CliptoItem = CliptoItem(
        createDate = clip.createDate,
        updateDate = clip.updateDate,
        modifyDate = clip.modifyDate,
        deleteDate = clip.deleteDate,
        textType = clip.textType,
        fav = clip.fav,
        text = clip.text,
        title = clip.title,
        tracked = clip.tracked,
        usageCount = clip.usageCount,
        platform = clip.platform,
        fileIds = clip.fileIds,

        tagIds = clip.tagIds,
        objectType = clip.objectType,
        snippetId = clip.snippetId,
        snippetSetsIds = clip.snippetSetsIds,
        abbreviation = clip.abbreviation,
        description = clip.description,
        firestoreId = clip.firestoreId
    )

    private fun map(from: Filter): CliptoFilter = CliptoFilter(
        created = from.createDate,
        updated = from.updateDate,
        uid = from.uid,
        name = from.name,
        type = from.type.id,
        limit = from.limit,
        color = from.color,
        sortBy = from.sortBy.id,
        listStyle = from.listStyle.id,
        autoRuleByTextIn = from.autoRuleByTextIn,
        autoRulesEnabled = from.autoRulesEnabled,
        textLike = from.textLike,
        recycled = from.recycled,
        clipboard = from.clipboard,
        untagged = from.untagged,
        starred = from.starred,
        snippets = from.snippets,
        tagIds = from.tagIds,
        objectType = from.objectType,
        hideHint = from.hideHint,
        description = from.description,
        pinStarredEnabled = from.pinStarredEnabled,
        excludeWithCustomAttributes = from.excludeWithCustomAttributes,
        tagIdsWhereType = from.tagIdsWhereType,
        snippetSetIds = from.snippetSetIds,
        snippetSetIdsWhereType = from.snippetSetIdsWhereType,
        textTypeIn = from.textTypeIn,
        locatedInWhereType = from.locatedInWhereType,
        showOnlyWithAttachments = from.showOnlyWithAttachments,
        showOnlyWithPublicLink = from.showOnlyWithPublicLink,
        showOnlyNotSynced = from.showOnlyNotSynced,
        createDateFrom = from.createDateFrom,
        createDateTo = from.createDateTo,
        createDatePeriod = from.createDatePeriod,
        updateDateFrom = from.updateDateFrom,
        updateDateTo = from.updateDateTo,
        updateDatePeriod = from.updateDatePeriod,
        snippetKit = from.snippetKit
    )

    private data class CliptoBackup(
        @SerializedName("created") val created: Date,
        @SerializedName("settings") val settings: Settings? = null,
        @SerializedName("notes") val notes: List<CliptoItem> = emptyList(),
        @SerializedName("filters") val filters: List<CliptoFilter> = emptyList()
    ) {
        fun isEmpty(): Boolean = settings == null && notes.isEmpty() && filters.isEmpty()
    }

    private data class CliptoItem(
        @SerializedName("created") val createDate: Date?,
        @SerializedName("updated") val updateDate: Date?,
        @SerializedName("modified") val modifyDate: Date?,
        @SerializedName("deleted") val deleteDate: Date?,
        @SerializedName("type") val textType: TextType,
        @Deprecated("not used anymore prior to tagIds")
        @SerializedName("tags") val tags: List<String>? = null,
        @SerializedName("title") val title: String?,
        @SerializedName("text") val text: String?,
        @SerializedName("used") val usageCount: Int,
        @SerializedName("tracked") val tracked: Boolean,
        @SerializedName("fav") val fav: Boolean,
        @SerializedName("platform") val platform: String? = null,
        @SerializedName("fileIds") val fileIds: List<String>? = null,
        @SerializedName("tagIds") val tagIds: List<String>? = null,
        @SerializedName("objectType") val objectType: ObjectType? = null,
        @SerializedName("snippetId") val snippetId: String? = null,
        @SerializedName("snippetSetsIds") val snippetSetsIds: List<String>? = null,
        @SerializedName("abbreviation") val abbreviation: String? = null,
        @SerializedName("description") val description: String? = null,
        @SerializedName("uid") val firestoreId: String? = null
    ) {
        fun toClip(): ClipBox = ClipBox().also {
            it.textType = textType
            it.updateDate = updateDate
            it.createDate = createDate
            it.deleteDate = deleteDate
            it.modifyDate = modifyDate
            it.usageCount = usageCount
            it.tracked = tracked
            it.title = title
            it.text = text
            it.fav = fav
            it.platform = platform
            it.fileIds = fileIds ?: emptyList()
            it.tagIds = tagIds ?: emptyList()
            it.objectType = objectType ?: ObjectType.INTERNAL
            it.snippetId = snippetId
            it.snippetSetsIds = snippetSetsIds ?: emptyList()
            it.abbreviation = abbreviation
            it.description = description
            it.firestoreId = firestoreId
        }
    }

    private data class CliptoFilter(
        @SerializedName("created") val created: Date?,
        @SerializedName("updated") val updated: Date?,
        @SerializedName("uid") val uid: String?,
        @SerializedName("name") val name: String?,
        @SerializedName("type") val type: Int,
        @SerializedName("limit") val limit: Int?,
        @SerializedName("color") val color: String?,
        @SerializedName("sortBy") val sortBy: Int,
        @SerializedName("listStyle") val listStyle: Int,
        @SerializedName("autoRuleByTextIn") val autoRuleByTextIn: String?,
        @SerializedName("autoRulesEnabled") val autoRulesEnabled: Boolean,
        @SerializedName("tagsIn") @Deprecated("not used anymore prior to tagIds") val tagsIn: List<String>? = null,
        @SerializedName("tagIds") val tagIds: List<String>? = null,
        @SerializedName("textLike") val textLike: String? = null,
        @SerializedName("starred") val starred: Boolean = false,
        @SerializedName("untagged") val untagged: Boolean = false,
        @SerializedName("clipboard") val clipboard: Boolean = false,
        @SerializedName("recycled") val recycled: Boolean = false,
        @SerializedName("snippets") val snippets: Boolean = false,
        @SerializedName("objectType") val objectType: ObjectType? = null,
        @SerializedName("hideHint") val hideHint: Boolean = false,
        @SerializedName("description") val description: String? = null,
        @SerializedName("pinStarredEnabled") val pinStarredEnabled: Boolean = false,
        @SerializedName("excludeWithCustomAttributes") val excludeWithCustomAttributes: Boolean = true,
        @SerializedName("tagIdsWhereType") val tagIdsWhereType: Filter.WhereType? = null,
        @SerializedName("snippetSetIds") val snippetSetIds: List<String>? = null,
        @SerializedName("snippetSetIdsWhereType") val snippetSetIdsWhereType: Filter.WhereType? = null,
        @SerializedName("textTypeIn") val textTypeIn: List<TextType>? = null,
        @SerializedName("locatedInWhereType") val locatedInWhereType: Filter.WhereType? = null,
        @SerializedName("showOnlyWithAttachments") val showOnlyWithAttachments: Boolean = false,
        @SerializedName("showOnlyWithPublicLink") val showOnlyWithPublicLink: Boolean = false,
        @SerializedName("showOnlyNotSynced") val showOnlyNotSynced: Boolean = false,
        @SerializedName("createDateFrom") val createDateFrom: Date? = null,
        @SerializedName("createDateTo") val createDateTo: Date? = null,
        @SerializedName("createDatePeriod") val createDatePeriod: TimePeriod? = null,
        @SerializedName("updateDateFrom") val updateDateFrom: Date? = null,
        @SerializedName("updateDateTo") val updateDateTo: Date? = null,
        @SerializedName("updateDatePeriod") val updateDatePeriod: TimePeriod? = null,
        @SerializedName("snippetKit") val snippetKit: SnippetKit? = null
    ) {
        fun toFilter() = FilterBox().also {
            it.createDate = created
            it.updateDate = updated
            it.uid = uid
            it.name = name
            it.type = Filter.Type.byId(type)
            it.limit = limit
            it.color = color
            it.tagIds = tagIds ?: tagsIn?.let { listOf(uid!!) } ?: emptyList()
            it.sortBy = SortBy.byId(sortBy)
            it.listStyle = ListStyle.byId(listStyle)
            it.autoRuleByTextIn = autoRuleByTextIn
            it.autoRulesEnabled = autoRulesEnabled
            it.textLike = textLike
            it.starred = starred
            it.untagged = untagged
            it.clipboard = clipboard
            it.recycled = recycled
            it.snippets = snippets
            it.objectType = objectType ?: ObjectType.INTERNAL
            it.hideHint = hideHint
            it.description = description
            it.pinStarredEnabled = pinStarredEnabled
            it.excludeWithCustomAttributes = excludeWithCustomAttributes
            it.tagIdsWhereType = tagIdsWhereType ?: Filter.WhereType.ANY_OF
            it.snippetSetIds = snippetSetIds ?: emptyList()
            it.snippetSetIdsWhereType = snippetSetIdsWhereType ?: Filter.WhereType.ANY_OF
            it.textTypeIn = textTypeIn ?: emptyList()
            it.locatedInWhereType = locatedInWhereType ?: Filter.WhereType.ANY_OF
            it.showOnlyWithAttachments = showOnlyWithAttachments
            it.showOnlyWithPublicLink = showOnlyWithPublicLink
            it.showOnlyNotSynced = showOnlyNotSynced
            it.createDateFrom = createDateFrom
            it.createDateTo = createDateTo
            it.createDatePeriod = createDatePeriod
            it.updateDateFrom = updateDateFrom
            it.updateDateTo = updateDateTo
            it.updateDatePeriod = updateDatePeriod
            it.snippetKit = snippetKit
        }
    }

}