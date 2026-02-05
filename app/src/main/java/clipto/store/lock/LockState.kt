package clipto.store.lock

import android.app.Application
import clipto.config.IAppConfig
import clipto.presentation.lockscreen.FingerprintUtils
import clipto.store.StoreObject
import clipto.store.StoreState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockState @Inject constructor(
    private val app: Application,
    appConfig: IAppConfig
) : StoreState(appConfig) {

    val locked by lazy {
        StoreObject<Boolean>("locked", onChanged = { _, newValue ->
            val unlockTime = if (newValue!!) null else System.currentTimeMillis()
            lastUnlockTime.setValue(unlockTime)
        })
    }
    val lastUnlockTime by lazy { StoreObject<Long>(id = "last_unlock_time") }

    val isFingerprintAvailable by lazy { StoreObject("is_fingerprint_available", FingerprintUtils.isFingerprintAvailable(app)) }

}