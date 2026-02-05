package clipto.dao.firebase

import clipto.common.extensions.disposeSilently
import clipto.domain.User
import clipto.extensions.log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import io.reactivex.disposables.Disposable
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class FirebaseOptimizedSnapshotListener(
    private val context: FirebaseExecutionContext,

    private val defaultSortOrderBy: String? = null,

    activeId: String,
    private val activeRef: CollectionReference,
    private val activeInitialCallback: (changes: List<DocumentChange>, isReconnected: Boolean) -> Unit,
    private val activeSnapshotCallback: (changes: List<DocumentChange>, isReconnected: Boolean) -> Unit,

    deletedId: String? = null,
    private val deletedRef: CollectionReference? = null,
    private val deletedInitialCallback: (changes: List<DocumentChange>, isReconnected: Boolean) -> Unit = { _, _ -> },
    private val deletedSnapshotCallback: (changes: List<DocumentChange>, isReconnected: Boolean) -> Unit = { _, _ -> },

    private val firstCallback: () -> Unit
) {

    private val userState = context.userState
    private val appConfig = userState.appConfig
    private val user: User = userState.user.requireValue()
    private val firebaseDaoHelper = context.firebaseDaoHelper
    private val queryOnlyLatestChangedNotesUseInitialQuery: Boolean = appConfig.queryOnlyLatestChangedNotesUseInitialQuery()
    private val queryOnlyLatestChangedNotesMinimumInterval: Long = appConfig.queryOnlyLatestChangedNotesMinimumInterval()
    private val queryOnlyLatestChangedNotesReconnectDelay: Long = appConfig.queryOnlyLatestChangedNotesReconnectDelay()
    private val queryOnlyLatestChangedNotesThreshold: Int = appConfig.queryOnlyLatestChangedNotesThreshold()
    private val activeChangesId = "${activeId}_${FirebaseDaoHelper.ATTR_CHANGE_TIMESTAMP}"
    private val deletedChangesId = "${deletedId}_${FirebaseDaoHelper.ATTR_CHANGE_TIMESTAMP}"

    private val firstCallbackConsumed = AtomicBoolean(false)
    private var activeListener: ListenerRegistration? = null
    private var deletedListener: ListenerRegistration? = null
    private var activeLatestDate: Date? = null
    private var deletedLatestDate: Date? = null

    private var reconnectActiveChangesDisposable: Disposable? = null
    private var reconnectDeletedChangesDisposable: Disposable? = null

    fun subscribe(forceSync: Boolean): FirebaseOptimizedSnapshotListener {
        getDeletedChanges(forceSync = forceSync, isReconnected = false)
        getActiveChanges(forceSync = forceSync, isReconnected = false)
        return this
    }

    fun dispose() {
        disposeActive()
        disposeDeleted()
    }

    private fun disposeActive() {
        reconnectActiveChangesDisposable.disposeSilently()
        activeListener?.remove()
        log("FIREBASE LISTENER :: disposed :: {}", activeChangesId)
    }

    private fun disposeDeleted() {
        if (deletedRef != null) {
            reconnectDeletedChangesDisposable.disposeSilently()
            deletedListener?.remove()
            log("FIREBASE LISTENER :: disposed :: {}", deletedChangesId)
        }
    }

    private fun reconnectActiveChanges(force: Boolean) {
        reconnectActiveChangesDisposable.disposeSilently()
        val delay = if (force) 0 else queryOnlyLatestChangedNotesReconnectDelay
        reconnectActiveChangesDisposable = userState.getBackgroundScheduler().scheduleDirect(
            { getActiveChanges(forceSync = false, isReconnected = true) },
            delay,
            TimeUnit.MILLISECONDS
        )
    }

    private fun reconnectDeletedChanges(force: Boolean) {
        reconnectDeletedChangesDisposable.disposeSilently()
        val delay = if (force) 0 else queryOnlyLatestChangedNotesReconnectDelay
        reconnectDeletedChangesDisposable = userState.getBackgroundScheduler().scheduleDirect(
            { getDeletedChanges(forceSync = false, isReconnected = true) },
            delay,
            TimeUnit.MILLISECONDS
        )
    }

    private fun getActiveChanges(forceSync: Boolean, isReconnected: Boolean) {
        disposeActive()

        if (forceSync) {
            context.appState.setLoadingState()
        }

        log(
            "FIREBASE LISTENER :: getActiveChanges :: id={}, forceSync={}, isReconnected={}",
            activeChangesId,
            forceSync,
            isReconnected
        )

        activeLatestDate =
            if (forceSync) {
                appConfig.saveDate(user, activeChangesId, null)
                null
            } else {
                appConfig.getDate(user, activeChangesId)
            }

        val lastDateRef = activeLatestDate
        log(
            "'FIREBASE LISTENER :: getActiveChanges :: id={}, last_date={}",
            activeChangesId,
            lastDateRef
        )

        val query = activeRef.let {
            when {
                lastDateRef != null -> {
                    it.whereGreaterThan(FirebaseDaoHelper.ATTR_CHANGE_TIMESTAMP, lastDateRef)
                        .orderBy(FirebaseDaoHelper.ATTR_CHANGE_TIMESTAMP)
                }
                defaultSortOrderBy != null -> {
                    log("FIREBASE LISTENER :: WARN :: no last date :: id={}", activeChangesId)
                    it.orderBy(defaultSortOrderBy)
                }
                else -> {
                    log("FIREBASE LISTENER :: WARN :: no last date :: id={}", activeChangesId)
                    it
                }
            }
        }

        if (forceSync && queryOnlyLatestChangedNotesUseInitialQuery) {
            log("FIREBASE LISTENER :: initial query")
            query.get()
                .addOnSuccessListener { snapshot ->
                    context.appState.onBackground {
                        val metadata = snapshot.metadata
                        val changes = snapshot.documentChanges

                        log("FIREBASE LISTENER :: initial query results :: {} - {}", metadata.isFromCache, changes.size)

                        activeInitialCallback.invoke(changes, isReconnected)

                        if (firstCallbackConsumed.compareAndSet(false, true)) {
                            firstCallback.invoke()
                        }

                        if (!metadata.isFromCache && changes.isNotEmpty()) {
                            val newDate = firebaseDaoHelper.getNewDateFromChanges(changes) ?: Date()
                            appConfig.saveDate(user, activeChangesId, newDate)
                            activeLatestDate = newDate
                            reconnectActiveChanges(forceSync)

                            if (deletedRef != null) {
                                val deletedDate = deletedLatestDate
                                if (deletedDate == null || deletedDate > newDate) {
                                    appConfig.saveDate(user, deletedChangesId, deletedDate)
                                    deletedLatestDate = newDate
                                    reconnectDeletedChanges(forceSync)
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    context.appState.setLoadingState(
                        FirebaseException(
                            code = "initial_data_${activeChangesId}",
                            throwable = it
                        )
                    )
                }
            return
        }

        log("FIREBASE LISTENER :: listen realtime")

        activeListener = firebaseDaoHelper.createListener(
            id = activeChangesId,
            query = query,
            forceSync = forceSync,
            initialCallback = { snapshot ->
                val metadata = snapshot.metadata
                val changes = snapshot.documentChanges

                activeInitialCallback.invoke(changes, isReconnected)

                if (firstCallbackConsumed.compareAndSet(false, true)) {
                    firstCallback.invoke()
                }

                if (!metadata.isFromCache && changes.isNotEmpty()) {
                    val newDate = firebaseDaoHelper.getNewDateFromChanges(changes) ?: Date()
                    appConfig.saveDate(user, activeChangesId, newDate)
                    activeLatestDate = newDate
                    reconnectActiveChanges(forceSync)

                    if (deletedRef != null) {
                        val deletedDate = deletedLatestDate
                        if (deletedDate == null || deletedDate > newDate) {
                            appConfig.saveDate(user, deletedChangesId, deletedDate)
                            deletedLatestDate = newDate
                            reconnectDeletedChanges(forceSync)
                        }
                    }
                }
            },
            snapshotCallback = { snapshot ->
                val metadata = snapshot.metadata
                val changes = snapshot.documentChanges

                activeSnapshotCallback.invoke(changes, isReconnected)

                if (firstCallbackConsumed.compareAndSet(false, true)) {
                    firstCallback.invoke()
                }

                if (activeLatestDate == null || (!metadata.isFromCache && (changes.isNotEmpty() || snapshot.size() > queryOnlyLatestChangedNotesThreshold))) {
                    val newDate = firebaseDaoHelper.getNewDateFromChanges(changes) ?: firebaseDaoHelper.getNewDateFromSnapshots(snapshot.documents) ?: Date()
                    if (activeLatestDate == null) {
                        log("FIREBASE LISTENER :: warn null active date")
                    }
                    log(
                        "FIREBASE LISTENER :: check if reconnect :: id={}, hasPendingWrites={}, newDate={}, prevDate={}, interval={}, snapshot.size={}, notesThreshold={}, changes={}",
                        activeChangesId,
                        metadata.hasPendingWrites(),
                        newDate,
                        activeLatestDate,
                        queryOnlyLatestChangedNotesMinimumInterval,
                        snapshot.size(),
                        queryOnlyLatestChangedNotesThreshold,
                        changes.size
                    )
                    val prevDate = activeLatestDate
                    val reconnect = prevDate == null ||
                            newDate.time - prevDate.time > queryOnlyLatestChangedNotesMinimumInterval ||
                            snapshot.size() > queryOnlyLatestChangedNotesThreshold
                    if (reconnect || (prevDate != null && prevDate > newDate)) {
                        appConfig.saveDate(user, activeChangesId, newDate)
                        activeLatestDate = newDate

                        if (deletedRef != null) {
                            val deletedDate = deletedLatestDate
                            if (deletedDate == null || deletedDate > newDate) {
                                appConfig.saveDate(user, deletedChangesId, newDate)
                                deletedLatestDate = newDate
                            }
                        }

                        if (reconnect) {
                            reconnectActiveChanges(forceSync)
                        }
                    }
                }
            }
        )
    }

    private fun getDeletedChanges(forceSync: Boolean, isReconnected: Boolean) {
        disposeDeleted()

        if (deletedRef == null) {
            return
        }

        log(
            "FIREBASE LISTENER :: _getDeletedChanges :; id={}, forceSync={}, isReconnected={}",
            deletedChangesId,
            forceSync,
            isReconnected
        )

        deletedLatestDate =
            if (!forceSync) {
                deletedLatestDate ?: appConfig.getDate(user, deletedChangesId) ?: activeLatestDate
            } else {
                Date()
            }

        appConfig.saveDate(user, deletedChangesId, deletedLatestDate)

        val lastDateRef = deletedLatestDate ?: return

        var query = deletedRef.whereGreaterThan(FirebaseDaoHelper.ATTR_CHANGE_TIMESTAMP, lastDateRef)
        query =
            if (forceSync) {
                query.orderBy(FirebaseDaoHelper.ATTR_CHANGE_TIMESTAMP, Query.Direction.DESCENDING).limit(1)
            } else {
                query.orderBy(FirebaseDaoHelper.ATTR_CHANGE_TIMESTAMP)
            }

        deletedListener = firebaseDaoHelper.createListener(
            id = deletedChangesId,
            query = query,
            forceSync = forceSync,
            initialCallback = { snapshot ->
                val changes = snapshot.documentChanges

                deletedInitialCallback.invoke(changes, isReconnected)

                val newDate = firebaseDaoHelper.getNewDateFromChanges(changes)
                if (newDate != null && newDate != deletedLatestDate) {
                    appConfig.saveDate(user, deletedChangesId, newDate)
                    deletedLatestDate = newDate
                    reconnectDeletedChanges(forceSync)
                }
            },
            snapshotCallback = { snapshot ->
                val metadata = snapshot.metadata
                val changes = snapshot.documentChanges

                deletedSnapshotCallback.invoke(changes, isReconnected)

                if (!metadata.isFromCache && (changes.isNotEmpty() || snapshot.size() > queryOnlyLatestChangedNotesThreshold)) {
                    val newDate = firebaseDaoHelper.getNewDateFromChanges(changes) ?: firebaseDaoHelper.getNewDateFromSnapshots(snapshot.documents) ?: Date()
                    log(
                        "FIREBASE LISTENER :: check if reconnect :: id={}, hasPendingWrites={}, newDate={}, prevDate={}, interval={}, snapshot.size={}, notesThreshold={}, changes={}",
                        deletedChangesId,
                        metadata.hasPendingWrites(),
                        newDate,
                        deletedLatestDate,
                        queryOnlyLatestChangedNotesMinimumInterval,
                        snapshot.size(),
                        queryOnlyLatestChangedNotesThreshold,
                        changes.size
                    )
                    val prevDate = deletedLatestDate
                    val reconnect = prevDate == null ||
                            newDate.time - prevDate.time > queryOnlyLatestChangedNotesMinimumInterval ||
                            snapshot.size() > queryOnlyLatestChangedNotesThreshold
                    if (reconnect || (prevDate != null && prevDate > newDate)) {
                        appConfig.saveDate(user, deletedChangesId, newDate)
                        deletedLatestDate = newDate
                        if (reconnect) {
                            reconnectDeletedChanges(forceSync)
                        }
                    }
                }
            }
        )
    }

}