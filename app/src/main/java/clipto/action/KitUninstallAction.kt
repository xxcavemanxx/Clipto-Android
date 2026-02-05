package clipto.action

import clipto.dao.objectbox.ClipBoxDao
import clipto.domain.Filter
import clipto.domain.SnippetKit
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.repository.IClipRepository
import clipto.repository.IFilterRepository
import clipto.store.internet.InternetState
import clipto.store.main.MainState
import com.wb.clipboard.R
import dagger.Lazy
import dagger.hilt.android.scopes.ViewModelScoped
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject

@ViewModelScoped
class KitUninstallAction @Inject constructor(
    private val mainState: MainState,
    private val clipBoxDao: ClipBoxDao,
    private val dialogState: DialogState,
    private val internetState: InternetState,
    private val clipsRepository: Lazy<IClipRepository>,
    private val filterRepository: Lazy<IFilterRepository>
) : CompletableAction<KitUninstallAction.Context>() {

    override val name: String = "kit_uninstall"

    fun execute(kit: SnippetKit, callback: () -> Unit = {}) {
        internetState.withInternet(
            success = {
                val confirm = ConfirmDialogData(
                    iconRes = R.drawable.ic_attention,
                    title = appState.app.getString(R.string.snippet_kit_uninstall_title, kit.name),
                    description = appState.app.getString(R.string.snippet_kit_uninstall_description),
                    confirmActionTextRes = R.string.button_uninstall,
                    onConfirmed = { execute(Context(kit), callback) }
                )
                dialogState.showConfirm(confirm)
            }
        )
    }

    override fun create(context: Context): Completable {
        val kit = context.kit
        val filters = appState.getFilters()
        val filter = filters.findFilterBySnippetKit(kit) ?: return Completable.complete()
        val flow = Single.fromCallable {
            val snapshot = Filter.Snapshot().copy(filter).copy(cleanupRequest = true)
            clipBoxDao.getFiltered(snapshot).find().filter { it.snippetSetsIds.contains(filter.uid) }
        }
        return flow
            .flatMap { clips ->
                val clipsToDelete = clips.filter { it.snippetSetsIds.size == 1 && !it.isInternal() }
                val clipsToUnlink = clips.filter { it.snippetSetsIds.size > 1 || it.isInternal() }
                Single.zip(
                    listOf(
                        clipsRepository.get().deleteAll(clipsToDelete, permanently = true, withUndo = false, clearClipboard = true),
                        clipsRepository.get().deleteAllFromFilters(listOf(filter), clipsToUnlink)
                    )
                ) {}
            }
            .flatMap { filterRepository.get().remove(filter) }
            .doOnSuccess { appState.refreshFilters() }
            .doOnSuccess { mainState.requestClearFilter(closeNavigation = false) }
            .ignoreElement()
    }

    data class Context(val kit: SnippetKit) : ActionContext(showLoadingIndicator = true)

}