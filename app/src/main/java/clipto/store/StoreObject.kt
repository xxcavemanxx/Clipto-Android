package clipto.store

import android.os.Looper
import android.view.View
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import androidx.core.view.doOnNextLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import clipto.common.extensions.disposeSilently
import clipto.common.presentation.mvvm.lifecycle.SingleLiveData
import clipto.extensions.log
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.schedulers.Schedulers

class StoreObject<T> constructor(
    val id: String = "unknown",
    private val initialValue: T? = null,
    private val liveDataStrategy: LiveDataStrategy = LiveDataStrategy.DEFAULT,
    private val liveDataChangeStrategy: LiveDataChangeStrategy = LiveDataChangeStrategy.POST,
    private val onChanged: (prevValue: T?, newValue: T?) -> Unit = { _, _ -> }
) {

    enum class LiveDataStrategy {
        DEFAULT, SINGLE_CONSUMER
    }

    enum class LiveDataChangeStrategy {
        SET {
            override fun <T> setValue(liveData: MutableLiveData<T>, value: T?) {
                liveData.value = value
            }
        },
        POST {
            override fun <T> setValue(liveData: MutableLiveData<T>, value: T?) {
                liveData.postValue(value)
            }
        },
        AUTO {
            override fun <T> setValue(liveData: MutableLiveData<T>, value: T?) {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    liveData.value = value
                } else {
                    liveData.postValue(value)
                }
            }
        }
        ;

        abstract fun <T> setValue(liveData: MutableLiveData<T>, value: T?)
    }

    private var hasInitialValue: Boolean = initialValue !== null
    private var lastValue: T? = initialValue

    private val valueProcessor = lazy {
        val processor = BehaviorProcessor.create<StoreObjectSnapshot<T>>()
        if (hasInitialValue) {
            processor.onNext(StoreObjectSnapshot(lastValue))
        }
        processor
    }

    private val liveData = lazy {
        val live = when (liveDataStrategy) {
            LiveDataStrategy.DEFAULT -> MutableLiveData<T>()
            LiveDataStrategy.SINGLE_CONSUMER -> SingleLiveData()
        }
        if (hasInitialValue) {
            live.postValue(lastValue)
        }
        live
    }

    fun setValue(value: T?, force: Boolean = false, notifyChanged: Boolean = true): Boolean {
        val isChanged = force || lastValue != value
        hasInitialValue = true
        val prevValue = lastValue
        lastValue = value
        if (isChanged && notifyChanged) {
//            L.log(this, "setValue :: id={}, prev={}, new={}", id, prevValue, value)
            onChanged.invoke(prevValue, value)
            if (valueProcessor.isInitialized()) {
                valueProcessor.value.onNext(StoreObjectSnapshot(value))
            }
            if (liveData.isInitialized()) {
                liveDataChangeStrategy.setValue(liveData.value, value)
            }
        }
        return isChanged
    }

    fun getLiveChanges(): Flowable<StoreObjectSnapshot<T>> = valueProcessor.value.share().onBackpressureLatest()

    fun updateValue(
        force: Boolean = false,
        notifyChanged: Boolean = true,
        valueProvider: (current: T?) -> T?
    ) = setValue(
        valueProvider.invoke(lastValue),
        notifyChanged = notifyChanged,
        force = force
    )

    fun clearValue(force: Boolean = false, notifyChanged: Boolean = true) = setValue(initialValue, force = force, notifyChanged = notifyChanged)
    fun getMutableLiveData(): MutableLiveData<T> = liveData.value
    fun getLiveData(): LiveData<T> = liveData.value
    fun requireValue(): T = lastValue!!
    fun getValue(): T? = lastValue
    fun consumeValue(): T? {
        val lastValueRef = lastValue
        lastValue = null
        return lastValueRef
    }

    fun isNull(): Boolean = lastValue == null
    fun isNotNull(): Boolean = !isNull()
    fun clearAndUnbind(owner: LifecycleOwner) {
        clearValue(notifyChanged = false)
        getLiveData().removeObservers(owner)
    }

    fun getLiveChanges(onNext: (StoreObjectSnapshot<T>) -> Unit, onError: (t: Throwable) -> Unit = {}, subscribeOn: Scheduler = Schedulers.single()): Disposable {
        return getLiveChanges()
            .subscribeOn(subscribeOn)
            .subscribe(onNext, onError)
    }

    fun getLiveChanges(view: View, onNext: (T) -> Unit) {
        view.doOnAttach {
            log("getLiveChanges :: doOnAttach")
            val disposable = getLiveChanges()
                .filter { it.isNotNull() }
                .map { it.requireValue() }
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { log("getLiveChanges :: {} - {}", it, Thread.currentThread()) }
                .subscribe(onNext) { log("getLiveChanges :: error :: {}", it.message) }
            view.doOnDetach {
                log("getLiveChanges :: doOnDetach")
                disposable.disposeSilently()
                view.doOnNextLayout {
                    getLiveChanges(view, onNext)
                }
            }
        }
    }
}