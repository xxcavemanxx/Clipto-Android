package clipto.dao.firebase

import clipto.common.logging.L
import clipto.config.IAppConfig
import clipto.store.internet.InternetState
import com.google.firebase.functions.FirebaseFunctions
import io.reactivex.Maybe
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FunctionsFunctionsHelper @Inject constructor(
    val internetState: InternetState,
    val appConfig: IAppConfig
) {

    private val execCallableOnceCache = mutableMapOf<String, Any?>()

    fun <T> exec(name: String, params: Any? = null): Maybe<T> = Maybe.create { emitter ->
        if (internetState.isConnected()) {
            log("exec :: started {} -> {}", name, params)
            val callable = FirebaseFunctions.getInstance().getHttpsCallable(name)
            callable.setTimeout(appConfig.apiReadTimeout(), TimeUnit.MILLISECONDS)
            callable.call(params)
                .addOnSuccessListener {
                    log("exec :: finished {} -> {}", name, params)
                    if (!emitter.isDisposed) {
                        emitter.onSuccess(it.data as T)
                    }
                }
                .addOnFailureListener {
                    log("exec :: error {} -> {}", name, params)
                    if (!emitter.isDisposed) {
                        emitter.onError(it)
                    }
                }
                .addOnCompleteListener {
                    log("exec :: completed {} -> {}", name, params)
                    emitter.onComplete()
                }
        } else {
            emitter.onComplete()
        }
    }

    fun <T> execOnce(id: String, name: String, params: Any? = null): Maybe<T> = Maybe.create { emitter ->
        when {
            execCallableOnceCache.contains(id) -> {
                log("execOnce :: found in cache: {}", id)
                emitter.onSuccess(execCallableOnceCache[id] as T)
                emitter.onComplete()
            }
            internetState.isConnected() -> {
                log("execOnce :: started {} -> {}", name, params)
                val callable = FirebaseFunctions.getInstance().getHttpsCallable(name)
                callable.setTimeout(appConfig.apiReadTimeout(), TimeUnit.MILLISECONDS)
                callable.call(params)
                    .addOnSuccessListener {
                        log("execOnce :: finished {} -> {}", name, params)
                        execCallableOnceCache[id] = it.data
                        if (!emitter.isDisposed) {
                            emitter.onSuccess(it.data as T)
                        }
                    }
                    .addOnFailureListener {
                        log("execOnce :: error {} -> {}", name, params)
                        if (it is TimeoutException) {
                            execCallableOnceCache[id] = emptyMap<String, Any?>()
                        }
                        if (!emitter.isDisposed) {
                            emitter.onError(it)
                        }
                    }
                    .addOnCompleteListener {
                        log("execOnce :: completed {} -> {}", name, params)
                        emitter.onComplete()
                    }
            }
            else -> {
                emitter.onComplete()
            }
        }
    }

    private fun log(message: String, vararg params: Any?) = L.log(this, message, *params)

}