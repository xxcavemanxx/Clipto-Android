package clipto.action

import clipto.common.extensions.disposeSilently
import clipto.config.IAppConfig
import clipto.store.app.AppState
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

abstract class CompletableAction<C : ActionContext> : Action() {

    @Inject
    lateinit var appConfig: IAppConfig

    @Inject
    lateinit var appState: AppState

    private var disposable: Disposable? = null

    private var inProgress = false

    protected open fun canExecute(context: C): Boolean = true

    protected fun isInProgress(): Boolean = inProgress

    fun execute(context: C, callback: () -> Unit = {}) {
        if (!canExecute(context)) {
            log("skip action due to restrictions")
            return
        }
        disposable?.takeIf { context.disposeRunning }.disposeSilently()
        disposable = Single.fromCallable { appConfig.getRxTimeout() }
            .flatMapCompletable { initTimeout ->
                if (context.withTimeout) {
                    create(context).timeout(initTimeout, TimeUnit.MILLISECONDS)
                } else {
                    create(context)
                }
            }
            .doOnSubscribe { log("action {} started", name) }
            .doOnComplete { log("action {} finished", name) }
            .doOnError { log("action {} error {}", name, it) }
            .doOnSubscribe { if (context.showLoadingIndicator) appState.setLoadingState() }
            .doFinally { if (context.showLoadingIndicator) appState.setLoadedState() }
            .doOnSubscribe { inProgress = true }
            .doFinally { inProgress = false }
            .subscribeOn(subscribeOn())
            .subscribe({ callback.invoke() }, { callback.invoke() })
    }

    protected open fun subscribeOn() = appState.getBackgroundScheduler()

    protected abstract fun create(context: C): Completable

    companion object {
        val SCHEDULER_ACTION_SINGLE by lazy { Schedulers.from(Executors.newSingleThreadExecutor()) }
    }

}