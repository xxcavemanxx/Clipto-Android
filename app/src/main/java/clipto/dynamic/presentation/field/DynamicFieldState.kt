package clipto.dynamic.presentation.field

import androidx.fragment.app.FragmentActivity
import clipto.common.extensions.disposeSilently
import clipto.config.IAppConfig
import clipto.dynamic.DynamicField
import clipto.dynamic.presentation.field.model.RequestResponse
import clipto.dynamic.presentation.field.model.ResultCode
import clipto.dynamic.presentation.field.model.ViewMode
import clipto.store.StoreObject
import clipto.store.StoreState
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.reactivex.Maybe
import io.reactivex.disposables.Disposable
import javax.inject.Inject

@ActivityRetainedScoped
class DynamicFieldState @Inject constructor(
        appConfig: IAppConfig
) : StoreState(appConfig) {

    private var requestFieldDisposable: Disposable? = null

    val dynamicFieldRequest by lazy {
        StoreObject<RequestResponse>(
                id = "dynamic_field_request",
                liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    val dynamicFieldResponse by lazy {
        StoreObject<RequestResponse>(
                id = "dynamic_field_response",
                liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    private val dynamicFieldActivityRequest by lazy {
        StoreObject<Long>(
                id = "dynamic_field_activity_request",
                liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    fun requestCopyField(field: DynamicField): Maybe<RequestResponse> = requestField(field, ViewMode.COPY)
    fun requestInsertField(field: DynamicField): Maybe<RequestResponse> = requestField(field, ViewMode.INSERT)
    fun requestViewField(field: DynamicField): Maybe<RequestResponse> = requestField(field, ViewMode.VIEW)
    fun requestFillField(field: DynamicField): Maybe<RequestResponse> = requestField(field, ViewMode.FILL, ResultCode.UPDATE)
    fun requestEditField(field: DynamicField): Maybe<RequestResponse> = requestField(field, ViewMode.EDIT, ResultCode.UPDATE)

    private fun requestField(field: DynamicField, viewMode: ViewMode, resultCode: ResultCode? = null): Maybe<RequestResponse> {
        val request = RequestResponse(field = field, viewMode = viewMode, resultCode = resultCode)
        dynamicFieldRequest.setValue(request)
        dynamicFieldActivityRequest.setValue(System.currentTimeMillis(), force = true)
        return dynamicFieldResponse.getLiveChanges()
                .filter { it.isNotNull() }
                .map { it.requireValue() }
                .filter { it.id == request.id }
                .filter { it.resultCode != null }
                .firstElement()
                .doOnSubscribe {
                    requestFieldDisposable.disposeSilently()
                    requestFieldDisposable = it
                }
    }

    fun bind(activity: FragmentActivity) {
        dynamicFieldActivityRequest.getLiveData().observe(activity) {
            DynamicFieldFragment.show(activity)
        }
    }

}