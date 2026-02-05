package clipto.store.analytics

import android.app.Application
import android.os.Build
import clipto.analytics.FirebaseAnalyticsTracker
import clipto.common.analytics.A
import clipto.config.IAppConfig
import clipto.domain.User
import clipto.store.StoreState
import clipto.store.app.AppState
import clipto.store.user.UserState
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsState @Inject constructor(
    appConfig: IAppConfig,
    private val app: Application
) : StoreState(appConfig) {

    private val analyticsRef = lazy { FirebaseAnalytics.getInstance(app) }

    fun onUpdateUserState(userState: UserState) {
        val user = userState.user.getValue()
        if (user != null && user != User.NULL) {
            runCatching {
                val analytics = analyticsRef.value
                A.init(FirebaseAnalyticsTracker(analytics) { appConfig.firebaseAnalyticsCollectionEnabled() && userState.isAuthorized() })
                analytics.setUserId(user.firebaseId)
                analytics.setUserProperty("license", user.license.code)
                analytics.setUserProperty("device_brand", Build.BRAND.lowercase())
                analytics.setUserProperty("device_model", Build.MODEL.lowercase())
                analytics.setUserProperty("invited_count", "${user.invitedCount}")
                analytics.setUserProperty("device_sdk", "${Build.VERSION.SDK_INT}")
                analytics.setUserProperty("is_authorized", if (user.isAuthorized()) "yes" else "no")
                analytics.setUserProperty("sync_limit", userState.getSyncLimit().toString())

                // crashlytics
                user.firebaseId?.let { FirebaseCrashlytics.getInstance().setUserId(it) }

                log("AnalyticsState :: onUpdateUserState :: {}", appConfig.firebaseAnalyticsCollectionEnabled() && userState.isAuthorized())
            }
        }
    }

    fun onUpdateAppState(appState: AppState) {
        runCatching {
            if (analyticsRef.isInitialized()) {
                val filters = appState.getFilters()
                val analytics = analyticsRef.value
                analytics.setUserProperty("all_notes_count", averageValue(filters.all.notesCount))
                analytics.setUserProperty("clipboard_notes_count", averageValue(filters.clipboard.notesCount))
                log("AnalyticsState :: onUpdateAppState")
            }
        }
    }

    private fun averageValue(value: Long): String {
        return when {
            value <= 100 -> "< 100"
            value <= 200 -> "< 200"
            value <= 300 -> "< 300"
            value <= 500 -> "< 500"
            value <= 1000 -> "< 1000"
            value <= 3000 -> "< 3000"
            value <= 5000 -> "< 5000"
            value <= 10000 -> "< 10000"
            value <= 20000 -> "< 20000"
            else -> "> 20000"
        }
    }

}