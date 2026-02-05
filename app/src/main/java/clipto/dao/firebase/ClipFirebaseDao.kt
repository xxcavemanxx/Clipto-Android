package clipto.dao.firebase

import clipto.common.misc.IdUtils
import clipto.config.IAppConfig
import clipto.dao.firebase.mapper.ClipMapper
import clipto.dao.firebase.mapper.DateMapper
import clipto.dao.firebase.model.UserCollection
import clipto.domain.Clip
import clipto.extensions.log
import clipto.store.app.AppState
import clipto.store.user.UserState
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipFirebaseDao @Inject constructor(
    private val appState: AppState,
    private val appConfig: IAppConfig,
    private val userState: UserState,
    private val clipMapper: ClipMapper,
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
        deletedInitialCallback: (changes: List<DocumentChange>, isReconnected: Boolean) -> Unit,
        deletedSnapshotCallback: (changes: List<DocumentChange>, isReconnected: Boolean) -> Unit,
        firstCallback: () -> Unit
    ) {
        stopSync()
        val collection = firebaseDaoHelper.getAuthUserCollection()
        if (collection != null) {
            val forceSync = userState.forceSync.requireValue()
            val changesListenerRef = FirebaseOptimizedSnapshotListener(
                context = executionContext,

                activeId = "c",
                activeRef = collection.getActiveClipsRef(),
                activeInitialCallback = activeInitialCallback,
                activeSnapshotCallback = activeSnapshotCallback,

                deletedId = "cd",
                deletedRef = collection.getDeletedClipsRef(),
                deletedInitialCallback = deletedInitialCallback,
                deletedSnapshotCallback = deletedSnapshotCallback,

                defaultSortOrderBy = FirebaseDaoHelper.ATTR_CLIP_CREATE_DATE,

                firstCallback = firstCallback
            )
            changesListener = changesListenerRef.subscribe(forceSync)
        } else {
            firstCallback.invoke()
        }
    }

    fun saveInBatch(
        clip: Clip,
        batch: WriteBatch?,
        collection: UserCollection,
        changes: MutableMap<String, Any?>? = null
    ): Boolean {
        val persist: Boolean = !clip.isSynced() || !clip.sourceClips.isNullOrEmpty()
        val id = getFirestoreId(clip)
        val clipRef = collection.getActiveClipRef(id)
        if (!changes.isNullOrEmpty()) {
            FirebaseDaoHelper.normalizeMap(changes, deleteFromCloud = true)
            log("update clip with id: {}, {}", id, changes)
            changes[FirebaseDaoHelper.ATTR_CHANGE_TIMESTAMP] = FirebaseDaoHelper.getServerTimestamp()
            changes[FirebaseDaoHelper.ATTR_API_VERSION] = appConfig.getApiVersion()
            changes[FirebaseDaoHelper.ATTR_DEVICE_ID] = appState.getInstanceId()
            if (batch != null) {
                batch.update(clipRef, changes)
            } else {
                clipRef.update(changes)
            }
        } else {
            log("save new clip with id: {}, {}", id, clip)
            if (batch != null) {
                batch.set(clipRef, clipMapper.toMap(clip), SetOptions.merge())
            } else {
                clipRef.set(clipMapper.toMap(clip), SetOptions.merge())
            }
        }
        return persist
    }

    fun save(
        clip: Clip,
        collection: UserCollection,
        changes: MutableMap<String, Any?>? = null
    ): Boolean {
        return saveInBatch(clip, null, collection, changes)
    }

    fun deleteInBatch(
        clip: Clip,
        batch: WriteBatch,
        collection: UserCollection
    ) {
        val id = clip.firestoreId ?: return
        val clipRef = collection.getActiveClipRef(id)
        if (clip.isDeleted()) {
            batch.update(
                clipRef, mapOf(
                    FirebaseDaoHelper.ATTR_DEVICE_ID to appState.getInstanceId(),
                    FirebaseDaoHelper.ATTR_API_VERSION to appConfig.getApiVersion(),
                    FirebaseDaoHelper.ATTR_CHANGE_TIMESTAMP to FirebaseDaoHelper.getServerTimestamp(),
                    FirebaseDaoHelper.ATTR_CLIP_DELETE_DATE to DateMapper.toTimestamp(clip.deleteDate, true)
                )
            )
        } else {
            val deletedClipRef = collection.getDeletedClipRef(id)
            batch.set(
                deletedClipRef, mapOf(
                    FirebaseDaoHelper.ATTR_DEVICE_ID to appState.getInstanceId(),
                    FirebaseDaoHelper.ATTR_API_VERSION to appConfig.getApiVersion(),
                    FirebaseDaoHelper.ATTR_CHANGE_TIMESTAMP to FirebaseDaoHelper.getServerTimestamp()
                )
            )
            batch.delete(clipRef)
            clip.publicLink?.let { batch.delete(collection.getPublicNoteRef(id)) }
        }
    }

    fun deleteAllInBatch(
        clips: List<Clip>,
        batch: WriteBatch,
        collection: UserCollection
    ) {
        clips.forEach { clip ->
            deleteInBatch(clip, batch, collection)
        }
    }

    private fun getFirestoreId(clip: Clip): String {
        val fid = clip.firestoreId ?: clip.snippetId?.takeIf { clip.isInternal() } ?: IdUtils.autoId()
        clip.firestoreId = fid
        return fid
    }

}