package clipto

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import clipto.analytics.Analytics
import clipto.common.extensions.navigateSafe
import clipto.common.extensions.onBackPressDeclined
import clipto.common.extensions.withSafeFragmentManager
import clipto.common.misc.IntentUtils
import clipto.common.presentation.fragment.IndeterminateProgressFragment
import clipto.common.presentation.mvvm.ActivityBackPressConsumer
import clipto.common.presentation.mvvm.MvvmNavigationActivity
import clipto.common.presentation.mvvm.model.DataLoadingState
import clipto.dao.firebase.FirebaseException
import clipto.domain.User
import clipto.extensions.onCreateWithTheme
import clipto.presentation.auth.IAuth
import clipto.presentation.clip.add.AddClipFragment
import clipto.presentation.clip.add.data.AddClipRequest
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.presentation.notification.NewVersionBannerFragment
import clipto.presentation.notification.NotificationBannerFragment
import com.wb.clipboard.BuildConfig
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanCustomCode
import kotlinx.android.synthetic.main.activity_navigation.*
import javax.inject.Inject

@AndroidEntryPoint
class AppContainer : MvvmNavigationActivity<AppContainerViewModel>() {

    @Inject
    lateinit var auth: IAuth

    private var initialized = false

    private val scanBarcode = registerForActivityResult(ScanCustomCode(), ::onBarcodeResult)

    override val layoutResId: Int = R.layout.activity_navigation
    override val viewModel: AppContainerViewModel by viewModels()
    override fun getNavHostFragment(): NavHostFragment = navHostFragment as NavHostFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        onCreateWithTheme()
        super.onCreate(savedInstanceState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onCreateWithTheme()
    }

    override fun bind(viewModel: AppContainerViewModel) {
        initCommonListeners()
        initNewVersionAvailable()
        initSelectPlanBanner()
        initShareAppRequest()
        initSignOut()
        initSignIn()
        initSplash()
        viewModel.onOpenIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        viewModel.onOpenIntent(intent)
        super.onNewIntent(intent)
    }

    override fun onBackPressed() {
        val canNavigateUp = navController.currentDestination?.id != navController.graph.startDestination
        if (canNavigateUp) {
            onSupportNavigateUp()
        } else {
            val fragment = navHostFragment?.childFragmentManager?.primaryNavigationFragment
            if (fragment is ActivityBackPressConsumer && fragment.onBackPressConsumed()) {
                // do nothing
            } else if (viewModel.settings.doubleClickToExit && onBackPressDeclined()) {
                // do nothing
            } else {
                super.onBackPressed()
            }
        }
    }

    private fun initCommonListeners() {
        viewModel.folderState.bind(this)
        viewModel.dialogState.bind(this)
        viewModel.dialogState.bind(this, scanBarcode)
        viewModel.filterDetailsState.bind(this)
        viewModel.dynamicTextState.bind(this)
        viewModel.dynamicFieldState.bind(this)
        viewModel.hideOnCopyLive.observe(this) { moveTaskToBack(true) }
        viewModel.appState.restart.getLiveData().observe(this) { recreate() }
        viewModel.navigateToLive.observe(this) {
            navController.navigateSafe(it.destinationId, it.args)
        }
        viewModel.appState.clipInfoTextRequest.getLiveData().observe(this) {
            AddClipFragment.show(
                this, AddClipRequest(
                    text = it.text,
                    scanBarcode = it.scanBarcode,
                    folderId = viewModel.appState.getActiveFolderId()
                )
            )
        }
        viewModel.loadingStateLive.observe(this) {
            withSafeFragmentManager()?.let { fm ->
                if (it == DataLoadingState.LOADING) {
                    val timeout = viewModel.appConfig.dataLoadingTimeout()
                    IndeterminateProgressFragment.show(fm, timeout = timeout)
                } else {
                    IndeterminateProgressFragment.hide(fm)
                    if (it is DataLoadingState.Error && viewModel.appConfig.canReportUnexpectedErrors()) {
                        val error = it.throwable
                        if (error != null) {
                            NotificationBannerFragment.show(
                                this,
                                code = it.code,
                                title = viewModel.string(R.string.error_unexpected),
                                message = viewModel.string(R.string.error_unexpected_send),
                                error = error
                            )
                            Analytics.onError("error_${it.code}", error)
                        }
                    } else if (it is FirebaseException) {
                        NotificationBannerFragment.show(
                            this,
                            code = it.code,
                            title = viewModel.string(R.string.error_unexpected),
                            message = viewModel.string(R.string.error_unexpected_send),
                            error = it.throwable
                        )
                        Analytics.onError("error_${it.code}", it.throwable)
                    }
                }
            }
        }
    }

