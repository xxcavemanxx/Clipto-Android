package clipto.presentation.settings

import android.app.Application
import clipto.action.SaveFilterAction
import clipto.action.SaveSettingsAction
import clipto.backup.BackupManager
import clipto.common.presentation.mvvm.RxViewModel
import clipto.config.IAppConfig
import clipto.domain.Theme
import clipto.domain.Filter
import clipto.presentation.common.dialog.DialogState
import clipto.store.app.AppState
import clipto.store.clipboard.ClipboardState
import clipto.store.internet.InternetState
import clipto.store.main.MainState
import clipto.store.user.UserState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
        app: Application,
        val appState: AppState,
        val mainState: MainState,
        val appConfig: IAppConfig,
        val userState: UserState,
        val dialogState: DialogState,
        val internetState: InternetState,
        val backupManager: BackupManager,
        val clipboardState: ClipboardState,
        private val saveFilterAction: SaveFilterAction,
        private val saveSettingsAction: SaveSettingsAction
) : RxViewModel(app) {

    var isClipboardFilterChanged = false

    val settingsLive = appState.settings.getLiveData()

    override fun doClear() {
        super.doClear()
        if (isClipboardFilterChanged) {
            saveFilterAction.execute(appState.getFilterByClipboard())
        }
        onSaveSettings()
    }

    fun isAuthorized() = userState.isAuthorized()

    fun onChangeTheme(theme: Theme) {
        appState.getSettings().theme = theme.id
        if (appState.theme.setValue(theme)) {
            clipboardState.refreshNotification()
            appState.requestRestart()
        }
    }

    fun onSignIn(webAuth: Boolean = false, withWarning: Boolean = false, callback: () -> Unit = {}) {
        userState.signIn(UserState.SignInRequest(webAuth = webAuth, withWarning = withWarning))
                .observeOn(getViewScheduler())
                .subscribeBy { callback.invoke() }
    }

    fun onApplyLastFilter() = mainState.requestApplyFilter(appState.getFilterByLast(), force = true, closeNavigation = false)

    fun onSaveFilter(filter: Filter, withReload: Boolean = false) = saveFilterAction.execute(filter, reload = withReload)

    fun onSaveSettings() = saveSettingsAction.execute()

}