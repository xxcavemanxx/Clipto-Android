package clipto

import android.app.Application
import android.content.Intent
import clipto.action.ShareAppLinkAction
import clipto.action.intent.IntentActionFactory
import clipto.analytics.Analytics
import clipto.common.logging.L
import clipto.common.presentation.mvvm.RxViewModel
import clipto.config.IAppConfig
import clipto.dao.firebase.FirebaseDaoHelper
import clipto.domain.User
import clipto.dynamic.presentation.field.DynamicFieldState
import clipto.dynamic.presentation.text.DynamicTextState
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.presentation.snippets.details.SnippetKitDetailsViewModel
import clipto.repository.ISettingsRepository
import clipto.repository.IUserRepository
import clipto.store.app.AppState
import clipto.store.clipboard.ClipboardState
import clipto.store.filter.FilterDetailsState
import clipto.store.folder.FolderState
import clipto.store.internet.InternetState
import clipto.store.lock.LockState
import clipto.store.main.MainState
import clipto.store.user.UserState
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppContainerViewModel @Inject constructor(
    app: Application,
    lockState: LockState,
    clipboardState: ClipboardState,
    val appState: AppState,
    val userState: UserState,
    val mainState: MainState,
    val appConfig: IAppConfig,
    val dialogState: DialogState,
    val folderState: FolderState,
    val filterDetailsState: FilterDetailsState,
    val internetState: InternetState,
    val dynamicTextState: DynamicTextState,
    val dynamicFieldState: DynamicFieldState,
    val firebaseDaoHelper: FirebaseDaoHelper,
    private val userRepository: IUserRepository,
    private val settingsRepository: ISettingsRepository,
    private val shareAppLinkAction: ShareAppLinkAction,
    private val intentActionFactory: IntentActionFactory
) : RxViewModel(app) {

    val settings = appState.getSettings()

    val lockedLive = lockState.locked.getLiveData()
    val navigateToLive = appState.navigateTo.getLiveData()
    val hideOnCopyLive = clipboardState.hideOnCopy.getLiveData()
    val loadingStateLive = appState.dataLoadingState.getLiveData()

    val canSyncLive = userState.canSync.getLiveData()
    val requestSignInLive = userState.requestSignIn.getLiveData()
    val requestSignOutLive = userState.requestSignOut.getLiveData()
    val requestShareAppLive = userState.requestShareApp.getLiveData()

    fun isLocked() = settings.isLocked()
    fun getTheme() = appState.getTheme()

    fun onSignIn(user: User) {
        userRepository.login(user)
            .subscribeBy("onSignIn") {
                Analytics.onSignedIn()
            }
    }

    fun onSignOut(user: User) {
        userRepository.logout(user)
            .doOnSubscribe { appState.setLoadingState() }
            .subscribeBy("onSignOut") {
                Analytics.onSignedOut()
                appState.setLoadedState()
            }
    }

    fun onShareApp() {
        internetState.withInternet({
            shareAppLinkAction.execute()
        })
    }

    fun onShowSyncRestriction() {
        val confirm = ConfirmDialogData(
            iconRes = R.drawable.ic_attention,
            title = string(R.string.account_sync_plan_warning_limit_reached_title),
            description = string(R.string.account_sync_plan_warning_limit_reached_sub_title, userState.getSyncedNotesCount(), userState.getSyncLimit()),
            confirmActionTextRes = R.string.button_update,
            onConfirmed = { appState.requestNavigateTo(R.id.action_select_plan) }
        )
        dialogState.showConfirm(confirm)
    }

    fun onOpenIntent(intent: Intent) {
        if (appState.lastIntent.setValue(intent)) {
            if (!intentActionFactory.handle(app, intent)) {
                runCatching {
                    FirebaseDynamicLinks.getInstance().getDynamicLink(intent)
                        .addOnFailureListener { L.log(this, "error_get_dynamic_link", it) }
                        .addOnSuccessListener { linkData ->
                            linkData?.link?.let { link ->
                                AppUtils.fromReferralUri(link) { referralId ->
                                    referralId?.let {
                                        settings.referralId = referralId
                                        settingsRepository.update(settings)
                                            .subscribeBy("updateSettings")
                                    }
                                }
                                AppUtils.fromSnippetKitUri(link) { id ->
                                    val args = SnippetKitDetailsViewModel.buildArgs(id)
                                    appState.requestNavigateTo(R.id.action_snippet_kit_details, args)
                                }
                            }
                        }
                }
            }
            AppUtils.getAuthTokenFromUrl(intent.dataString)?.let {
                userState.requestSignIn(UserState.SignInRequest.newWebAuthTokenRequest(it))
            }
        }
    }

}