package clipto.presentation.contextactions

import clipto.config.IAppConfig
import clipto.store.StoreObject
import clipto.store.StoreState
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
class ContextActionsState @Inject constructor(appConfig: IAppConfig) : StoreState(appConfig) {

    val finishRequest = StoreObject<FinishRequest>(
            "finish_request",
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
    )

    fun requestFinish() = finishRequest.setValue(FinishRequest())

    data class FinishRequest(
            val id: Long = System.currentTimeMillis()
    )

}