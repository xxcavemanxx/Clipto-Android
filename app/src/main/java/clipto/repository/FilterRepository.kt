package clipto.repository

import clipto.action.CleanupFiltersAction
import clipto.common.misc.IdUtils
import clipto.dao.TxHelper
import clipto.dao.firebase.FilterFirebaseDao
import clipto.dao.firebase.mapper.FilterMapper
import clipto.dao.objectbox.ClipBoxDao
import clipto.dao.objectbox.FilterBoxDao
import clipto.dao.objectbox.model.FilterBox
import clipto.dao.objectbox.model.toBox
import clipto.domain.Filter
import clipto.extensions.log
import clipto.store.app.AppState
import clipto.store.filter.FilterState
import clipto.store.main.MainState
import clipto.store.user.UserState
import com.google.firebase.firestore.DocumentChange
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Single
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterRepository @Inject constructor(
    private val txHelper: TxHelper,
    private val appState: AppState,
    private val mainState: MainState,
    private val filterState: FilterState,
    private val userState: UserState,
    private val clipBoxDao: ClipBoxDao,
    private val filterMapper: FilterMapper,
    private val filterBoxDao: FilterBoxDao,
    private val filterFirebaseDao: FilterFirebaseDao,
    private val cleanupFiltersAction: Lazy<CleanupFiltersAction>
) : IFilterRepository {

    override fun terminate(): Completable = Completable.fromCallable { filterFirebaseDao.stopSync() }

    override fun init(): Completable = terminate()
        .andThen(Completable.fromPublisher<Boolean> { publisher ->
            log("FIREBASE LISTENER :: init filters")
            filterFirebaseDao.startSync(
                activeInitialCallback = { changes, isReconnected ->
                    updateFilters(changes, initialSync = true, isReconnected = isReconnected)
                },
                activeSnapshotCallback = { changes, isReconnected ->
                    updateFilters(changes, initialSync = false, isReconnected = isReconnected)
                },
                firstCallback = {
                    publisher.onNext(true)
                    publisher.onComplete()
                }
            )
        })

    override fun updateNotesCount(filter: Filter): Single<Filter> = Single
        .fromCallable {
            val snapshot = Filter.Snapshot().copy(filter)
            val notesCount = clipBoxDao.getFiltered(snapshot).count()
            filter.notesCount = notesCount
            filter
        }

    override fun save(filter: Filter): Single<Filter> = Single
        .fromCallable<Filter> {
            val filterBox = filter.toBox()
            if (filterBox.isLast()) {
                if (appState.getSettings().restoreFilterOnStart) {
                    val prevFilter = filterBoxDao.getByUid(filterBox.uid)
                    val changed = prevFilter?.syncDate == null || !prevFilter.isSame(filterBox)
                    log("save last filter :: {}", changed)
                    if (changed) {
                        filterBoxDao.save(filterBox)
                    }
                }
            } else {
                val prevFilter = filterBoxDao.getByUid(filterBox.uid)
                val changed = prevFilter?.syncDate == null || !prevFilter.isSame(filterBox)
                log("save filter :: id={}, save={}", filterBox.uid, changed)
                if (changed) {
                    if (filterBox.isNew()) {
                        filterBox.hideHint = true
                    }
                    when {
                        filterBox.isTag() -> {
                            val id = filterBox.uid ?: IdUtils.autoId()
                            filterBox.tagIds = listOf(id)
                            filterBox.uid = id
                        }
                        else -> {
                            val id = filterBox.uid ?: IdUtils.autoId()
                            filterBox.uid = id
                        }
                    }
                    filterBox.syncDate = Date()
                    filterBoxDao.save(filterBox)
                    filterFirebaseDao.save(filterBox)
                }
            }
            filterBox
        }
        .doOnSuccess { appState.refreshFilters() }

    override fun remove(filter: Filter): Single<Filter> = Single
        .fromCallable {
            val filterBox = filter.toBox()
            filterBoxDao.remove(filterBox)
            filterFirebaseDao.remove(filterBox)
            filterBox
        }

    private fun updateFilters(changes: List<DocumentChange>, initialSync: Boolean, isReconnected: Boolean) {
        val removed = changes
            .filter { !it.document.metadata.hasPendingWrites() }
            .filter { it.type != DocumentChange.Type.REMOVED && filterMapper.isDeleted(it.document) }
            .mapNotNull { filterMapper.fromDocChange((it.document)) }

        val changed = changes
            .filter { !it.document.metadata.hasPendingWrites() }
            .filter { it.type != DocumentChange.Type.REMOVED && !filterMapper.isDeleted(it.document) }
            .mapNotNull { filterMapper.fromDocChange((it.document)) }

        var isChanged = false
        var changedFilter: Filter? = null
        val updatedList = mutableSetOf<FilterBox>()
        val removedList = mutableSetOf<FilterBox>()
        val pinStarredEnabled = appState.getFilters().groupNotes.pinStarredEnabled

        if (initialSync && !isReconnected) {
            userState.deletedTags.setValue(removed.filter { it.isTag() })
        }

        if (removed.isNotEmpty() || changed.isNotEmpty()) {
            log("to be removed: {}, to be changed: {}", removed.size, changed.size)
            txHelper.inTx("sync with server state") {
                removed.forEach {
                    log("try to remove: {} -> {}", it.name, it.color)
                    filterBoxDao.getByUid(it.uid!!)?.let { filter ->
                        log("remove filter: {}", filter.name)
                        filterBoxDao.remove(filter)
                        updatedList.add(filter)
                        removedList.add(filter)
                        isChanged = true
                    }
                }
                changed.forEach { newFilter ->
                    if (initialSync) {
                        newFilter.hideHint = true
                    }
                    var prevFilter = filterBoxDao.getByUid(newFilter.uid!!)
                    if (prevFilter == null || !prevFilter.isSame(newFilter)) {
                        if (changedFilter == null && (prevFilter?.color != newFilter.color)) {
                            changedFilter = prevFilter ?: newFilter
                        }
                        prevFilter = prevFilter?.withFilter(newFilter, withAllAttrs = true)?.toBox() ?: newFilter
                        log("update filter :: {}", prevFilter)
                        filterBoxDao.save(prevFilter)
                        isChanged = true
                    }
                    updatedList.add(prevFilter)
                }
            }
        }

        if (!isReconnected && initialSync) {
            val newFilters = filterBoxDao.getAll()
                .filter { !it.isLast() }
                .filter { !it.uid.isNullOrBlank() }
                .filter { f -> updatedList.find { it.uid == f.uid } == null }
                .toList()
            filterFirebaseDao.saveAll(newFilters)
        }
        if (isChanged) {
            val active = appState.getFilters().findActive()
            mainState.requestApplyListConfig { it.copy(active) }
            changedFilter?.let { filterState.requestUpdateFilter(it) }
            appState.refreshFilters()
            if (appState.getFilters().groupNotes.pinStarredEnabled != pinStarredEnabled) {
                mainState.requestReloadFilter()
            }
        }
        removedList
            .filter { it.isTag() }
            .takeIf { it.isNotEmpty() }
            ?.let { cleanupFiltersAction.get().execute(it) }
    }

}