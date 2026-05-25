package clipto.repository

import clipto.common.misc.IdUtils
import clipto.dao.TxHelper
import clipto.dao.objectbox.ClipBoxDao
import clipto.dao.objectbox.FilterBoxDao
import clipto.dao.objectbox.model.FilterBox
import clipto.dao.objectbox.model.toBox
import clipto.domain.Filter
import clipto.extensions.log
import clipto.store.app.AppState
import io.reactivex.Completable
import io.reactivex.Single
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterRepository @Inject constructor(
    private val txHelper: TxHelper,
    private val appState: AppState,
    private val clipBoxDao: ClipBoxDao,
    private val filterBoxDao: FilterBoxDao
) : IFilterRepository {

    override fun terminate(): Completable = Completable.complete()

    override fun init(): Completable = Completable.complete()

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
                }
            }
            filterBox
        }
        .doOnSuccess { appState.refreshFilters() }

    override fun remove(filter: Filter): Single<Filter> = Single
        .fromCallable {
            val filterBox = filter.toBox()
            filterBoxDao.remove(filterBox)
            filterBox
        }
}