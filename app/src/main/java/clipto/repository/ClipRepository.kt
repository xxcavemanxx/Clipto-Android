package clipto.repository

import clipto.AppContext
import clipto.action.CleanupFiltersAction
import clipto.analytics.Analytics
import clipto.api.IApi
import clipto.common.extensions.toNullIfEmpty
import clipto.dao.TxHelper
import clipto.dao.firebase.ClipFirebaseDao
import clipto.dao.firebase.FirebaseDaoHelper
import clipto.dao.firebase.mapper.ClipMapper
import clipto.dao.firebase.mapper.DateMapper
import clipto.dao.firebase.mapper.PublicNoteLinkMapper
import clipto.dao.objectbox.ClipBoxDao
import clipto.dao.objectbox.FileBoxDao
import clipto.dao.objectbox.FilterBoxDao
import clipto.dao.objectbox.model.ClipBox
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
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
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
    private val clipMapper: ClipMapper,
    private val clipBoxDao: ClipBoxDao,
    private val fileBoxDao: FileBoxDao,
    private val filterBoxDao: FilterBoxDao,
    private val clipboardState: ClipboardState,
    private val clipFirebaseDao: ClipFirebaseDao,
    private val clipDetailsState: ClipDetailsState,
    private val firebaseDaoHelper: FirebaseDaoHelper,
    private val cleanupFiltersAction: Lazy<CleanupFiltersAction>
) : IClipRepository {

    override fun terminate(): Completable = Completable.fromCallable { clipFirebaseDao.stopSync() }

    override fun init(): Completable = terminate()
        .andThen(Completable.fromPublisher<Boolean> { publisher ->
            log("FIREBASE LISTENER :: init clips")
            clipFirebaseDao.startSync(
                activeInitialCallback = { changes, _ -> initialClips(changes) },
                activeSnapshotCallback = { changes, _ -> snapshotClips(changes) },
                deletedInitialCallback = { changes, _ -> deletedClips(changes) },
                deletedSnapshotCallback = { changes, _ -> deletedClips(changes) },
                firstCallback = {
                    publisher.onNext(true)
                    publisher.onComplete()
                }
            )
        })

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

                firebaseDaoHelper.getAuthUserCollection()?.let { collection ->
                    firebaseDaoHelper.splitBatches(favClips.filter { it.isSynced() }) { batchClips ->
                        val batch = collection.createBatch()
                        batchClips.forEach { favClip ->
                            val changes = mutableMapOf(
                                FirebaseDaoHelper.ATTR_CLIP_FAV to favClip.fav,
                                FirebaseDaoHelper.ATTR_CLIP_MODIFY_DATE to DateMapper.toTimestamp(favClip.modifyDate)
                            )
                            clipFirebaseDao.saveInBatch(favClip.toBox(), batch, collection, changes)
                        }
                        batch.commit()
                    }
                }

            }

            favClips
        }

    override fun syncAll(newClips: List<Clip>, callback: (clips: List<Clip>) -> Unit) {
        if (userState.canSyncNewNotes()) {
            firebaseDaoHelper.getAuthUserCollection()?.let { collection ->
                updateCanSyncState()
                userState.onBackground {
                    Analytics.onSyncEnabled()
                    val notSyncedClips = newClips.takeIf { it.isNotEmpty() }?.map { ClipBox().apply(it) } ?: clipBoxDao.getNotSyncedClips()
                    if (notSyncedClips.isNotEmpty()) {
                        firebaseDaoHelper.splitBatches(notSyncedClips) { clips ->
                            if (userState.canSyncNewNotes()) {
                                runCatching {
                                    log("not synced notes: {}", clips.size)
                                    txHelper.inTx("Update synced clips") {
                                        val savedClips = mutableListOf<ClipBox>()
                                        val batch = collection.createBatch()
                                        clips.forEach { clip ->
                                            if (clipFirebaseDao.saveInBatch(clip, batch, collection)) {
                                                savedClips.add(clip)
                                            }
                                        }
                                        if (savedClips.isNotEmpty()) {
                                            clipBoxDao.saveAll(savedClips)
                                        }
                                        batch.commit()
                                    }
                                    AppContext.get().onCheckSession()
                                }
                            }
                        }
                    }
                    callback.invoke(notSyncedClips)
                }
            }
        } else {
            Analytics.onSyncDisabled()
            updateCanSyncState()
        }
    }

    override fun tagAll(clips: List<Clip>, assignTagIds: List<String>): Single<List<Clip>> = Single
        .fromCallable {
            val changedClips = mutableListOf<ClipBox>()
            val commonTagIds = DomainUtils.getCommonTagIds(clips)
            val removedTagIds = commonTagIds.minus(assignTagIds)
            val addedTagIds = assignTagIds.minus(commonTagIds)
            log(
                "assignTags: \ncommon: {} \nremoved: {} \nadded: {}",
                commonTagIds,
                removedTagIds,
                addedTagIds
            )
            if (removedTagIds.isNotEmpty() || addedTagIds.isNotEmpty()) {
                txHelper.inTx("assign tags to clips") {
                    clips.map { it.toBox() }.forEach { clip ->
                        val clipTagIds = clip.getTagIds()
                        val newClipTagIds = clipTagIds
                            .minus(removedTagIds)
                            .plus(addedTagIds)
                            .distinct()
                        if (newClipTagIds != clipTagIds) {
                            log("assignTag to clip: {} -> {}", clip.tagIds, newClipTagIds)
                            val newClip = ClipBox().apply {
                                apply(clip)
                                tagIds = newClipTagIds
                            }
                            filterBoxDao.update(clip, newClip)
                            changedClips.add(newClip)
                        }
                    }
                    clipBoxDao.saveAll(changedClips, modified = true)

                    firebaseDaoHelper.getAuthUserCollection()?.let { collection ->
                        firebaseDaoHelper.splitBatches(changedClips.filter { it.isSynced() }) { changed ->
                            val batch = collection.createBatch()
                            changed.forEach { clip ->
                                val changes = mutableMapOf(
                                    FirebaseDaoHelper.ATTR_CLIP_TAG_IDS to clip.tagIds,
                                    FirebaseDaoHelper.ATTR_CLIP_UPDATE_DATE to FieldValue.serverTimestamp(),
                                    FirebaseDaoHelper.ATTR_CLIP_MODIFY_DATE to DateMapper.toTimestamp(clip.modifyDate)
                                )
                                clipFirebaseDao.saveInBatch(clip, batch, collection, changes)
                            }
                            batch.commit()
                        }
                    }
                }
            }

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

                firebaseDaoHelper.getAuthUserCollection()?.let { collection ->
                    firebaseDaoHelper.splitBatches(deletedClips.filter { it.isSynced() }) { deleted ->
                        val batch = collection.createBatch()
                        clipFirebaseDao.deleteAllInBatch(deleted, batch, collection)
                        batch.commit()
                    }
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
        }

    override fun undoDeleteAll(clips: List<Clip>): Single<List<Clip>> = Single
        .fromCallable {
            txHelper.inTx {
                val undeletedClips = clipBoxDao.undoDeleteAll(clips)

                firebaseDaoHelper.getAuthUserCollection()?.let { collection ->
                    firebaseDaoHelper.splitBatches(undeletedClips.filter { it.isSynced() }) { undeleted ->
                        val batch = collection.createBatch()
                        undeleted.forEach { clipFirebaseDao.saveInBatch(it.toBox(), batch, collection) }
                        batch.commit()
                    }
                }

                undeletedClips
            }
        }

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
                firebaseDaoHelper.getAuthUserCollection()?.let { collection ->
                    firebaseDaoHelper.splitBatches(deletedClips) { batchClips ->
                        txHelper.inTx {
                            val batch = collection.createBatch()
                            batchClips.forEach { clip ->
                                val changes = mutableMapOf<String, Any?>()
                                changes[FirebaseDaoHelper.ATTR_CLIP_TAG_IDS] = clip.tagIds
                                changes[FirebaseDaoHelper.ATTR_CLIP_SNIPPET_SETS_IDS] = clip.snippetSetsIds
                                changes[FirebaseDaoHelper.ATTR_CLIP_UPDATE_DATE] = FirebaseDaoHelper.getServerTimestamp()
                                clipFirebaseDao.saveInBatch(clip, batch, collection, changes)
                            }
                            clipBoxDao.saveAll(batchClips)
                            batch.commit()
                        }
                    }
                }
                deletedClips
            } else {
                emptyList()
            }
        }

    override fun save(clip: Clip, copied: Boolean): Single<Clip> = Single
        .fromCallable {
            txHelper.inTx<Clip> {
                val prevClip = clip.toBox().let {
                    if (it.localId != 0L) {
                        clipBoxDao.getById(it.localId)
                    } else if (!clip.text.isNullOrBlank() && clip.tracked) {
                        clipBoxDao.getClipByText(clip.text)
                    } else {
                        null
                    }
                }
                var sourceClips = clip.sourceClips ?: emptyList()
                val prevFileIds = prevClip?.fileIds
                val prevPublicLink = prevClip?.publicLink
                val objectType = prevClip?.objectType
                val usageCount = prevClip?.usageCount
                val modifyDate = prevClip?.modifyDate
                val deleteDate = prevClip?.deleteDate
                val snippetId = prevClip?.snippetId
                val folderId = prevClip?.folderId
                val type = prevClip?.textType
                val tagIds = prevClip?.tagIds
                val description = prevClip?.description
                val abbreviation = prevClip?.abbreviation
                val snippetSetsIds = prevClip?.snippetSetsIds
                val title = prevClip?.title
                val text = prevClip?.text
                val fav = prevClip?.fav

                val clipBox = clipBoxDao.createOrUpdate(clip, copied)
                sourceClips = clipBox.sourceClips ?: sourceClips

                firebaseDaoHelper.getAuthUserCollection()?.let { collection ->
                    val firestoreId = clipBox.firestoreId
                    if (firestoreId == null) {
                        val canSyncNotes = userState.canSyncNote(clipBox)
                        log("create new clip? {}", canSyncNotes)
                        if (canSyncNotes || sourceClips.isNotEmpty()) {
                            if (canSyncNotes && clipFirebaseDao.save(clipBox, collection)) {
                                clipBoxDao.save(clipBox)
                            }
                            firebaseDaoHelper.splitBatches(sourceClips) {
                                val batch = collection.createBatch()
                                clipFirebaseDao.deleteAllInBatch(it, batch, collection)
                                batch.commit()
                            }
                        }
                    } else if (prevClip != null) {
                        val changes = mutableMapOf<String, Any?>()
                        if (title != clipBox.title) {
                            changes[FirebaseDaoHelper.ATTR_CLIP_TITLE] = clipBox.title
                        }
                        if (snippetId != clipBox.snippetId) {
                            changes[FirebaseDaoHelper.ATTR_CLIP_SNIPPET_ID] = clipBox.snippetId
                        }
                        if (text != clipBox.text) {
                            changes[FirebaseDaoHelper.ATTR_CLIP_TEXT] = clipBox.text
                        }
                        if (usageCount != clipBox.usageCount) {
                            changes[FirebaseDaoHelper.ATTR_CLIP_UPDATE_DATE] = FieldValue.serverTimestamp()
                            changes[FirebaseDaoHelper.ATTR_CLIP_USAGE_COUNT] = clipBox.usageCount
                        }
                        if (type != clipBox.textType) {
                            changes[FirebaseDaoHelper.ATTR_CLIP_TEXT_TYPE] = clipBox.textType.typeId
                        }
                        if (objectType != clipBox.objectType) {
                            changes[FirebaseDaoHelper.ATTR_CLIP_OBJECT_TYPE] = clipBox.objectType.id
                        }
                        if (tagIds != clipBox.tagIds) {
                            changes[FirebaseDaoHelper.ATTR_CLIP_TAG_IDS] = clipBox.tagIds
                        }
                        if (snippetSetsIds != clipBox.snippetSetsIds) {
                            changes[FirebaseDaoHelper.ATTR_CLIP_SNIPPET_SETS_IDS] = clipBox.snippetSetsIds
                        }
                        if (abbreviation != clipBox.abbreviation) {
                            changes[FirebaseDaoHelper.ATTR_CLIP_ABBREVIATION] = clipBox.abbreviation
                        }
                        if (folderId != clipBox.folderId) {
                            changes[FirebaseDaoHelper.ATTR_CLIP_FOLDER_ID] = clipBox.folderId
                        }
                        if (description != clipBox.description) {
                            changes[FirebaseDaoHelper.ATTR_CLIP_DESCRIPTION] = clipBox.description
                        }
                        if (fav != clipBox.fav) {
                            changes[FirebaseDaoHelper.ATTR_CLIP_FAV] = clipBox.fav
                        }
                        if (modifyDate != clipBox.modifyDate) {
                            changes[FirebaseDaoHelper.ATTR_CLIP_MODIFY_DATE] = DateMapper.toTimestamp(clipBox.modifyDate)
                        }
                        if (deleteDate != clipBox.deleteDate) {
                            changes[FirebaseDaoHelper.ATTR_CLIP_DELETE_DATE] = DateMapper.toTimestamp(clipBox.deleteDate, true)
                        }
                        if (prevFileIds != clipBox.fileIds) {
                            changes[FirebaseDaoHelper.ATTR_CLIP_FILE_IDS] = clipBox.fileIds
                        }
                        if (prevPublicLink != clipBox.publicLink) {
                            changes[FirebaseDaoHelper.ATTR_CLIP_PUBLIC_LINK] = PublicNoteLinkMapper.toMap(clipBox.publicLink)
                        }
                        if (changes.isNotEmpty()) {
                            log("update clip with id: {}, {}", firestoreId, changes)
                            if (clipFirebaseDao.save(clipBox, collection, changes)) {
                                clipBoxDao.save(clipBox)
                            }
                            firebaseDaoHelper.splitBatches(sourceClips) { batchClips ->
                                val batch = collection.createBatch()
                                clipFirebaseDao.deleteAllInBatch(batchClips, batch, collection)
                                batch.commit()
                            }
                        }
                    }
                }

                clipBox
            }
        }
        .doOnSuccess { if (copied) clipboardState.clip.setValue(it) }

    override fun createLink(clip: Clip): Single<Clip> = api.get()
        .createNotePublicLink(clip)
        .flatMapSingle { save(it, copied = false) }

    override fun removeLink(clip: Clip): Single<Clip> = api.get()
        .removeNotePublicLink(clip)
        .flatMapSingle { save(it, copied = false) }

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

                firebaseDaoHelper.getAuthUserCollection()?.let { collection ->
                    firebaseDaoHelper.splitBatches(changed.filter { it.isSynced() }) { batchClips ->
                        val batch = collection.createBatch()
                        batchClips.forEach { clip ->
                            val changes = mutableMapOf(
                                FirebaseDaoHelper.ATTR_CLIP_FOLDER_ID to clip.folderId,
                                FirebaseDaoHelper.ATTR_CLIP_MODIFY_DATE to DateMapper.toTimestamp(clip.modifyDate)
                            )
                            clipFirebaseDao.saveInBatch(clip.toBox(), batch, collection, changes)
                        }
                        batch.commit()
                    }
                }

            }

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
            val fileIdsToRemove = FirebaseDaoHelper.getFieldValueArrayRemove(fileIds)
            log("unlink clips from files :: clips={}, files={}", clips.size, files.size)
            firebaseDaoHelper.getAuthUserCollection()?.let { collection ->
                firebaseDaoHelper.splitBatches(clips) { batchClips ->
                    txHelper.inTx("unlink files") {
                        val batch = collection.createBatch()
                        val modifyDate = Date()
                        batchClips.forEach { clip ->
                            clip.modifyDate = modifyDate
                            clip.fileIds = clip.fileIds.minus(fileIds)
                            clipBoxDao.save(clip)

                            val changes = mutableMapOf(
                                FirebaseDaoHelper.ATTR_CLIP_FILE_IDS to fileIdsToRemove,
                                FirebaseDaoHelper.ATTR_CLIP_MODIFY_DATE to DateMapper.toTimestamp(modifyDate)
                            )
                            clipFirebaseDao.saveInBatch(clip.toBox(), batch, collection, changes)
                        }
                        batch.commit()
                    }
                }
            }
            emptyList()
        }

    private fun initialClips(changes: List<DocumentChange>) {
        val added = changes
            .filter { it.type == DocumentChange.Type.ADDED }
            .filter { !it.document.metadata.hasPendingWrites() && !it.document.id.startsWith("b_") }
            .mapNotNull { clipMapper.fromDocChange(it) }
            .toList()
        if (added.isNotEmpty()) {
            txHelper.inTx("Add clips after sync") {
                log("add clips: {}", added.size)
                val allClips = clipBoxDao.getAllClips()
                val addedClips = mutableListOf<ClipBox>()
                added.forEach { newClip ->
                    val prevClip = allClips.find { it.text == newClip.text }
                    filterBoxDao.update(prevClip, newClip)
                    newClip.apply {
                        if (prevClip != null) {
                            localId = prevClip.localId
                        }
                    }
                    addedClips.add(newClip)
                }
                clipBoxDao.saveAll(addedClips)
            }
            userState.deletedTags.consumeValue()
                ?.takeIf { it.isNotEmpty() }
                ?.let { cleanupFiltersAction.get().execute(it) }
        }
        syncAll()
    }

    private fun snapshotClips(changes: List<DocumentChange>) {
        val changed = changes
            .filter { it.type != DocumentChange.Type.REMOVED }
            .filter { !it.document.metadata.hasPendingWrites() && !it.document.id.startsWith("b_") }
            .mapNotNull { clipMapper.fromDocChange(it) }
            .toList()

        if (changed.isNotEmpty()) {
            log("to be removed: {}", changed.size)
            val checkForUniversalCopy = changed.size == 1
            var copiedClip: ClipBox? = null
            txHelper.inTx("sync with server state") {
                val changedClips = mutableListOf<ClipBox>()
                changed.forEach { newClip ->
                    val prevClip = changedClips.find { it.firestoreId == newClip.firestoreId }
                        ?: clipBoxDao.getByFirestoreId(newClip.firestoreId)
                    if (prevClip == null || !Clip.areTheSame(prevClip, newClip)) {
                        if (prevClip != null && !prevClip.isDeleted() && newClip.isDeleted()) {
                            filterBoxDao.update(prevClip, null)
                        } else if (prevClip != null && prevClip.isDeleted() && !newClip.isDeleted()) {
                            filterBoxDao.update(null, newClip)
                        } else {
                            filterBoxDao.update(prevClip, newClip)
                        }
                        if (prevClip != null) {
                            newClip.localId = prevClip.localId
                        }
                        log("change or add clip: {}", newClip)
                        changedClips.add(newClip)
                        if (checkForUniversalCopy && clipboardState.isUniversalClipboardActivated() && copiedClip == null) {
                            if (prevClip != null) {
                                if (prevClip.usageCount < newClip.usageCount) {
                                    copiedClip = newClip
                                }
                            } else if (newClip.tracked) {
                                copiedClip = newClip
                            }
                        }
                    }
                }
                if (changedClips.isNotEmpty()) {
                    clipBoxDao.saveAll(changedClips)
                    clipState.screenState.getValue()?.value
                        ?.takeIf { clip -> clip.firestoreId != null }
                        ?.let { clip ->
                            changedClips.firstOrNull { changed -> changed == clip }?.let { changed ->
                                if (clip.changeTimestamp != changed.changeTimestamp) {
                                    clipDetailsState.openedClip.setValue(changed)
                                    clipState.updateState(changed)
                                }
                            }
                        }
                }
            }
            copiedClip?.let { AppContext.get().onUniversalCopy(it) }
        }
    }

    private fun deletedClips(changes: List<DocumentChange>) {
        val removed = changes
            .filter { it.type != DocumentChange.Type.REMOVED }
            .filter { !it.document.metadata.hasPendingWrites() }
            .mapNotNull { clipMapper.fromDocChange(it) }
            .toList()

        if (removed.isNotEmpty()) {
            log("to be finally removed: {}", removed.size)
            txHelper.inTx("sync with server state") {
                clipBoxDao.deleteAll(removed.mapNotNull { clipBoxDao.getByFirestoreId(it.firestoreId) })
            }
        }
    }

    private fun updateCanSyncState() {
        userState.canSync.setValue(userState.canSyncNewNotes(), force = true)
    }

}