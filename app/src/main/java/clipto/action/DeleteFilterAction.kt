package clipto.action

import clipto.domain.Filter
import clipto.extensions.getTitle
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.repository.IClipRepository
import clipto.repository.IFilterRepository
import clipto.store.main.MainState
import com.wb.clipboard.R
import dagger.Lazy
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.reactivex.Completable
import javax.inject.Inject

@ActivityRetainedScoped
class DeleteFilterAction @Inject constructor(
        private val mainState: MainState,
        private val dialogState: DialogState,
        private val clipsRepository: Lazy<IClipRepository>,
        private val filterRepository: Lazy<IFilterRepository>
) : CompletableAction<DeleteFilterAction.Context>() {

    override val name: String = "delete_filter"

    fun execute(filter: Filter, callback: () -> Unit = {}) {
        val titleRes: Int
        val descriptionRes: Int
        when {
            filter.isTag() -> {
                titleRes = R.string.filter_tag_delete_title
                descriptionRes = R.string.filter_tag_delete_description
            }
            filter.isSnippetKit() -> {
                titleRes = R.string.snippet_kit_delete_title
                descriptionRes = R.string.snippet_kit_delete_description
            }
            else -> {
                titleRes = R.string.filter_delete_title
                descriptionRes = R.string.filter_delete_description
            }
        }
        val app = appState.app
        dialogState.showConfirm(ConfirmDialogData(
                iconRes = R.drawable.ic_attention,
                title = app.getString(titleRes, filter.getTitle(app)),
                description = app.getString(descriptionRes),
                confirmActionTextRes = R.string.menu_delete,
                onConfirmed = { execute(Context(filter), callback) }
        ))
    }

    override fun create(context: Context): Completable {
        val filter = context.filter
        val isActive = mainState.activeFilter.getValue() == filter
        return clipsRepository.get().deleteAllFromFilters(listOf(filter))
                .flatMap { filterRepository.get().remove(filter) }
                .doOnSuccess { if (isActive) mainState.requestClearFilter(closeNavigation = false) }
                .doOnSuccess { appState.refreshFilters() }
                .ignoreElement()
    }

    data class Context(val filter: Filter) : ActionContext(showLoadingIndicator = true)

}