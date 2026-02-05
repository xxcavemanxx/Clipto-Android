package clipto.dynamic.presentation.field

import android.app.Application
import androidx.lifecycle.Transformations
import clipto.action.CopyClipsAction
import clipto.common.extensions.disposeSilently
import clipto.common.presentation.mvvm.RxViewModel
import clipto.config.IAppConfig
import clipto.domain.Clip
import clipto.domain.ObjectType
import clipto.dynamic.DynamicContext
import clipto.dynamic.DynamicValuesFactory
import clipto.dynamic.presentation.field.model.ResultCode
import clipto.extensions.from
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.presentation.contextactions.ContextActionsState
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class DynamicFieldViewModel @Inject constructor(
        app: Application,
        val factory: DynamicValuesFactory,
        private val appConfig: IAppConfig,
        private val dialogState: DialogState,
        private val dynamicFieldState: DynamicFieldState,
        private val contextActionsState: ContextActionsState,
        private val copyClipsAction: CopyClipsAction,
        val dynamicContext: DynamicContext
) : RxViewModel(app) {

    private val requestLive by lazy { dynamicFieldState.dynamicFieldRequest.getMutableLiveData() }

    private var refreshDisposable: Disposable? = null

    val blocksLive by lazy {
        Transformations.map(requestLive) {
            val field = it.field
            val viewMode = it.viewMode
            val fieldProvider = factory.getFieldProvider(field)
            fieldProvider.createFieldConfig(field, viewMode, this)
        }
    }

    override fun doClear() {
        super.doClear()
        onUpdate()
    }

    fun onRefresh(withDelay: Boolean = false) {
        requestLive.value?.let { req ->
            refreshDisposable.disposeSilently()
            if (withDelay) {
                refreshDisposable = Single.just(req)
                        .delay(appConfig.getDynamicValueRefreshDelay(), TimeUnit.MILLISECONDS)
                        .filter { it.id == requestLive.value?.id }
                        .subscribeBy { requestLive.postValue(it) }
            } else {
                requestLive.postValue(req)
            }
        }
    }

    fun onCopy() {
        requestLive.value?.let { req ->
            val field = req.field
            val fieldProvider = factory.getFieldProvider(field)
            val placeholder = fieldProvider.createPlaceholder(field)
            val clip = Clip.from(placeholder).apply {
                objectType = ObjectType.INTERNAL_GENERATED
            }
            copyClipsAction.execute(listOf(clip)) {
                dismiss()
            }
        }
    }

    fun onDelete() {
        val confirmData = ConfirmDialogData(
                iconRes = R.drawable.ic_attention,
                title = string(R.string.dynamic_field_confirm_delete_title),
                description = string(R.string.dynamic_field_confirm_delete_description),
                confirmActionTextRes = R.string.button_yes,
                cancelActionTextRes = R.string.button_no,
                onConfirmed = { onComplete(ResultCode.DELETE) }
        )
        dialogState.showConfirm(confirmData)
    }

    fun onUpdate() {
        requestLive.value?.let { req ->
            dynamicFieldState.dynamicFieldResponse.setValue(req)
        }
    }

    fun onClosed() {
        contextActionsState.requestFinish()
    }

    fun onComplete(resultCode: ResultCode? = null) {
        requestLive.value?.let { req ->
            dynamicFieldState.dynamicFieldResponse.setValue(req.copy(resultCode = resultCode))
            dismiss()
        }
    }

}