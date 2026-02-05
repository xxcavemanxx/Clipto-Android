package clipto.dao.firebase

import clipto.dao.firebase.mapper.FilterMapper
import clipto.dao.firebase.model.UserCollection
import clipto.domain.Filter
import clipto.store.app.AppState
import clipto.store.user.UserState
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterFirebaseDao @Inject constructor(
    private val appState: AppState,
    private val userState: UserState,
    private val filterMapper: FilterMapper,
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
        firstCallback: () -> Unit
    ) {
        stopSync()
        val collection = firebaseDaoHelper.getAuthUserCollection()
        if (collection != null) {
            val forceSync = userState.forceSync.requireValue() || appState.getFilters().initial
            val changesListenerRef = FirebaseOptimizedSnapshotListener(
                context = executionContext,
                activeId = "f",
                activeRef = collection.getFiltersRef(),
                activeInitialCallback = activeInitialCallback,
                activeSnapshotCallback = activeSnapshotCallback,
                firstCallback = firstCallback
            )
            changesListener = changesListenerRef.subscribe(forceSync)
        } else {
            firstCallback.invoke()
        }
    }


    fun save(
        filter: Filter,
        batch: WriteBatch? = null,
        collection: UserCollection? = null
    ) {
        val collectionRef = collection ?: firebaseDaoHelper.getAuthUserCollection()
        collectionRef?.let {
            val uid = filter.uid
            if (!filter.isLast() && uid != null) {
                val filterRef = collectionRef.getFilterRef(uid)
                batch?.set(filterRef, filterMapper.toMap(filter), SetOptions.merge())
                    ?: filterRef.set(filterMapper.toMap(filter), SetOptions.merge())
            }
        }
    }

    fun saveAll(filters: List<Filter>) {
        if (filters.isNotEmpty()) {
            firebaseDaoHelper.getAuthUserCollection()?.let { collection ->
                val batch = collection.createBatch()
                filters.forEach { filter ->
                    val uid = filter.uid
                    if (uid != null) {
                        val filterRef = collection.getFilterRef(uid)
                        batch.set(filterRef, filterMapper.toMap(filter), SetOptions.merge())
                    }
                }
                batch.commit()
            }
        }
    }

    fun remove(filter: Filter) {
        val uid = filter.uid
        if (uid != null) {
            firebaseDaoHelper.getAuthUserCollection()?.let { collection ->
                val filterRef = collection.getFilterRef(uid)
                filterRef.set(filterMapper.toMap(filter, deleted = true), SetOptions.merge())
            }
        }
    }

}