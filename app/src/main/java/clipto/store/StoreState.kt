package clipto.store

import android.content.res.Resources
import clipto.common.logging.L
import clipto.common.presentation.mvvm.RxViewModel
import clipto.config.IAppConfig
import io.reactivex.Scheduler
import java.util.*
import java.util.concurrent.TimeUnit

abstract class StoreState(val appConfig: IAppConfig) {

    fun getRxTimeout(): Long = appConfig.getRxTimeout()
    fun getLocale(): Locale = Resources.getSystem().configuration.locale ?: Locale.getDefault()
    fun getLanguage(): String = getLocale().language.toLowerCase()
    fun onMain(func: () -> Unit) = getViewScheduler().scheduleDirect(func)
    fun onBackground(func: () -> Unit) = getBackgroundScheduler().scheduleDirect(func)
    fun onMain(delay: Long, func: () -> Unit) = getViewScheduler().scheduleDirect(func, delay, TimeUnit.MILLISECONDS)
    fun getBackgroundScheduler(): Scheduler = RxViewModel.defaultBackgroundScheduler.value
    fun getViewScheduler(): Scheduler = RxViewModel.defaultViewScheduler.value
    protected fun log(message: String, vararg args: Any?) = L.log(this, message, *args)

}