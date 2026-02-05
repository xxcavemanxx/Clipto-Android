package clipto.store.filter

import clipto.config.IAppConfig
import clipto.domain.Filter
import clipto.store.StoreObject
import clipto.store.StoreState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterState @Inject constructor(
    appConfig: IAppConfig
) : StoreState(appConfig) {

    val requestUpdateFilter by lazy {
        StoreObject<Filter>(id = "request_update_filter")
    }

    fun requestUpdateFilter(filter: Filter) = requestUpdateFilter.setValue(filter, force = true)
}