package clipto.common.presentation.mvvm

import android.app.Application
import android.os.Looper
import androidx.annotation.CallSuper
import clipto.common.extensions.disposeSilently
import io.reactivex.*
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.internal.subscriptions.SubscriptionHelper
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Subscription
import java.util.concurrent.atomic.AtomicReference

abstract class RxViewModel(app: Application) : ViewModel(app) {

    companion object {
        var defaultViewScheduler = lazy {
            AndroidSchedulers.from(Looper.getMainLooper(), true).apply {
                RxAndroidPlugins.setInitMainThreadSchedulerHandler { this }
                RxAndroidPlugins.setMainThreadSchedulerHandler { this }
            }
        }
        var defaultBackgroundScheduler = lazy { Schedulers.single() }
    }

    private val disposableList = CompositeDisposable()
    private val disposableMap = mutableMapOf<String, Disposable>()

    open fun getViewScheduler(): Scheduler = defaultViewScheduler.value
    open fun getBackgroundScheduler(): Scheduler = defaultBackgroundScheduler.value
    fun isMainThread(): Boolean = Looper.getMainLooper().thread === Thread.currentThread()

    @CallSuper
    override fun doClear() {
        runCatching {
            disposableList.clear()
            disposableMap.clear()
        }
    }

    fun onMain(func: () -> Unit) = getViewScheduler().scheduleDirect(func)
    fun onBackground(func: () -> Unit) = getBackgroundScheduler().scheduleDirect(func)

    fun <T> Single<T>.subscribeBy(tag: String? = null, subscribeOn: Scheduler, onSuccess: (T) -> Unit): Disposable {
        return withDefaults(tag, this, subscribeOn)
            .subscribe(onSuccess) {}
            .checked()
    }

    fun <T> Single<T>.subscribeBy(tag: String? = null, onSuccess: (T) -> Unit, onError: (t: Throwable) -> Unit = {}, loadingStateProvider: LoadingStateProvider? = null): Disposable {
        return withDefaults(tag, this)
            .doOnSubscribe { loadingStateProvider?.setLoadingState() }
            .doFinally { loadingStateProvider?.setLoadedState() }
            .subscribe(onSuccess, onError)
            .checked()
    }

    fun <T> Single<T>.subscribeBy(tag: String? = null, loadingStateProvider: LoadingStateProvider? = null, onSuccess: (T) -> Unit = {}): Disposable {
        return subscribeBy(tag, onSuccess, {}, loadingStateProvider)
    }

    fun <T> Maybe<T>.subscribeBy(tag: String? = null, onSuccess: (T) -> Unit, onError: (t: Throwable) -> Unit = {}): Disposable {
        return withDefaults(tag, this)
            .subscribe(onSuccess, onError)
            .checked()
    }

    fun <T> Maybe<T>.subscribeBy(tag: String? = null, onSuccess: (T) -> Unit = {}): Disposable {
        return subscribeBy(tag, onSuccess) {}
    }

    fun <T> Observable<T>.subscribeBy(tag: String? = null, onNext: (T) -> Unit, onError: (t: Throwable) -> Unit = {}): Disposable {
        return withDefaults(tag, this)
            .subscribe(onNext, onError)
            .checked()
    }

    fun <T> Observable<T>.subscribeBy(tag: String? = null, onSuccess: (T) -> Unit): Disposable {
        return subscribeBy(tag, onSuccess) {}
    }

    fun Completable.subscribeBy(tag: String? = null, loadingStateProvider: LoadingStateProvider? = null, onComplete: () -> Unit, onError: (t: Throwable) -> Unit = {}) {
        withDefaults(tag, this)
            .doOnSubscribe { loadingStateProvider?.setLoadingState() }
            .doFinally { loadingStateProvider?.setLoadedState() }
            .subscribe(onComplete, onError)
            .checked()
    }

    fun Completable.subscribeBy(tag: String? = null, loadingStateProvider: LoadingStateProvider? = null, onComplete: () -> Unit = {}) {
        subscribeBy(tag, loadingStateProvider, onComplete) {}
    }

    fun <T> Flowable<T>.subscribeBy(tag: String? = null, onNext: (T) -> Unit, onError: (t: Throwable) -> Unit = {}): Disposable {
        return withDefaults(tag, this)
            .subscribe(onNext, onError)
            .checked()
    }

    fun <T> Flowable<T>.subscribeBy(tag: String? = null, onNext: (T) -> Unit): Disposable = subscribeBy(tag, onNext, {})

    private fun <T> withDefaults(tag: String?, single: Single<T>, subscribeOn: Scheduler = getBackgroundScheduler()): Single<T> = single
        .subscribeOn(subscribeOn)
        .doOnSubscribe { onSubscribe(tag, it) }
        .doOnError { onError(it) }

    private fun <T> withDefaults(tag: String?, observable: Observable<T>): Observable<T> = observable
        .subscribeOn(getBackgroundScheduler())
        .doOnSubscribe { onSubscribe(tag, it) }
        .doOnError { onError(it) }

    private fun <T> withDefaults(tag: String? = null, flowable: Flowable<T>): Flowable<T> = flowable
        .subscribeOn(getBackgroundScheduler())
        .doOnSubscribe { onSubscribe(tag, it) }
        .doOnError { onError(it) }

    private fun withDefaults(tag: String?, completable: Completable): Completable = completable
        .subscribeOn(getBackgroundScheduler())
        .doOnSubscribe { onSubscribe(tag, it) }
        .doOnError { onError(it) }

    private fun <T> withDefaults(tag: String?, maybe: Maybe<T>): Maybe<T> = maybe
        .subscribeOn(getBackgroundScheduler())
        .doOnSubscribe { onSubscribe(tag, it) }
        .doOnError { onError(it) }

    private fun Disposable.checked(): Disposable {
        return this
    }

    private fun onSubscribe(tag: String?, disposable: Disposable) {
        if (tag != null) {
            log("onSubscribe :: {} - {}", tag, disposableMap[tag])
            disposableMap[tag].disposeSilently()
            disposableMap[tag] = disposable
        }
        disposableList.add(disposable)
    }

    private fun onSubscribe(tag: String?, subscription: Subscription) {
        onSubscribe(tag, DisposableSubscription(subscription))
    }

    private fun onError(th: Throwable) {
        log("onError", th.message)
        th.printStackTrace()
    }

    private class DisposableSubscription(subscription: Subscription) : Disposable {

        private val reference = AtomicReference(subscription)

        override fun dispose() {
            SubscriptionHelper.cancel(reference)
        }

        override fun isDisposed(): Boolean = reference.get() == SubscriptionHelper.CANCELLED

    }
}