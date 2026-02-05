package clipto.action

import clipto.domain.Filter
import clipto.repository.IClipRepository
import clipto.store.main.MainState
import dagger.Lazy
import io.reactivex.Completable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CleanupFiltersAction @Inject constructor(
        private val mainState: MainState,
        private val clipsRepository: Lazy<IClipRepository>,
) : CompletableAction<CleanupFiltersAction.Context>() {

    override val name: String = "cleanup_filter"

    override fun canExecute(context: Context): Boolean = context.filters.isNotEmpty()

    fun execute(filters: List<Filter>, callback: () -> Unit = {}) = execute(Context(filters), callback)

    override fun create(context: Context): Completable {
        val tags = context.filters
        val isActive = tags.any { mainState.activeFilter.getValue() == it }
        return clipsRepository.get().deleteAllFromFilters(tags)
                .doOnSuccess { if (isActive) mainState.requestClearFilter(closeNavigation = false) }
                .doOnSuccess { appState.refreshFilters() }
                .ignoreElement()
    }

    data class Context(val filters: List<Filter>) : ActionContext(showLoadingIndicator = false)

}