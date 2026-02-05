package clipto.dao.firebase

import clipto.common.misc.IdUtils
import clipto.config.IAppConfig
import clipto.dao.firebase.mapper.FileMapper
import clipto.dao.firebase.model.UserCollection
import clipto.domain.Clip
import clipto.domain.FileRef
import clipto.extensions.log
import clipto.store.app.AppState
import clipto.store.user.UserState
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileFirebaseDao @Inject constructor(
    private val appState: AppState,
    private val userState: UserState,
    private val appConfig: IAppConfig,
    private val fileMapper: FileMapper,
    private val firebaseDaoHelper: FirebaseDaoHelper,
    private val executionContext: FirebaseExecutionContext
) {

    private var changesListener: FirebaseOptimizedSnapshotListener? = null

    fun stopSync() {
        changesListener?.dispose()
        changesListener = null
    }

    fun startSync(
        activeInitialCallback: (changes: List<DocumentChange>, isReconnected: Boolean) -> Unit,
        activeSnapshotCallback: (changes: List<DocumentChange>, isReconnected: Boolean) -> Unit,
        deletedCallback: (changes: List<DocumentChange>, isReconnected: Boolean) -> Unit,
        firstCallback: () -> Unit
    ) {
        stopSync()
        val collection = firebaseDaoHelper.getAuthUserCollection()
        if (collection != null) {
            val forceSync = userState.forceSync.requireValue()
            val changesListenerRef = FirebaseOptimizedSnapshotListener(
                context = executionContext,

                activeId = "fs",
                activeRef = collection.getFilesRef(),
                activeInitialCallback = activeInitialCallback,
                activeSnapshotCallback = activeSnapshotCallback,

                deletedId = "fsd",
                deletedRef = collection.getFilesDeletedRef(),
                deletedInitialCallback = deletedCallback,
                deletedSnapshotCallback = deletedCallback,

                defaultSortOrderBy = FirebaseDaoHelper.ATTR_FILE_CREATED,

                firstCallback = firstCallback
            )
            changesListener = changesListenerRef.subscribe(forceSync)
        } else {
            firstCallback.invoke()
        }
    }


    fun save(fileRef: FileRef, collection: UserCollection) {
        saveInBatch(fileRef, null, collection)
    }

    fun saveInBatch(
        fileRef: FileRef,
        batch: WriteBatch?,
        collection: UserCollection,
        changes: MutableMap<String, Any?>? = null
    ) {
        val uid = fileRef.getUid() ?: IdUtils.autoId()
        fileRef.firestoreId = uid
        val ref = collection.getActiveFileRef(uid)
        if (!changes.isNullOrEmpty()) {
            FirebaseDaoHelper.normalizeMap(changes, deleteFromCloud = true)
            log("update file with id: {}, {}", uid, changes)
            changes[FirebaseDaoHelper.ATTR_CHANGE_TIMESTAMP] = FirebaseDaoHelper.getServerTimestamp()
            changes[FirebaseDaoHelper.ATTR_API_VERSION] = appConfig.getApiVersion()
            changes[FirebaseDaoHelper.ATTR_DEVICE_ID] = appState.getInstanceId()
            if (batch != null) {
                batch.update(ref, changes)
            } else {
                ref.update(changes)
            }
        } else {
            val fileData = fileMapper.toMap(fileRef)
            val setOptions = SetOptions.merge()
            if (batch != null) {
                batch.set(ref, fileData, setOptions)
            } else {
                ref.set(fileData, setOptions)
            }
        }
    }

    fun deleteInBatch(
        file: FileRef,
        batch: WriteBatch,
        collection: UserCollection
    ) {
        val id = file.firestoreId ?: return
        val fileData = fileMapper.toMap(file)
        val fileRef = collection.getActiveFileRef(id)
        val deletedFileRef = collection.getDeletedFileRef(id)
        batch.set(deletedFileRef, fileData, SetOptions.merge())
        batch.delete(fileRef)
    }

    fun deleteAllInBatch(
        files: List<FileRef>,
        batch: WriteBatch,
        collection: UserCollection
    ) {
        files.forEach { file ->
            deleteInBatch(file, batch, collection)
        }
    }

}