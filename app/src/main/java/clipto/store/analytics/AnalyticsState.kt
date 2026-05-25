package clipto.store.analytics

import android.app.Application
import clipto.config.IAppConfig
import clipto.store.StoreState
import clipto.store.app.AppState
import clipto.store.user.UserState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsState @Inject constructor(
    appConfig: IAppConfig,
    private val app: Application
) : StoreState(appConfig) {

    fun onUpdateUserState(userState: UserState) {
        // Mocked: Firebase analytics has been removed.
    }

    fun onUpdateAppState(appState: AppState) {
        // Mocked: Firebase analytics has been removed.
    }
}