package clipto.action

import clipto.domain.Filter
import clipto.domain.SnippetKit
import clipto.presentation.common.dialog.DialogState
import clipto.repository.ISnippetRepository
import clipto.store.internet.InternetState
import clipto.store.main.MainState
import clipto.store.user.UserState
import dagger.Lazy
import dagger.hilt.android.scopes.ViewModelScoped
import io.reactivex.Completable
import javax.inject.Inject

@ViewModelScoped
class KitInstallAction @Inject constructor(
    private val mainState: MainState,
    private val userState: UserState,
    private val dialogState: DialogState,
    private val internetState: InternetState,
    private val snippetRepository: Lazy<ISnippetRepository>
) : CompletableAction<KitInstallAction.Context>() {

    override val name: String = "kit_install"

    fun execute(kit: SnippetKit, callback: (filter: Filter) -> Unit = {}) {
        internetState.withInternet(
            success = {
                userState.withAuth {
                    execute(Context(kit, callback))
                }
            }
        )
    }

    override fun create(context: Context): Completable {
        val kit = context.kit
        return snippetRepository.get().installKit(kit)
            .doOnError { dialogState.showError(it) }
            .flatMap { filter ->
                appState.filters.getLiveChanges()
                    .filter { it.isNotNull() }
                    .map { it.requireValue() }
                    .filter { it.getByUid(filter.uid)?.name != null }
                    .map { it.getByUid(filter.uid)!! }
                    .firstElement()
                    .toSingle()
            }
            .doOnSuccess { filter -> context.callback.invoke(filter) }
            .doOnSuccess { filter -> mainState.requestApplyFilter(filter) }
            .ignoreElement()
    }

    data class Context(val kit: SnippetKit, val callback: (filter: Filter) -> Unit) : ActionContext(showLoadingIndicator = true)

}