package clipto.action

import clipto.domain.User
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.presentation.common.dialog.hint.HintDialogData
import clipto.repository.IUserRepository
import clipto.store.internet.InternetState
import com.wb.clipboard.R
import dagger.Lazy
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.reactivex.Completable
import javax.inject.Inject

@ActivityRetainedScoped
class RebuildIndexAction @Inject constructor(
    private val dialogState: DialogState,
    private val internetState: InternetState,
    private val userRepository: Lazy<IUserRepository>
) : CompletableAction<RebuildIndexAction.Context>() {

    override val name: String = "rebuild_index"

    fun execute(user: User, callback: () -> Unit = {}) {
        internetState.withInternet(
            success = {
                val app = appState.app
                dialogState.showConfirm(ConfirmDialogData(
                    iconRes = R.drawable.ic_attention,
                    title = app.getString(R.string.account_rebuild_index_title),
                    description = app.getString(R.string.account_rebuild_index_description),
                    confirmActionTextRes = R.string.button_continue,
                    onConfirmed = {
                        execute(Context(user), callback)
                    }
                ))
            }
        )
    }

    override fun create(context: Context): Completable {
        return userRepository.get()
            .upgrade()
            .doOnSuccess {
                dialogState.showHint(
                    HintDialogData(
                        title = appState.app.getString(R.string.account_rebuild_index_title),
                        descriptionIsMarkdown = true,
                        description = it
                    )
                )
            }
            .doOnError { dialogState.showError(it) }
            .ignoreElement()
    }

    data class Context(val user: User) : ActionContext(showLoadingIndicator = true)

}