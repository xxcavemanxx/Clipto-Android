package clipto.repository

import clipto.AppContext
import clipto.action.CleanupFiltersAction
import clipto.analytics.Analytics
import clipto.api.IApi
import clipto.common.extensions.toNullIfEmpty
import clipto.dao.TxHelper
import clipto.dao.drive.DriveServiceHelper
import clipto.dao.objectbox.ClipBoxDao
import clipto.dao.objectbox.FileBoxDao
import clipto.dao.objectbox.FilterBoxDao
import clipto.dao.objectbox.model.ClipBox
import clipto.dao.objectbox.model.FilterBox
import clipto.dao.objectbox.model.toBox
import clipto.domain.Clip
import clipto.domain.FileRef
import clipto.domain.Filter
import clipto.domain.factory.FileRefFactory
import clipto.domain.getTagIds
import clipto.extensions.log
import clipto.presentation.clip.details.ClipDetailsState
import clipto.store.clip.ClipState
import clipto.store.clipboard.ClipboardState
import clipto.store.clipboard.data.toStackItem
import clipto.store.clipboard.toClipData
import clipto.store.main.MainState
import clipto.store.user.UserState
import clipto.utils.DomainUtils
import com.google.gson.Gson
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Single
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipRepository @Inject constructor(
    private val api: Lazy<IApi>,
    private val clipState: ClipState,
    private val txHelper: TxHelper,
    private val userState: UserState,
    private val mainState: MainState,
    private val clipBoxDao: ClipBoxDao,
    private val fileBoxDao: FileBoxDao,
    private val filterBoxDao: FilterBoxDao,
    private val clipboardState: ClipboardState,
    private val clipDetailsState: ClipDetailsState,
    private val driveHelper: DriveServiceHelper,
    private val gson: Gson,
    private val cleanupFiltersAction: Lazy<CleanupFiltersAction>
) : IClipRepository {

    companion object {
        private const val METADATA_FILE = "metadata.json"
    }

    override fun terminate(): Completable = Completable.complete()

    override fun init(): Completable = Completable.fromCallable {
        syncAll()
    }

    override fun getRelativePath(folderId: String?, clip: Clip): Single<String> = Single
        .fromCallable {
            val path = fileBoxDao.getPath(clip.folderId)
            val folderIdRef = folderId.toNullIfEmpty()
            val indexOf = path.indexOfFirst { it.getUid() == folderIdRef }.takeIf { it >= 0 }?.let { it + 1 } ?: 0
            val relativePath = path.subList(indexOf, path.size)
            relativePath.mapNotNull { it.title }.joinToString(
                prefix = FileRefFactory.ROOT_PATH,
                separator = FileRefFactory.PATH_SEPARATOR
            )
        }

    override fun getById(id: Long): Single<Clip> = Single
        .fromCallable { clipBoxDao.getById(id) }

    override fun getByText(text: String?, id: Long): Single<Clip> = Single
        .fromCallable {
            var clip: Clip? = null
            if (id != 0L) {
                clip = clipBoxDao.getById(id)
            }
            if (clip == null && text != null) {
                clip = clipBoxDao.getClipByText(text)
            }
            clip
        }

    override fun restoreLastCopiedClip(): Single<Clip> = Single
        .fromCallable {
            val clipData = clipboardState.getPrimaryClip()
            if (clipData == null) {
                val clip = clipBoxDao.getLastClipboardState()
                if (clip != null) {
                    clipboardState.refreshClipboard(internal = true, clip.toClipData(""))
                }
                clip
            } else {
                null
            }
        }

    override fun favAll(clips: List<Clip>, fav: Boolean): Single<List<Clip>> = Single
        .fromCallable {
            val favClips = mutableListOf<ClipBox>()

            txHelper.inTx("fav clips") {
                clips.forEach {
                    val newClip = it.toBox()
                    val prevClip = clipBoxDao.getById(newClip.localId) ?: newClip
                    if (prevClip.fav != fav) {
                        prevClip.fav = fav
                        favClips.add(prevClip)
                    }
                }
                clipBoxDao.saveAll(favClips, modified = true)
            }

            syncAll()
            favClips
        }

    override fun syncAll(newClips: List<Clip>, callback: (clips: List<Clip>) -> Unit) {
        if (userState.isSyncEnabled() && userState.isAuthorized()) {
            userState.onBackground {
                try {
                    val driveFiles = driveHelper.listFiles()
                    
                    // 1. Download or initialize metadata.json
                    val remoteMetaId = driveFiles.find { it.name == METADATA_FILE }?.id
                    val remoteMetadata: SyncMetadata = if (remoteMetaId != null) {
                        val content = driveHelper.downloadFile(remoteMetaId)
                        gson.fromJson(content, SyncMetadata::class.java)
                    } else {
                        SyncMetadata()
                    }

                    // 2. Query ObjectBox
                    val localClips = clipBoxDao.getAllClips()
                    val localFilters = filterBoxDao.getFilters().getSortedNamedFilters()

                    val updatedRemoteItems = mutableMapOf<String, Long>()
                    updatedRemoteItems.putAll(remoteMetadata.items)

                    // 3. Sync clips
                    txHelper.inTx("DriveSync-Clips") {
                        localClips.forEach { localClip ->
                            if (localClip.firestoreId == null && localClip.snippetId == null) {
                                localClip.snippetId = clipto.common.misc.IdUtils.autoId()
                                clipBoxDao.save(localClip)
                            }
                            val uid = localClip.firestoreId ?: localClip.snippetId ?: return@forEach
                            val localTime = localClip.modifyDate?.time ?: localClip.createDate?.time ?: 0L
                            val remoteTime = remoteMetadata.items[uid] ?: 0L

                            if (localClip.isDeleted()) {
                                if (remoteMetadata.items.containsKey(uid)) {
                                    driveHelper.deleteFileByName("clip_$uid.json")
                                    updatedRemoteItems.remove(uid)
                                    remoteMetadata.tombstones.add(uid)
                                }
                            } else if (remoteMetadata.tombstones.contains(uid)) {
                                clipBoxDao.deleteAll(listOf(localClip))
                            } else if (localTime > remoteTime) {
                                val clipJson = gson.toJson(localClip, Clip::class.java)
                                driveHelper.uploadFile("clip_$uid.json", clipJson, "application/json")
                                updatedRemoteItems[uid] = localTime
                            } else if (remoteTime > localTime) {
                                val fileId = driveFiles.find { it.name == "clip_$uid.json" }?.id
                                if (fileId != null) {
                                    val remoteJson = driveHelper.downloadFile(fileId)
                                    val remoteClip = gson.fromJson(remoteJson, ClipBox::class.java)
                                    localClip.apply(remoteClip)
                                    clipBoxDao.save(localClip)
                                }
                            }
                        }

                        // Add new remote clips
                        remoteMetadata.items.forEach { (uid, _) ->
                            if (uid.startsWith("clip") || driveFiles.any { it.name == "clip_$uid.json" }) {
                                val local = localClips.find { it.firestoreId == uid || it.snippetId == uid }
                                if (local == null && !remoteMetadata.tombstones.contains(uid)) {
                                    val fileId = driveFiles.find { it.name == "clip_$uid.json" }?.id
                                    if (fileId != null) {
                                        val remoteJson = driveHelper.downloadFile(fileId)
                                        val remoteClip = gson.fromJson(remoteJson, ClipBox::class.java)
                                        clipBoxDao.save(remoteClip)
                                    }
                                }
                            }
                        }
                    }

                    // 4. Sync filters
                    txHelper.inTx("DriveSync-Filters") {
                        localFilters.forEach { localFilter ->
                            val uid = localFilter.uid ?: return@forEach
                            val localTime = localFilter.updateDate?.time ?: localFilter.createDate?.time ?: 0L
                            val remoteTime = remoteMetadata.items[uid] ?: 0L

                            if (remoteMetadata.tombstones.contains(uid)) {
                                filterBoxDao.remove(localFilter.toBox())
                            } else if (localTime > remoteTime) {
                                val filterJson = gson.toJson(localFilter, Filter::class.java)
                                driveHelper.uploadFile("filter_$uid.json", filterJson, "application/json")
                                updatedRemoteItems[uid] = localTime
                            } else if (remoteTime > localTime) {
                                val fileId = driveFiles.find { it.name == "filter_$uid.json" }?.id
                                if (fileId != null) {
                                    val remoteJson = driveHelper.downloadFile(fileId)
                                    val remoteFilter = gson.fromJson(remoteJson, FilterBox::class.java)
                                    localFilter.apply(remoteFilter)
                                    filterBoxDao.save(localFilter.toBox())
                                }
                            }
                        }

                        // Add new remote filters
                        remoteMetadata.items.forEach { (uid, _) ->
                            if (uid.startsWith("filter") || driveFiles.any { it.name == "filter_$uid.json" }) {
                                val local = localFilters.find { it.uid == uid }
                                if (local == null && !remoteMetadata.tombstones.contains(uid)) {
                                    val fileId = driveFiles.find { it.name == "filter_$uid.json" }?.id
                                    if (fileId != null) {
                                        val remoteJson = driveHelper.downloadFile(fileId)
                                        val remoteFilter = gson.fromJson(remoteJson, FilterBox::class.java)
                                        filterBoxDao.save(remoteFilter)
                                    }
                                }
                            }
                        }
                    }

                    // 5. Upload metadata
                    remoteMetadata.items = updatedRemoteItems
                    val updatedMetaJson = gson.toJson(remoteMetadata)
                    driveHelper.uploadFile(METADATA_FILE, updatedMetaJson, "application/json")

                    callback.invoke(newClips)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            callback.invoke(newClips)
        }
    }

    override fun tagAll(clips: List<Clip>, assignTagIds: List<String>): Single<List<Clip>> = Single
        .fromCallable {
            val changedClips = mutableListOf<ClipBox>()
            val commonTagIds = DomainUtils.getCommonTagIds(clips)
            val removedTagIds = commonTagIds.minus(assignTagIds)
            val addedTagIds = assignTagIds.minus(commonTagIds)
            if (removedTagIds.isNotEmpty() || addedTagIds.isNotEmpty()) {
                txHelper.inTx("assign tags to clips") {
                    clips.map { it.toBox() }.forEach { clip ->
                        val clipTagIds = clip.getTagIds()
                        val newClipTagIds = clipTagIds
                            .minus(removedTagIds)
                            .plus(addedTagIds)
                            .distinct()
                        if (newClipTagIds != clipTagIds) {
                            val newClip = ClipBox().apply {
                                apply(clip)
                                tagIds = newClipTagIds
                            }
                            filterBoxDao.update(clip, newClip)
                            changedClips.add(newClip)
                        }
                    }
                    clipBoxDao.saveAll(changedClips, modified = true)
                }
            }

            syncAll()
            changedClips
        }

    override fun deleteAll(clips: List<Clip>, permanently: Boolean, withUndo: Boolean, clearClipboard: Boolean): Single<List<Clip>> = Single
        .fromCallable {
            txHelper.inTx {
                val deletedClips: List<Clip>
                if (permanently || clips.find { it.deleteDate == null } == null) {
                    clipBoxDao.deleteAll(clips)
                    deletedClips = clips
                } else {
                    val recycled = mutableListOf<ClipBox>()
                    val transactionDate = Date()
                    val timestamp = transactionDate.time
                    clips.forEach {
                        val recycledClip = it.toBox(new = true)
                        filterBoxDao.update(recycledClip, null)
                        recycledClip.changeTimestamp = timestamp
                        recycledClip.deleteDate = transactionDate
                        recycled.add(recycledClip)
                    }
                    clipBoxDao.postClipsCountChanged()
                    clipBoxDao.saveAll(recycled)
                    val deleted = clipBoxDao.deleteAll(clipBoxDao.getRecycleBinExceedingClips())
                    deletedClips = recycled.plus(deleted.minus(recycled))
                }

                deletedClips
            }
        }
        .doOnSuccess {
            clipboardState.historyStack.updateValue { stack ->
                stack?.minus(clips.map { clip -> clip.toStackItem() })
            }
            if (clearClipboard && it.contains(clipboardState.clip.getValue())) {
                clipboardState.clearClipboard()
            }
            if (withUndo && clips.size == 1) {
                mainState.undoDeleteClips.setValue(setOf(it.first()))
            }
            mainState.clearSelection()
            syncAll()
        }

    override fun undoDeleteAll(clips: List<Clip>): Single<List<Clip>> = Single
        .fromCallable {
            txHelper.inTx {
                val undeletedClips = clipBoxDao.undoDeleteAll(clips)
                undeletedClips
            }
        }
        .doOnSuccess { syncAll() }

    override fun deleteAllFromFilters(filters: List<Filter>, clips: List<Clip>?): Single<List<Clip>> = Single
        .fromCallable {
            val tagIds = filters.filter { it.isTag() }.mapNotNull { it.uid }
            val kitIds = filters.filter { it.isSnippetKit() }.mapNotNull { it.uid }
            if (tagIds.isNotEmpty() || kitIds.isNotEmpty()) {
                val deletedClips = (
                        clips?.map { it.toBox() } ?: clipBoxDao
                            .getFiltered(
                                Filter.Snapshot(
                                    tagIds = tagIds,
                                    snippetSetIds = kitIds,
                                    cleanupRequest = true
                                )
                             )
                            .find())
                    .filter { clip ->
                        val oldTags = clip.tagIds
                        val newTags = oldTags.minus(tagIds)
                        val oldKits = clip.snippetSetsIds
                        val newKits = oldKits.minus(kitIds)
                        if (oldTags != newTags || oldKits != newKits) {
                            clip.snippetSetsIds = newKits
                            clip.tagIds = newTags
                            true
                        } else {
                            false
                        }
                    }
                txHelper.inTx {
                    clipBoxDao.saveAll(deletedClips)
                }
                syncAll()
                deletedClips
            } else {
                emptyList()
            }
        }

    override fun save(clip: Clip, copied: Boolean): Single<Clip> = Single
        .fromCallable {
            txHelper.inTx<Clip> {
                val clipBox = clipBoxDao.createOrUpdate(clip, copied)
                clipBox
            }
        }
        .doOnSuccess { 
            if (copied) clipboardState.clip.setValue(it) 
            syncAll()
        }

    override fun createLink(clip: Clip): Single<Clip> = Single.just(clip)

    override fun removeLink(clip: Clip): Single<Clip> = Single.just(clip)

    override fun clearClipboard(): Single<List<Clip>> = Single
        .fromCallable { clipBoxDao.getClipboardClips() }
        .flatMap { deleteAll(it, permanently = true) }

    override fun clearRecycleBin(): Single<List<Clip>> = Single
        .fromCallable { clipBoxDao.getRecycleBinClips() }
        .flatMap { deleteAll(it, permanently = true) }

    override fun getChildren(folderIds: List<String>): Single<List<Clip>> = Single
        .fromCallable {
            val all = mutableListOf<Clip>()
            folderIds.forEach { folderId ->
                all.addAll(clipBoxDao.getChildren(folderId))
            }
            all
        }

    override fun changeFolder(clips: List<Clip>, folderId: String?): Single<List<Clip>> = Single
        .fromCallable {
            val changed = mutableListOf<ClipBox>()

            txHelper.inTx("change folder") {
                clips.forEach {
                    val newClip = it.toBox()
                    val prevClip = clipBoxDao.getById(newClip.localId) ?: newClip
                    if (prevClip.folderId != folderId) {
                        prevClip.folderId = folderId
                        changed.add(prevClip)
                    }
                }
                clipBoxDao.saveAll(changed, modified = true)
            }

            syncAll()
            changed
        }

    override fun getByFile(fileRef: FileRef): Single<List<Clip>> = Single
        .fromCallable {
            val fileIds = listOf(fileRef).mapNotNull { it.getUid() }
            if (fileIds.isEmpty()) return@fromCallable emptyList()
            val request = Filter.Snapshot(
                fileIds = fileIds,
                cleanupRequest = true,
                fileIdsWhereType = Filter.WhereType.ANY_OF
            )
            clipBoxDao.getFiltered(request).find()
        }

    override fun unlink(files: List<FileRef>): Single<List<Clip>> = Single
        .fromCallable {
            val fileIds = files.mapNotNull { it.getUid() }
            if (fileIds.isEmpty()) return@fromCallable emptyList()

            val request = Filter.Snapshot(
                fileIds = fileIds,
                cleanupRequest = true,
                fileIdsWhereType = Filter.WhereType.ANY_OF
            )
            val clips = clipBoxDao.getFiltered(request).find()
            txHelper.inTx("unlink files") {
                val modifyDate = Date()
                clips.forEach { clip ->
                    clip.modifyDate = modifyDate
                    clip.fileIds = clip.fileIds.minus(fileIds)
                    clipBoxDao.save(clip)
                }
            }
            syncAll()
            emptyList()
        }

    private data class SyncMetadata(
        var items: Map<String, Long> = emptyMap(),
        val tombstones: MutableSet<String> = mutableSetOf()
    )
}