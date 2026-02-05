package clipto.presentation.auth

import android.app.Application
import clipto.store.app.AppState
import com.wb.clipboard.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AuthModule {

    @Provides
    @Singleton
    fun auth(app: Application, appState: AppState): IAuth = Auth(
            app = app,
            theme = { appState.getTheme().authThemeId },
            logo = 0,
            privacyUrl = BuildConfig.privacyPolicyUrl,
            tosUrl = BuildConfig.tosUrl
    )

}