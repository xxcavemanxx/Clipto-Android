package clipto.common.presentation.mvvm

import android.app.Application
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import clipto.common.extensions.showToast
import clipto.common.logging.L
import clipto.common.presentation.mvvm.lifecycle.SingleLiveData
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

abstract class ViewModel(val app: Application) : AndroidViewModel(app) {

    private val initialized = AtomicBoolean(false)
    private val subscribers = AtomicInteger()
    private val clearRequested = AtomicBoolean()

    val dismissLive by lazy { SingleLiveData<Boolean>() }

    companion object {
        private val activeViewQueue = mutableListOf<Class<out Any>>()
        private val activeViewLive = MutableLiveData<Class<out Any>>()
        fun addViewToQueue(any: Any) {
            runCatching {
                val clazz = any::class.java
                activeViewQueue.add(clazz)
                activeViewLive.postValue(clazz)
            }
        }

        fun removeViewFromQueue(any: Any) {
            runCatching {
                val clazz = any::class.java
                val lastIndexOf = activeViewQueue.lastIndexOf(clazz)
                if (lastIndexOf != -1) {
                    activeViewQueue.removeAt(lastIndexOf)
                    activeViewLive.postValue(activeViewQueue.lastOrNull())
                }
            }
        }
    }

    internal fun init() {
        if (initialized.compareAndSet(false, true)) {
            onCreated()
        }
    }

    private fun onCreated() {
        log("onCreated")
        doCreate()
    }

    internal fun onSubscribed(subscriber: Any) {
        val value = subscribers.incrementAndGet()
        log("onSubscribed: {} -> {}", subscriber, value)
        doSubscribe()
    }

    internal fun onUnsubscribed(subscriber: Any) {
        val value = subscribers.decrementAndGet()
        doUnsubscribe()
        if (clearRequested.get()) {
            onCleared()
        }
        log("onUnsubscribed: {} -> {}", subscriber, value)
    }

    final override fun onCleared() {
        if (subscribers.get() == 0) {
            try {
                log("onCleared started")
                doClear()
            } finally {
                initialized.set(false)
                clearRequested.set(false)
                log("onCleared finished")
            }
        } else {
            clearRequested.set(true)
            log("has active subscribers: {}", subscribers.get())
        }
    }

    fun getActiveViewLive(): LiveData<Class<out Any>> = activeViewLive
    protected fun isDismissed(): Boolean = dismissLive.value == true
    fun dismiss() = dismissLive.postValue(true)
    protected open fun doCreate() = Unit
    protected open fun doSubscribe() = Unit
    protected open fun doUnsubscribe() = Unit
    protected open fun doClear() = Unit

    fun log(message: String, vararg params: Any?) = L.log(this, message, *params)
    fun string(@StringRes id: Int, vararg args: Any?): String = app.getString(id, *args)
    fun string(@StringRes id: Int): String = app.getString(id)

    fun quantityString(@PluralsRes id: Int, count: Int, vararg args: Any?): String = app.resources.getQuantityString(id, count, *args)
    fun quantityString(@PluralsRes id: Int, count: Int): String = app.resources.getQuantityString(id, count)

    fun showToast(message: CharSequence) = app.showToast(message)

}