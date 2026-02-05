package clipto.action

import clipto.domain.User
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.repository.IClipRepository
import clipto.repository.IFilterRepository
import clipto.repository.IUserRepository
import clipto.store.internet.InternetState
import clipto.store.main.MainState
import com.wb.clipboard.R
import dagger.Lazy
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.reactivex.Completable
import java.util.*
import javax.inject.Inject

@ActivityRetainedScoped
class DeleteAccountAction @Inject constructor(
    private val dialogState: DialogState,
    private val internetState: InternetState,
    private val userRepository: Lazy<IUserRepository>
) : CompletableAction<DeleteAccountAction.Context>() {

    override val name: String = "delete_account"

    fun execute(user: User, callback: () -> Unit = {}) {
        internetState.withInternet(
            success = {
                val app = appState.app
                dialogState.showConfirm(ConfirmDialogData(
                    iconRes = R.drawable.ic_attention,
                    title = app.getString(R.string.account_delete_confirm_title),
                    description = app.getString(R.string.account_delete_confirm_description),
                    confirmActionTextRes = R.string.menu_delete,
                    onConfirmed = {
                        dialogState.showConfirm(ConfirmDialogData(
                            iconRes = R.drawable.ic_status_error,
                            title = app.getString(R.string.account_delete_confirm_again_title),
                            description = app.getString(R.string.account_delete_confirm_again_description),
                            confirmActionTextRes = R.string.button_confirm,
                            onConfirmed = { execute(Context(user), callback) }
                        ))
                    }
                ))
            }
        )
    }

    override fun create(context: Context): Completable {
        return userRepository.get()
            .delete(context.user)
            .doOnError { dialogState.showError(it) }
            .ignoreElement()
    }

    data class Context(val user: User) : ActionContext(showLoadingIndicator = true)

}