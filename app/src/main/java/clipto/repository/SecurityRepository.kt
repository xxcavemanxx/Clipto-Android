package clipto.repository

import clipto.config.IAppConfig
import clipto.dao.objectbox.SettingsBoxDao
import clipto.extensions.log
import clipto.store.lock.LockState
import clipto.utils.EncryptUtils
import com.wb.clipboard.BuildConfig
import io.reactivex.Completable
import io.reactivex.Single
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

@Singleton
class SecurityRepository @Inject constructor(
    private val settingsBoxDao: SettingsBoxDao,
    private val appConfig: IAppConfig,
    private val lockState: LockState
) : ISecurityRepository {

    private val unlocked = AtomicBoolean(false)

    override fun lock(): Completable = Completable.fromAction {
        settingsBoxDao.update { settings ->
            if (unlocked.get() && settings.isLocked()) {
                log("lock")
                settings.lastSessionDate = Date()
                true
            } else {
                false
            }
        }
    }

    override fun unlock(): Completable = Completable.fromAction {
        log("unlock")
        lockState.locked.setValue(false)
        unlocked.set(true)
    }

    override fun isLocked(): Single<Boolean> = Single.fromCallable {
        val settings = settingsBoxDao.get()
        var isLocked = settings.isLocked()
        if (isLocked) {
            val lastDate = settings.lastSessionDate ?: Date()
            val diff = (System.currentTimeMillis() - lastDate.time).absoluteValue
            val autoLockInterval = TimeUnit.MINUTES.toMillis(appConfig.autoLockInMinutes().toLong())
            log("isLocked:: check if is locked (by date): diff={}, autoLockInterval={}", diff, autoLockInterval)
            isLocked = diff > autoLockInterval || lockState.lastUnlockTime.isNull()
        }
        log("isLocked:: check if is locked: {}", isLocked)
        lockState.locked.setValue(isLocked)
        unlocked.set(!unlocked.get())
        isLocked
    }

    override fun isPassCodeSet(): Single<Boolean> = Single.fromCallable {
        settingsBoxDao.get().isLocked()
    }

    override fun savePassCode(passCode: String): Completable = Completable.fromAction {
        settingsBoxDao.update { settings ->
            if (passCode.length == BuildConfig.pinCodeLength) {
                settings.passcode = EncryptUtils.encryptDes(passCode)
                unlocked.set(true)
            } else {
                settings.passcode = null
                unlocked.set(false)
            }
            true
        }
    }

    override fun checkPassCode(passCode: String): Single<Boolean> = Single.fromCallable {
        val saved = settingsBoxDao.get().passcode?.trim()
        val encoded = EncryptUtils.encryptDes(passCode).trim()
        saved == encoded
    }

    override fun isFingerprintEnabled(): Single<Boolean> = Single.fromCallable {
        settingsBoxDao.get().useFingerprint
    }

    override fun setFingerprintEnabled(enabled: Boolean): Completable = Completable.fromAction {
        settingsBoxDao.update { settings ->
            settings.useFingerprint = enabled
            true
        }
    }
}