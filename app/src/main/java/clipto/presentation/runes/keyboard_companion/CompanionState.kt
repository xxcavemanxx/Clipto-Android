package clipto.presentation.runes.keyboard_companion

import android.app.Application
import androidx.navigation.NavDeepLinkBuilder
import clipto.common.extensions.canDrawOverlayViews
import clipto.common.extensions.isAccessibilityEnabled
import clipto.config.IAppConfig
import clipto.domain.IRune
import clipto.presentation.runes.RuneSettingsViewModel
import clipto.store.StoreObject
import clipto.store.StoreState
import clipto.store.app.AppState
import com.wb.clipboard.R
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanionState @Inject constructor(
    appConfig: IAppConfig,
    private val app: Application,
    private val appState: AppState,
    private val notificationManager: CompanionNotificationManager
) : StoreState(appConfig) {

    val mode = StoreObject("mode", initialValue = CompanionMode.HIDDEN)
    val tileActivated = StoreObject("isTileActivated", false)

    private val ifAvailableInProgress = AtomicBoolean(false)
    private var ifAvailableLastCheck = 0L

    fun isUserHidden(): Boolean = appState.getSettings().texpanderUserHidden
    fun isHidden(): Boolean = isUserHidden() || mode.requireValue() == CompanionMode.HIDDEN
    fun isUserSearch(): Boolean = mode.requireValue() == CompanionMode.USER_SEARCH
    fun isDetached(): Boolean = mode.requireValue() == CompanionMode.DETACHED
    fun isAvailable() = isEnabled() && app.isAccessibilityEnabled(CompanionService::class.java) && app.canDrawOverlayViews()
    fun isEnabled() = appState.getSettings().texpanderRuneEnabled

    val isAvailable by lazy {
        StoreObject(
            id = "is_available",
            initialValue = isAvailable()
        )
    }

    fun requestShowNotification() {
        if (tileActivated.requireValue() || !appState.getSettings().texpanderRuneEnabled) {
            notificationManager.hide()
        } else {
            notificationManager.show()
        }
    }

    fun requestHideNotification() {
        runCatching { notificationManager.hide() }
    }

    fun ifAvailable(requestActivationIfDisabled: Boolean = false, callback: () -> Unit) {
        if (!isEnabled() && !requestActivationIfDisabled) {
            log("disabled and will be skipped")
            return
        }
        val isAvailable = isAvailable.requireValue() || isAvailable()
        log("isAvailable :: {}", isAvailable)
        if (isAvailable) {
            callback.invoke()
        } else if (ifAvailableInProgress.compareAndSet(false, true)) {
            log("request activation")
            ifAvailableLastCheck = System.currentTimeMillis()
            runCatching {
                NavDeepLinkBuilder(app)
                    .setGraph(R.navigation.nav_main)
                    .setDestination(R.id.fragment_rune_settings)
                    .setArguments(RuneSettingsViewModel.withArgs(IRune.RUNE_TEXPANDER))
                    .createPendingIntent()
                    .send()
            }
        } else {
            if (System.currentTimeMillis() - ifAvailableLastCheck >= TimeUnit.MINUTES.toMillis(10)) {
                ifAvailableInProgress.set(false)
            }
        }
    }

}