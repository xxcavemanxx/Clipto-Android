package clipto.dao.objectbox

import android.app.Application
import clipto.config.IAppConfig
import clipto.extensions.log
import clipto.store.app.AppState
import io.objectbox.Box
import io.objectbox.BoxStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
abstract class AbstractBoxDao<E> : IBoxDao<E> {

    @Inject
    lateinit var appConfig: IAppConfig

    @Inject
    lateinit var boxStore: BoxStore

    @Inject
    lateinit var app: Application

    val box: Box<E> by lazy { boxStore.boxFor(getType()) }

    override fun init() {
        log("init box: {}", getType())
    }

    override fun clear() {
        log("clear box: {}", getType())
        box.removeAll()
    }

}