    private fun initSplash() {
        viewModel.lockedLive.observe(this) {
            if (!it) {
                initGraph()
            }
        }
    }

    private fun initGraph() {
        if (!initialized) {
            initialized = true
            val navInflater = navController.navInflater
            val navGraph = navInflater.inflate(R.navigation.nav_main)
            navController.graph = navGraph
        }
    }

    private fun initNewVersionAvailable() {
        if (viewModel.appConfig.isNewVersionAvailable()) {
            NewVersionBannerFragment.show(this)
        }
    }

    private fun initSelectPlanBanner() {
        viewModel.canSyncLive.observe(this) {
            if (!it) {
                viewModel.onShowSyncRestriction()
            }
        }
    }

    private fun initSignIn() {
        viewModel.requestSignInLive.observe(this) { request ->
            if (request == null) return@observe

            val webAuth = request.webAuth
            val webAuthToken = request.webAuthToken
            val withWarning = request.withWarning
            val userState = viewModel.userState
            val appConfig = viewModel.appConfig
            val appState = viewModel.appState
            val internetState = viewModel.internetState
            internetState.withInternet({
                userState.signOutInProgress.setValue(false)
                val onSignInCallback: () -> Unit = {
                    if (webAuthToken != null) {
                        auth.signIn(webAuthToken) { authData, th ->
                            if (authData != null) {
                                val user = User().apply {
                                    email = authData.email
                                    photoUrl = authData.photoUrl
                                    firebaseId = authData.firebaseId
                                    providerId = authData.providerId
                                    displayName = authData.displayName
                                }
                                viewModel.onSignIn(user)
                                if (!viewModel.isLocked() && appConfig.suggestSetPassCodeOnSignIn()) {
                                    navController.navigateSafe(R.id.action_set_passcode)
                                }
                            } else {
                                appState.setLoadingState(DataLoadingState.Error(code = "sign_in_token", throwable = th))
                            }
                        }
                    } else if (webAuth && IntentUtils.open(this, BuildConfig.appAuthLink)) {
                        //
                    } else {
                        auth.signIn(this) { authData, th ->
                            if (authData != null) {
                                val user = User().apply {
                                    email = authData.email
                                    photoUrl = authData.photoUrl
                                    firebaseId = authData.firebaseId
                                    providerId = authData.providerId
                                    displayName = authData.displayName
                                }
                                viewModel.onSignIn(user)
                                if (!viewModel.isLocked() && appConfig.suggestSetPassCodeOnSignIn()) {
                                    navController.navigateSafe(R.id.action_set_passcode)
                                }
                            } else {
                                appState.setLoadingState(DataLoadingState.Error(code = "sign_in_web", throwable = th))
                            }
                        }
                    }
                }
                viewModel.dialogState.showConfirm(ConfirmDialogData(
                    iconRes = request.iconRes,
                    title = viewModel.string(request.titleRes),
                    description = viewModel.string(request.descriptionRes),
                    confirmActionTextRes = request.actionTextRes,
                    onConfirmed = { onSignInCallback.invoke() },
                    autoConfirm = { !withWarning }
                ))
            })
        }
    }

    private fun initSignOut() {
        viewModel.requestSignOutLive.observe(this) { request ->
            val withConfirm = request.withConfirm
            val userState = viewModel.userState
            val appState = viewModel.appState
            val invoke: () -> Unit = {
                userState.signOutInProgress.setValue(true)
                viewModel.firebaseDaoHelper.waitForPendingWrites {
                    auth.signOut(this) { _, th ->
                        if (th != null) {
                            appState.setLoadingState(DataLoadingState.Error(code = "sign_out", throwable = th))
                        } else {
                            viewModel.onSignOut(userState.user.requireValue())
                        }
                    }
                }
            }
            viewModel.dialogState.showConfirm(ConfirmDialogData(
                iconRes = R.drawable.ic_attention,
                title = viewModel.string(R.string.account_dialog_sign_out_title),
                description = viewModel.string(R.string.account_dialog_sign_out_message),
                confirmActionTextRes = R.string.account_button_sign_out,
                onConfirmed = { invoke.invoke() },
                autoConfirm = { !withConfirm }
            ))
        }
    }

    private fun initShareAppRequest() {
        viewModel.requestShareAppLive.observe(this) {
            viewModel.onShareApp()
        }
    }

    private fun onBarcodeResult(result: QRResult) {
        viewModel.dialogState.onBarcodeResult(result)
    }

}