package clipto.dao.objectbox

import clipto.analytics.Analytics
import clipto.common.extensions.*
import clipto.common.logging.L
import clipto.common.misc.IdUtils
import clipto.dao.objectbox.model.ClipBox
import clipto.dao.objectbox.model.ClipBox_
import clipto.dao.objectbox.model.FileRefBox_
import clipto.dao.objectbox.model.toBox
import clipto.domain.*
import clipto.dynamic.DynamicField
import clipto.extensions.TextTypeExt
import clipto.extensions.isNew
import clipto.extensions.log
import dagger.Lazy
import io.objectbox.kotlin.inValues
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipBoxDao @Inject constructor(
    private val filterBoxDao: Lazy<FilterBoxDao>,
    private val fileBoxDao: Lazy<FileBoxDao>
) : AbstractBoxDao<ClipBox>() {

    private val clipsCountChanged = AtomicBoolean(true)

    private val queryAll by lazy {
        box.query()
            .order(ClipBox_.createDate)
            .build()
    }

    private val queryUntagged by lazy {
        box.query()
            .isNull(ClipBox_.tagIds)
            .isNull(ClipBox_.deleteDate)
            .build()
    }

    private val queryFolderIdEq = threadLocal {
        box.query()
            .equal(ClipBox_.folderId, "")
            .build()
    }

    private val querySnippets by lazy {
        box.query()
            .equal(ClipBox_.snippet, true)
            .isNull(ClipBox_.deleteDate)
            .build()
    }

    private val queryFav by lazy {
        box.query()
            .equal(ClipBox_.fav, true)
            .isNull(ClipBox_.deleteDate)
            .build()
    }

    private val queryRecycleBin by lazy {
        box.query()
            .notNull(ClipBox_.deleteDate)
            .order(ClipBox_.deleteDate)
            .build()
    }

    private val queryClipboard by lazy {
        box.query()
            .equal(ClipBox_.tracked, true)
            .isNull(ClipBox_.deleteDate)
            .build()
    }

    private val queryClipboardWithExcludedCustomAttrs by lazy {
        box.query()
            .equal(ClipBox_.tracked, true)
            .notEqual(ClipBox_.snippet, true)
            .notEqual(ClipBox_.fav, true)
            .isNull(ClipBox_.deleteDate)
            .isNull(ClipBox_.tagIds)
            .isNull(ClipBox_.title)
            .build()
    }

    private val queryClipboardCleanup by lazy {
        box.query()
            .equal(ClipBox_.tracked, true)
            .notEqual(ClipBox_.snippet, true)
            .notEqual(ClipBox_.fav, true)
            .isNull(ClipBox_.abbreviation)
            .isNull(ClipBox_.description)
            .isNull(ClipBox_.deleteDate)
            .isNull(ClipBox_.snippetSetsIds)
            .isNull(ClipBox_.fileIds)
            .isNull(ClipBox_.tagIds)
            .isNull(ClipBox_.title)
            .orderDesc(ClipBox_.updateDate)
            .build()
    }

    private val queryClipboardHistory by lazy {
        box.query()
            .isNull(ClipBox_.deleteDate)
            .orderDesc(ClipBox_.updateDate)
            .orderDesc(ClipBox_.createDate)
            .build()
    }

    private val queryNotSynced by lazy {
        box.query()
            .isNull(ClipBox_.firestoreId)
            .order(ClipBox_.modifyDate)
            .build()
    }

    private val querySynced by lazy {
        box.query()
            .notNull(ClipBox_.firestoreId)
            .build()
    }

    private val queryTextEq = threadLocal {
        box.query()
            .equal(ClipBox_.text, "", QueryBuilder.StringOrder.CASE_SENSITIVE)
            .orderDesc(ClipBox_.createDate)
            .build()
    }

    private val queryFirestoreIdEq = threadLocal {
        box.query()
            .equal(ClipBox_.firestoreId, "")
            .build()
    }

    private val querySnippetIdEq = threadLocal {
        box.query()
            .equal(ClipBox_.snippetId, "")
            .build()
    }

    fun postClipsCountChanged() = clipsCountChanged.set(true)

    fun consumeClipsCountChanged(): Boolean = clipsCountChanged.getAndSet(false)

    override fun getType(): Class<ClipBox> = ClipBox::class.java

    override fun clear() {
        if (appConfig.canRemoveNotSyncedNotesOnLogout()) {
            box.removeAll()
        } else {
            querySynced.remove()
            box.query().build().findLazy().forEach {
                filterBoxDao.get().update(null, it)
            }
        }
        postClipsCountChanged()
    }

    fun getChildren(folderId: String): List<Clip> {
        return queryFolderIdEq.getNotNull()
            .setParameter(ClipBox_.folderId, folderId)
            .find()
    }

    fun getByFirestoreId(id: String?): ClipBox? {
        if (id == null) return null
        return queryFirestoreIdEq.getNotNull().setParameter(ClipBox_.firestoreId, id).findFirst()
    }

    fun saveAll(clips: List<ClipBox>, modified: Boolean = false) {
        if (clips.isNotEmpty()) {
            val date = Date()
            val timestamp = date.time
            clips.forEach { clip ->
                shake(clip, timestamp)
                if (modified) {
                    clip.modifyDate = date
                }
            }
            postClipsCountChanged()
            box.put(clips)
        }
    }

    fun getClipboardClips(): List<ClipBox> {
        return queryClipboardCleanup.find()
    }

    fun getLegacyFiles(): List<ClipBox> {
        return box.query()
            .notNull(ClipBox_.files)
            .build()
            .find()
    }

    fun getLegacyTags(): List<ClipBox> {
        return box.query()
            .notNull(ClipBox_.tags)
            .build()
            .find()
    }

    fun getClipboardExceedingClips(): List<ClipBox> {
        val limit = filterBoxDao.get().getFilters().clipboard.limit
        if (limit != null && limit > 0) {
            val query = queryClipboardCleanup
            if (query.count() > limit) {
                val count = appConfig.limitClipboardNotesCleanupCount()
                val clips = query.find(limit.toLong(), count.toLong())
                log("delete exceed limit clip from clipboard : {}/{}", clips.size, count)
                return clips
            }
        }
        return emptyList()
    }

    fun getRecycleBinClips(): List<ClipBox> {
        return queryRecycleBin.find()
    }

    fun getRecycleBinExceedingClips(): List<ClipBox> {
        val limit = filterBoxDao.get().getFilters().deleted.limit ?: appConfig.limitDeletedNotesDefault()
        if (limit > 0) {
            val query = queryRecycleBin
            val exceedCount = query.count() - limit
            if (exceedCount > 0) {
                L.log(this, "delete exceed limit clips: {}", exceedCount)
                return query.find(0, exceedCount)
            }
        }
        return emptyList()
    }

    fun getLastClipboardState(): ClipBox? {
        return box.query()
            .isNull(ClipBox_.deleteDate)
            .orderDesc(ClipBox_.updateDate)
            .build()
            .findFirst()
    }

    fun getClipByText(text: String?): ClipBox? {
        if (text == null) return null
        val query = queryTextEq.getNotNull()
        query.setParameter(ClipBox_.text, text)
        val clips = query.find()
        return clips.find { it.tracked && !it.isDeleted() }
            ?: clips.find { !it.isDeleted() }
            ?: clips.firstOrNull()
    }

    fun getClipBySnippetId(id: String): ClipBox? {
        val query = querySnippetIdEq.getNotNull()
        query.setParameter(ClipBox_.snippetId, id)
        return query.findFirst()
    }

    fun getById(id: Long): ClipBox? = box.get(id)

    fun deleteAll(clips: List<Clip>): List<Clip> {
        if (clips.isEmpty()) return clips
        log("remove clips: {}", clips.size)
        val list = mutableListOf<ClipBox>()
        clips.forEach {
            val deletedClip = it.toBox()
            filterBoxDao.get().update(deletedClip, null)
            deletedClip.deleteDate = null
            list.add(deletedClip)
        }
        postClipsCountChanged()
        box.remove(list)
        return list
    }

    fun undoDeleteAll(clips: List<Clip>): List<Clip> {
        val list = mutableListOf<ClipBox>()
        clips.forEach {
            val clipBox = it.toBox(new = true)
            clipBox.deleteDate = null
            filterBoxDao.get().update(null, clipBox)
            list.add(clipBox)
        }
        saveAll(list)
        return list
    }

    fun getAllClipsCount() = box.count()

    fun getAllClips(): List<ClipBox> = queryAll.find()

    fun getSyncedClipsCount() = querySynced.count().toInt()

    fun getNotSyncedClipsCount() = queryNotSynced.count().toInt()

    fun getUntaggedClipsCount() = queryUntagged.count()

    fun getSnippetClipsCount() = querySnippets.count()

    fun getClipboardClipsCount() =
        if (filterBoxDao.get().getFilters().clipboard.excludeWithCustomAttributes) {
            queryClipboardWithExcludedCustomAttrs.count()
        } else {
            queryClipboard.count()
        }

    fun getRecycleBinClipsCount() = queryRecycleBin.count()

    fun getFavClipsCount() = queryFav.count()

    fun getTrackedHistory(count: Int): List<Clip> = queryClipboardHistory.find(0, count.toLong())

    fun getTrackedHistoryCount() = queryClipboardHistory.count()

    fun getNotSyncedClips(): List<ClipBox> = queryNotSynced.find()

    fun getFiltered(filter: Filter.Snapshot): Query<ClipBox> {
        val filters = filterBoxDao.get().getFilters()
        val query = box.query()

        // ===== SPECIFIC =====

        if (!filter.cleanupRequest && !filter.recycled && !filter.showOnlyWithAttachments && !filter.showOnlyNotSynced && !filter.showOnlyWithPublicLink) {
            query.isNull(ClipBox_.deleteDate)
        }

        // ===== WHERE FOLDER EQ =====
        if (filter.folderIds.isNotEmpty()) {
            var orLogic = false
            val folderIds = filter.folderIds.mapNotNull { it.toNullIfEmpty() }.toTypedArray()
            if (filter.folderIds.size != folderIds.size) {
                query.isNull(ClipBox_.folderId)
                orLogic = true
            }
            if (folderIds.isNotEmpty()) {
                if (orLogic) {
                    query.or()
                }
                if (folderIds.size == 1) {
                    query.equal(ClipBox_.folderId, folderIds.first())
                } else {
                    query.`in`(ClipBox_.folderId, folderIds)
                }
            }
        }

        // ===== TEXT LIKE =====

        filter.textLike?.let {
            val textLikes = it.split(",").mapNotNull { tokens -> tokens.toNullIfEmpty() }
            textLikes.forEachIndexed { index, textLike ->
                if (textLike.matches("\\w+".toRegex()) && Character.UnicodeBlock.of(textLike.first()) == Character.UnicodeBlock.CYRILLIC) {
                    Analytics.onSearchByCyrillic()
                    val combinations = mutableSetOf<String>()
                    combinations.add(textLike.uppercase())
                    combinations.add(textLike.lowercase())
                    combinations.add(textLike.capitalize())
                    combinations.add(textLike)
                    combinations.forEachIndexed { i, s ->
                        query.contains(ClipBox_.title, s, QueryBuilder.StringOrder.CASE_INSENSITIVE)
                            .or().contains(ClipBox_.text, s, QueryBuilder.StringOrder.CASE_INSENSITIVE)
                            .or().contains(ClipBox_.description, s, QueryBuilder.StringOrder.CASE_INSENSITIVE)
                            .or().equal(ClipBox_.abbreviation, s, QueryBuilder.StringOrder.CASE_INSENSITIVE)
                        if (i < combinations.size - 1) {
                            query.or()
                        }
                    }
                } else {
                    query.contains(ClipBox_.title, textLike, QueryBuilder.StringOrder.CASE_INSENSITIVE)
                        .or().contains(ClipBox_.text, textLike, QueryBuilder.StringOrder.CASE_INSENSITIVE)
                        .or().contains(ClipBox_.description, textLike, QueryBuilder.StringOrder.CASE_INSENSITIVE)
                        .or().equal(ClipBox_.abbreviation, textLike, QueryBuilder.StringOrder.CASE_INSENSITIVE)
                }
                if (index < textLikes.size - 1) {
                    query.or()
                }
            }
        }

        // ===== WHERE CLIP IDS =====

        if (filter.clipIds.isNotEmpty()) {
            when (filter.clipIdsWhereType) {
                Filter.WhereType.NONE_OF -> {
                    val ids = box.query().inValues(ClipBox_.firestoreId, filter.clipIds.toTypedArray()).build().findIds()
                    query.notIn(ClipBox_.localId, ids)
                }
                else -> {

                }
            }
        }

        var multipleConditions = false

        // ===== LOCATED IN =====

        if (filter.locatedInWhereType == Filter.WhereType.NONE_OF) {
            if (filter.untagged) {
                query.notNull(ClipBox_.tagIds)
            }
            if (filter.clipboard) {
                query.equal(ClipBox_.tracked, false)
            }
            if (filter.recycled) {
                query.isNull(ClipBox_.deleteDate)
            }
            if (filter.starred) {
                query.equal(ClipBox_.fav, false)
            }
            if (filter.snippets) {
                query.isNull(ClipBox_.snippetId)
            }
        } else {
            if (filter.untagged) {
                query.isNull(ClipBox_.tagIds)
                multipleConditions = true
            }
            if (filter.clipboard) {
                if (multipleConditions) {
                    query.or()
                }
                query.equal(ClipBox_.tracked, true)
                if (filters.clipboard.excludeWithCustomAttributes) {
                    query
                        .notEqual(ClipBox_.snippet, true)
                        .notEqual(ClipBox_.fav, true)
                        .isNull(ClipBox_.abbreviation)
                        .isNull(ClipBox_.description)
                        .isNull(ClipBox_.deleteDate)
                        .isNull(ClipBox_.snippetSetsIds)
                        .isNull(ClipBox_.fileIds)
                        .isNull(ClipBox_.tagIds)
                        .isNull(ClipBox_.title)
                }
                multipleConditions = true
            }
            if (filter.recycled) {
                if (multipleConditions) {
                    query.or()
                }
                query.notNull(ClipBox_.deleteDate)
                multipleConditions = true
            }
            if (filter.starred) {
                if (multipleConditions) {
                    query.or()
                }
                query.equal(ClipBox_.fav, true)
                multipleConditions = true
            }
            if (filter.snippets) {
                if (multipleConditions) {
                    query.or()
                }
                query.equal(ClipBox_.snippet, true)
                multipleConditions = true
            }
        }

        // ===== TAGS =====
        filter.tagIds
            .filter { filter.cleanupRequest || filters.findFilterByTagId(it) != null }
            .takeIf { it.isNotEmpty() }
            ?.let { tagIds ->
                if (multipleConditions) {
                    if (filter.cleanupRequest) {
                        query.or()
                    } else {
                        query.and()
                    }
                }
                val case = QueryBuilder.StringOrder.CASE_SENSITIVE
                tagIds.forEachIndexed { index, id ->
                    query.contains(ClipBox_.tagIds, id.inDashes(), case)
                    if (index < tagIds.size - 1) {
                        if (filter.tagIdsWhereType == Filter.WhereType.ANY_OF) {
                            query.or()
                        } else {
                            query.and()
                        }
                    }
                }
                multipleConditions = true
            }

        // ===== SNIPPET SETS =====
        filter.snippetSetIds
            .filter { filter.cleanupRequest || filters.findFilterBySnippetKitId(it) != null }
            .takeIf { it.isNotEmpty() }
            ?.let { snippetSetsIds ->
                if (multipleConditions) {
                    if (filter.cleanupRequest) {
                        query.or()
                    } else {
                        query.and()
                    }
                }
                val case = QueryBuilder.StringOrder.CASE_SENSITIVE
                snippetSetsIds.forEachIndexed { index, id ->
                    query.contains(ClipBox_.snippetSetsIds, id.inDashes(), case)
                    if (index < snippetSetsIds.size - 1) {
                        if (filter.snippetSetIdsWhereType == Filter.WhereType.ANY_OF) {
                            query.or()
                        } else {
                            query.and()
                        }
                    }
                }
                multipleConditions = true
            }

        // ===== SHOW PUBLIC LINKS =====
        if (filter.showOnlyWithPublicLink) {
            if (multipleConditions) {
                query.and()
            }
            query.notNull(ClipBox_.publicLink)
            multipleConditions = true
        }

        // ===== SHOW ONLY NOT SYNCED =====
        if (filter.showOnlyNotSynced) {
            if (multipleConditions) {
                query.and()
            }
            query.isNull(ClipBox_.firestoreId)
            multipleConditions = true
        }

        // ===== SHOW ONLY WITH ATTACHMENTS =====
        if (filter.showOnlyWithAttachments) {
            if (multipleConditions) {
                query.and()
            }
            query.greater(ClipBox_.filesCount, 0)
            multipleConditions = true
        }

        // ===== SHOW ONLY WITH FILES =====
        if (filter.fileIds.isNotEmpty()) {
            when (filter.fileIdsWhereType) {
                Filter.WhereType.ANY_OF -> {
                    if (multipleConditions) {
                        query.and()
                    }
                    val case = QueryBuilder.StringOrder.CASE_SENSITIVE
                    filter.fileIds.forEachIndexed { index, id ->
                        query.contains(ClipBox_.fileIds, id.inDashes(), case)
                        if (index < filter.fileIds.size - 1) {
                            query.or()
                        }
                    }
                    multipleConditions = true
                }
                else -> Unit
            }
        }

        // ===== SHOW ONLY WITH TEXT TYPES =====
        if (filter.textTypeIn.isNotEmpty()) {
            if (multipleConditions) {
                query.and()
            }
            query.inValues(ClipBox_.textType, filter.textTypeIn.map { it.typeId }.toIntArray())
        }

        // ===== SHOW ONLY WITH CREATE DATE =====
        val createInterval = filter.createDatePeriod?.toInterval(filter.createDateFrom, filter.createDateTo)
        if (createInterval != null) {
            if (multipleConditions) {
                query.and()
            }
            when {
                createInterval.from != null && createInterval.to != null -> query.between(ClipBox_.createDate, createInterval.from, createInterval.to)
                createInterval.from != null -> query.greaterOrEqual(ClipBox_.createDate, createInterval.from)
                createInterval.to != null -> query.lessOrEqual(ClipBox_.createDate, createInterval.to)
                else -> Unit
            }
            multipleConditions = true
        }

        // ===== SHOW ONLY WITH UPDATE DATE =====
        val updateInterval = filter.updateDatePeriod?.toInterval(filter.updateDateFrom, filter.updateDateTo)
        if (updateInterval != null) {
            if (multipleConditions) {
                query.and()
            }
            when {
                updateInterval.from != null && updateInterval.to != null -> query.between(ClipBox_.modifyDate, updateInterval.from, updateInterval.to)
                updateInterval.from != null -> query.greaterOrEqual(ClipBox_.modifyDate, updateInterval.from)
                updateInterval.to != null -> query.lessOrEqual(ClipBox_.modifyDate, updateInterval.to)
                else -> Unit
            }
            multipleConditions = true
        }

        when (filter.sortBy) {
            SortBy.MODIFY_DATE_ASC -> withSort(query, filter)
                .order(ClipBox_.modifyDate)
            SortBy.MODIFY_DATE_DESC -> withSort(query, filter)
                .orderDesc(ClipBox_.modifyDate)

            SortBy.CREATE_DATE_ASC -> withSort(query, filter)
                .order(ClipBox_.createDate)
            SortBy.CREATE_DATE_DESC -> withSort(query, filter)
                .orderDesc(ClipBox_.createDate)

            SortBy.USAGE_DATE_ASC -> withSort(query, filter)
                .order(ClipBox_.updateDate)
                .orderDesc(ClipBox_.createDate)
            SortBy.USAGE_DATE_DESC -> withSort(query, filter)
                .orderDesc(ClipBox_.updateDate)
                .orderDesc(ClipBox_.createDate)

            SortBy.DELETE_DATE_ASC -> withSort(query, filter)
                .order(ClipBox_.deleteDate)
                .orderDesc(ClipBox_.createDate)
            SortBy.DELETE_DATE_DESC -> withSort(query, filter)
                .orderDesc(ClipBox_.deleteDate)
                .orderDesc(ClipBox_.createDate)

            SortBy.USAGE_COUNT_ASC -> withSort(query, filter)
                .order(ClipBox_.usageCount)
                .orderDesc(ClipBox_.createDate)
            SortBy.USAGE_COUNT_DESC -> withSort(query, filter)
                .orderDesc(ClipBox_.usageCount)
                .orderDesc(ClipBox_.createDate)

            SortBy.TITLE_ASC -> withSort(query, filter).order(ClipBox_.title, QueryBuilder.NULLS_LAST or QueryBuilder.CASE_SENSITIVE)
                .orderDesc(ClipBox_.createDate)
            SortBy.TITLE_DESC -> withSort(query, filter)
                .order(ClipBox_.title, QueryBuilder.DESCENDING or QueryBuilder.NULLS_LAST or QueryBuilder.CASE_SENSITIVE)
                .orderDesc(ClipBox_.createDate)

            SortBy.NAME_ASC -> withSort(query, filter).order(ClipBox_.title, QueryBuilder.NULLS_LAST or QueryBuilder.CASE_SENSITIVE)
                .orderDesc(ClipBox_.createDate)
            SortBy.NAME_DESC -> withSort(query, filter)
                .order(ClipBox_.title, QueryBuilder.DESCENDING or QueryBuilder.NULLS_LAST or QueryBuilder.CASE_SENSITIVE)
                .orderDesc(ClipBox_.createDate)

            SortBy.TEXT_ASC -> withSort(query, filter)
                .order(ClipBox_.text, QueryBuilder.NULLS_LAST or QueryBuilder.CASE_SENSITIVE)
                .orderDesc(ClipBox_.createDate)
            SortBy.TEXT_DESC -> withSort(query, filter)
                .order(ClipBox_.text, QueryBuilder.DESCENDING or QueryBuilder.NULLS_LAST or QueryBuilder.CASE_SENSITIVE)
                .orderDesc(ClipBox_.createDate)

            SortBy.TAGS_ASC -> withSort(query, filter)
                .order(ClipBox_.tagIds, QueryBuilder.NULLS_LAST or QueryBuilder.CASE_SENSITIVE)
                .orderDesc(ClipBox_.createDate)
            SortBy.TAGS_DESC -> withSort(query, filter)
                .order(ClipBox_.tagIds, QueryBuilder.DESCENDING or QueryBuilder.NULLS_LAST or QueryBuilder.CASE_SENSITIVE)
                .orderDesc(ClipBox_.createDate)

            SortBy.SIZE_ASC -> withSort(query, filter)
                .order(ClipBox_.size)
                .orderDesc(ClipBox_.createDate)
            SortBy.SIZE_DESC -> withSort(query, filter)
                .orderDesc(ClipBox_.size)
                .orderDesc(ClipBox_.createDate)

            SortBy.CHARACTERS_ASC -> withSort(query, filter)
                .order(ClipBox_.characters)
                .orderDesc(ClipBox_.createDate)
            SortBy.CHARACTERS_DESC -> withSort(query, filter)
                .orderDesc(ClipBox_.characters)
                .orderDesc(ClipBox_.createDate)

            else -> Unit
        }

        return query.build()
    }

    private fun withSort(qb: QueryBuilder<ClipBox>, filter: Filter.Snapshot): QueryBuilder<ClipBox> {
        if (filter.pinSnippets) {
            qb.orderDesc(ClipBox_.snippet)
        }
        if (filter.pinStarred) {
            qb.orderDesc(ClipBox_.fav)
        }
        return qb
    }

    fun save(clip: ClipBox, copied: Boolean = false) {
        val isNew = clip.isNew()
        shake(clip)
        box.put(clip)
        clip.sourceClips?.let { deleteAll(it) }
        clip.sourceClips = null
        if (!copied || isNew) {
            postClipsCountChanged()
        }
    }

    private fun shake(clip: Clip, timestamp: Long = System.currentTimeMillis()) {
        val fileIds = clip.fileIds
        val size = fileBoxDao.get().getFiles(fileIds)
            .sumOf { it.size }
            .plus(clip.text.length())
            .plus(clip.title.length())

        clip.size = size
        clip.filesCount = fileIds.size
        clip.title = clip.title.ifNotEmpty()
        clip.characters = clip.text.length()
        clip.objectType = clip.objectType.getValue()
        clip.firestoreId = clip.firestoreId.ifNotEmpty()
        clip.updateDate = clip.updateDate ?: clip.createDate
        clip.publicLink = clip.publicLink?.takeIf { clip.hasPublicLink() }
        clip.dynamic = DynamicField.isDynamic(clip.text)
        clip.snippet = clip.isSnippet()
        clip.changeTimestamp = timestamp
    }

    fun defineClipType(text: String?, fileIds: List<String> = emptyList()): TextType =
        when {
            text.isNullOrBlank() || fileIds.isNotEmpty() -> TextType.TEXT_PLAIN
            TextTypeExt.HTML.isValid(text) -> TextType.HTML
            TextTypeExt.LINK.isValid(text) -> TextType.LINK
            else -> TextType.TEXT_PLAIN
        }

    fun applyAutoTags(clip: Clip) {
        val filters = filterBoxDao.get().getFilters()
        if (!appConfig.canCreateTagAutoRules() || !clip.canApplyAutoTags()) return
        val currentTags = clip.getTagIds()
        val autoTags = filters.getSortedTags()
            .filter { it.autoRulesEnabled }
            .filter { !currentTags.contains(it.uid) }
            .filter { !clip.excludedTagIds.contains(it.uid) }
            .mapNotNull { filter ->
                val commas = filter.autoRuleByTextIn?.split(",")?.mapNotNull { it.toNullIfEmpty() }
                if (commas.isNullOrEmpty()) {
                    null
                } else {
                    clip.text?.findAnyOf(commas, 0, true)?.let {
                        Analytics.featureAutoTagByComma(it.second)
                        filter.uid
                    }
                }
            }
        if (autoTags.isNotEmpty()) {
            clip.tagIds = currentTags.plus(autoTags).distinct()
        }
    }

    fun createOrUpdate(clip: Clip, copied: Boolean): ClipBox {
        val transactionDate = Date()
        if (clip.snippet && clip.snippetId.isNullOrBlank()) {
            clip.snippetId = clip.firestoreId ?: IdUtils.autoId()
        }
        val newClip = clip.toBox(new = true)
        if (newClip.localId != 0L) {
            val prevClip = getById(newClip.localId) ?: newClip
            val textChanged = newClip.text != prevClip.text
            if (!copied && textChanged) applyAutoTags(newClip)
            filterBoxDao.get().update(prevClip, newClip)
            newClip.apply {
                if (copied) {
                    updateDate = transactionDate
                    usageCount += 1
                } else if (!newClip.areContentTheSame(prevClip)) {
                    modifyDate = transactionDate
                }
                if (prevClip.firestoreId != null) {
                    firestoreId = prevClip.firestoreId
                }
            }
            save(newClip, copied = copied)
            return newClip
        } else {
            if (!newClip.text.isNullOrBlank() && newClip.tracked) {
                val prevClip = getClipByText(newClip.text!!)
                if (prevClip != null) {
                    val same = newClip.areContentTheSame(prevClip)
                    val prevClipSnapshot = prevClip.toBox(true)
                    val textChanged = newClip.text != prevClip.text
                    prevClip.apply {
                        if (!newClip.tracked) {
                            val newTagIds = prevClip.getTagIds().toMutableSet()
                            newClip.getTagIds().let { newTagIds.addAll(it) }
                            fileIds = fileIds.plus(newClip.fileIds).distinct()
                            sourceClips = newClip.sourceClips
                            title = newClip.title ?: title
                            textType = newClip.textType
                            tagIds = newTagIds.toList()
                        }
                        if (copied) {
                            updateDate = transactionDate
                            usageCount += 1
                        } else if (!same) {
                            modifyDate = transactionDate
                        }
                        deleteDate = null
                    }
                    if (!copied && textChanged) applyAutoTags(prevClip)
                    filterBoxDao.get().update(prevClipSnapshot, prevClip)
                    save(prevClip)
                    return prevClip
                } else {
                    val date = newClip.createDate ?: transactionDate
                    val box = newClip.apply {
                        if (canDefineTextType()) {
                            textType = defineClipType(text, newClip.fileIds)
                        }
                        updateDate = date
                        modifyDate = date
                        createDate = date
                        usageCount = 0
                    }
                    applyAutoTags(box)
                    filterBoxDao.get().update(null, box)
                    save(box)
                    return cleanupClipboard(box)
                }
            } else {
                val date = newClip.createDate ?: transactionDate
                val box = newClip.apply {
                    if (canDefineTextType()) {
                        textType = defineClipType(text, newClip.fileIds)
                    }
                    updateDate = date
                    modifyDate = date
                    createDate = date
                    usageCount = 0
                }
                applyAutoTags(box)
                filterBoxDao.get().update(null, box)
                save(box)
                return cleanupClipboard(box)
            }
        }
    }

    private fun cleanupClipboard(clip: ClipBox): ClipBox {
        if (clip.tracked) {
            val deletedClips = getClipboardExceedingClips()
            if (deletedClips.isNotEmpty()) {
                clip.sourceClips = deletedClips
                deleteAll(deletedClips)
                postClipsCountChanged()
            }
        }
        return clip
    }

}