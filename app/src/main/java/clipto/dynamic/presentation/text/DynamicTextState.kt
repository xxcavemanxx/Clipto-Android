package clipto.dynamic.presentation.text

import android.app.Application
import androidx.fragment.app.FragmentActivity
import clipto.action.intent.IntentActionFactory
import clipto.action.intent.provider.DynamicTextProvider
import clipto.common.extensions.disposeSilently
import clipto.config.IAppConfig
import clipto.dynamic.DynamicTextRequestResponse
import clipto.dynamic.DynamicValueConfig
import clipto.dynamic.FormField
import clipto.store.StoreObject
import clipto.store.StoreState
import io.reactivex.Maybe
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicTextState @Inject constructor(
    appConfig: IAppConfig,
    private val app: Application,
    private val intentActionFactory: IntentActionFactory
) : StoreState(appConfig) {

    private var activityRequestDisposable: Disposable? = null

    val dynamicTextRequest by lazy {
        StoreObject<DynamicTextRequestResponse>(
            id = "dynamic_text_request",
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    val dynamicTextResponse by lazy {
        StoreObject<DynamicTextRequestResponse>(
            id = "dynamic_text_response"
        )
    }

    private val activityRequest by lazy {
        StoreObject<Long>(
            id = "dynamic_text_activity_request",
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    private val activityResponse by lazy {
        StoreObject<Long>(
            id = "dynamic_text_activity_response",
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    fun requestManualInput(text: CharSequence, config: DynamicValueConfig, fields: List<FormField>): Maybe<DynamicTextRequestResponse> {
        val request = DynamicTextRequestResponse(text = text, config = config, fields = fields)
        dynamicTextRequest.setValue(request)

        val requestId = System.currentTimeMillis()
        activityRequestDisposable.disposeSilently()
        activityRequest.setValue(requestId, force = true)
        activityRequestDisposable = activityResponse.getLiveChanges()
            .filter { it.isNotNull() }
            .map { it.value }
            .filter { it == requestId }
            .firstElement()
            .timeout(appConfig.getDynamicTextRequestDelay(), TimeUnit.MILLISECONDS)
            .subscribe(
                {},
                {
                    if (activityResponse.setValue(requestId)) {
                        val intent = intentActionFactory.getIntent(DynamicTextProvider.Action())
                        app.startActivity(intent)
                    }
                }
            )

        return dynamicTextResponse.getLiveChanges()
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .filter { it.id == request.id }
            .firstElement()
            .flatMap {
                if (it.canceled) {
                    Maybe.empty()
                } else {
                    Maybe.just(it)
                }
            }
    }

    fun bind(activity: FragmentActivity) {
        activityRequest.getLiveData().observe(activity) {
            if (activityResponse.setValue(it)) {
                DynamicTextFragment.show(activity)
            }
        }
    }

}