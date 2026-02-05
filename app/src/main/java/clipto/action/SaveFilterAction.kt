package clipto.action

import clipto.cache.AppTextCache
import clipto.domain.Filter
import clipto.repository.IFilterRepository
import clipto.store.filter.FilterState
import clipto.store.main.MainState
import dagger.Lazy
import io.reactivex.Completable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveFilterAction @Inject constructor(
    private val mainState: MainState,
    private val filterState: FilterState,
    private val filterRepository: Lazy<IFilterRepository>
) : CompletableAction<SaveFilterAction.Context>() {

    override val name: String = "save_filter"

    fun execute(filter: Filter, reload: Boolean = false, callback: () -> Unit = {}) = execute(Context(filter, reload), callback)

    override fun create(context: Context): Completable = filterRepository.get()
        .save(context.filter)
        .doOnSuccess { if (!context.isNew) AppTextCache.clearCache() }
        .doOnSuccess { if (context.reload) mainState.requestReloadFilter(closeNavigation = false) }
        .doOnSuccess { filterState.requestUpdateFilter(it) }
        .ignoreElement()

    data class Context(val filter: Filter, val reload: Boolean = false, val isNew: Boolean = filter.isNew()) : ActionContext()

}