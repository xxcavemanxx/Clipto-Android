package clipto

import android.app.Activity
import android.app.Application
import clipto.common.extensions.disposeSilently
import clipto.common.presentation.mvvm.base.SimpleLifecycleCallbacks
import clipto.presentation.lockscreen.LockActivity
import clipto.repository.ISecurityRepository
import clipto.store.app.AppState
import clipto.store.lock.LockState
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockManager @Inject constructor(
    val app: Application,
    private val appState: AppState,
    private val lockState: LockState,
    private val securityRepository: ISecurityRepository
) {

    private val activitiesCount = AtomicInteger(0)
    private var changingConfigurations = false
    private var disposable: Disposable? = null

    init {
        app.registerActivityLifecycleCallbacks(object : SimpleLifecycleCallbacks {

            private val activityQueue = mutableListOf<Class<Activity>>()

            override fun onActivityStarted(activity: Activity) {
                activityQueue.add(activity.javaClass)
                appState.lastActivity.setValue(activity.javaClass)
                if (activity is AppContainer || activity is LockActivity) {
                    if (activitiesCount.incrementAndGet() == 1 && !changingConfigurations) {
                        disposable.disposeSilently()
                        disposable = onForegroundEntered()
                    }
                }
            }

            override fun onActivityStopped(activity: Activity) {
                activityQueue.removeLastOrNull()
                appState.lastActivity.setValue(activityQueue.lastOrNull())
                if (activity is AppContainer || activity is LockActivity) {
                    changingConfigurations = activity.isChangingConfigurations
                    if (activitiesCount.decrementAndGet() == 0 && !changingConfigurations) {
                        disposable.disposeSilently()
                        disposable = onBackgroundEntered()
                    }
                }
            }
        })
    }

    private fun onForegroundEntered(): Disposable? {
        return if (appState.getSettings().isLocked()) {
            securityRepository.isLocked()
                .onErrorResumeNext(securityRepository.unlock().toSingle { false })
                .subscribeOn(Schedulers.computation())
                .observeOn(appState.getViewScheduler())
                .subscribe({ if (it) showLockScreen() }, {})
        } else {
            lockState.locked.setValue(false)
            null
        }
    }

    private fun onBackgroundEntered(): Disposable? {
        return if (appState.getSettings().isLocked()) {
            securityRepository.lock()
                .subscribeOn(Schedulers.computation())
                .subscribe({}, {})
        } else {
            null
        }
    }

    private fun showLockScreen() {
        LockActivity.start(app)
    }
}