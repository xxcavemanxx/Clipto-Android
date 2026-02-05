package clipto.dao

import android.app.Application
import clipto.config.IAppConfig
import clipto.extensions.log
import io.objectbox.BoxStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TxHelper @Inject constructor(
    val appConfig: IAppConfig,
    val boxStore: BoxStore,
    val app: Application
) {
    fun <T> inTx(title: String = "", callable: () -> T): T = boxStore.callInTx {
        log("transaction started: {}", title)
        val result = callable.invoke()
        log("transaction finished: {}", title)
        result
    }
}