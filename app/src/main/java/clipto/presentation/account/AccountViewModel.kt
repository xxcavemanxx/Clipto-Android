package clipto.presentation.account

import android.app.Application
import clipto.common.presentation.mvvm.RxViewModel
import clipto.config.IAppConfig
import clipto.repository.IClipRepository
import clipto.store.app.AppState
import clipto.store.internet.InternetState
import clipto.store.user.UserState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
        app: Application,
        val appConfig: IAppConfig,
        private val appState: AppState,
        private val userState: UserState,
        private val internetState: InternetState,
        private val clipRepository: IClipRepository
) : RxViewModel(app) {

    val user = userState.user.getLiveData()
    val license = userState.license.getLiveData()
    val invitations = userState.invitations.getLiveData()

    fun getSettings() = appState.getSettings()

    fun onSyncAll() = clipRepository.syncAll()
    fun onSignOut() = internetState.withInternet({ userState.requestSignOut(withConfirm = true) })
    fun onShareApp() = internetState.withInternet({ userState.requestShareApp() })

}