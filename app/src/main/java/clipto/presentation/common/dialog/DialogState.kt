package clipto.presentation.common.dialog

import android.Manifest
import android.app.Application
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import clipto.AppContext
import clipto.analytics.Analytics
import clipto.common.extensions.disposeSilently
import clipto.common.extensions.withPermissions
import clipto.config.IAppConfig
import clipto.domain.Clip
import clipto.domain.FastAction
import clipto.domain.ObjectType
import clipto.dynamic.DynamicField
import clipto.dynamic.DynamicValueConfig
import clipto.exception.ValidationException
import clipto.extensions.TextTypeExt
import clipto.presentation.common.dialog.blocks.BlocksDialogFragment
import clipto.presentation.common.dialog.blocks.BlocksDialogRequest
import clipto.presentation.common.dialog.blocks.BlocksDialogViewModel
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.presentation.common.dialog.confirm.ConfirmDialogFragment
import clipto.presentation.common.dialog.hint.HintDialogData
import clipto.presentation.common.dialog.hint.HintDialogFragment
import clipto.presentation.common.dialog.select.date.SelectDateDialogFragment
import clipto.presentation.common.dialog.select.date.SelectDateDialogRequest
import clipto.presentation.common.dialog.select.options.SelectOptionsDialogFragment
import clipto.presentation.common.dialog.select.options.SelectOptionsDialogRequest
import clipto.presentation.common.dialog.select.value.SelectValueDialogFragment
import clipto.presentation.common.dialog.select.value.SelectValueDialogRequest
import clipto.presentation.contextactions.ContextActionsFragment
import clipto.store.StoreObject
import clipto.store.StoreState
import com.google.android.material.snackbar.Snackbar
import com.wb.clipboard.R
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.config.ScannerConfig
import io.reactivex.disposables.Disposable
import javax.inject.Inject

@ActivityRetainedScoped
class DialogState @Inject constructor(
    private val app: Application,
    appConfig: IAppConfig,
) : StoreState(appConfig) {

    private val snackbar by lazy {
        StoreObject<CharSequence>(
            id = "snackbar",
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    private val hintDialog by lazy {
        StoreObject<HintDialogData>(
            id = "hint_dialog",
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    private val confirmDialog by lazy {
        StoreObject<ConfirmDialogData>(
            id = "confirm_dialog",
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    private val selectValueDialog by lazy {
        StoreObject<SelectValueDialogRequest<*>>(
            id = "select_value_dialog",
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    private val selectDateDialog by lazy {
        StoreObject<SelectDateDialogRequest>(
            id = "select_date_dialog",
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    private val selectOptionsDialog by lazy {
        StoreObject<SelectOptionsDialogRequest>(
            id = "select_options_dialog",
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    private val blocksDialog by lazy {
        StoreObject<BlocksDialogRequest>(
            id = "blocks_dialog",
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    private var scanBarcodeDisposable: Disposable? = null
    private val scanBarcodeRequest = StoreObject<Boolean>(id = "scan_barcode_request", liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER)
    private val scanBarcodeResponse = StoreObject<QRResult>(id = "scan_barcode_response", liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER)

    private var fastActionRequestDisposable: Disposable? = null
    val fastActionRequest = StoreObject<FastActionRequest>(id = "fast_action_request", liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER)

    fun requestFastAction(action: FastAction, clip: Clip) = fastActionRequest.setValue(FastActionRequest(action = action, clip = clip))

    fun requestScanBarcode(onSuccess: (result: String) -> Unit) {
        scanBarcodeDisposable.disposeSilently()
        scanBarcodeRequest.setValue(true, force = true)
        scanBarcodeResponse.setValue(null)
        scanBarcodeDisposable = scanBarcodeResponse.getLiveChanges()
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .firstElement()
            .observeOn(getViewScheduler())
            .subscribe(
                { result ->
                    if (result is QRResult.QRSuccess) {
                        val barcode = result.content.rawValue
                        onSuccess.invoke(barcode)
                    }
                    scanBarcodeDisposable.disposeSilently()
                    scanBarcodeDisposable = null
                },
                { scanBarcodeDisposable = null }
            )
    }

    fun onBarcodeResult(result: QRResult) = scanBarcodeResponse.setValue(result, force = true)

    fun showHint(data: HintDialogData) = hintDialog.setValue(data)

    fun showConfirm(data: ConfirmDialogData) = confirmDialog.setValue(data)

    fun showSnackbar(message: CharSequence) = snackbar.setValue(message, force = true)

    fun requestSelectDateDialog(request: SelectDateDialogRequest) = selectDateDialog.setValue(request)

    fun <T> requestSelectValueDialog(request: SelectValueDialogRequest<T>) = selectValueDialog.setValue(request)

    fun requestSelectOptionsDialog(request: SelectOptionsDialogRequest) = selectOptionsDialog.setValue(request)

    fun showConfirmAction(action: String, onConfirm: () -> Unit) {
        val data = ConfirmDialogData(
            title = app.getString(R.string.common_action_confirm),
            iconRes = R.drawable.ic_attention,
            description = "```\n${action}\n```",
            descriptionIsMarkdown = true,
            confirmActionTextRes = R.string.button_confirm,
            onConfirmed = onConfirm
        )
        showConfirm(data)
    }

    fun requestBlocksDialog(
        onBackConsumed: (viewModel: BlocksDialogViewModel) -> Boolean = { false },
        onCreateAdapter: ((viewModel: BlocksDialogViewModel, fragment: Fragment, adapter: RecyclerView.Adapter<*>) -> RecyclerView.Adapter<*>)? = null,
        onDestroy: (viewModel: BlocksDialogViewModel) -> Unit = {},
        onReady: (viewModel: BlocksDialogViewModel) -> Unit = {}
    ) = blocksDialog.setValue(
        BlocksDialogRequest(
            onReady = onReady,
            onDestroy = onDestroy,
            onBackConsumed = onBackConsumed,
            onCreateAdapter = onCreateAdapter
        )
    )

    fun showAlert(title: CharSequence, description: CharSequence) = hintDialog.setValue(
        HintDialogData(
            title = title.toString(),
            description = description.toString(),
            actionRes = android.R.string.ok
        )
    )

    fun showError(title: CharSequence, description: CharSequence = "") = hintDialog.setValue(
        HintDialogData(
            title = title.toString(),
            description = description.toString(),
            actionRes = android.R.string.ok,
            iconRes = R.drawable.ic_attention,
            withDefaultIconColor = false
        )
    )

    fun showError(th: Throwable) =
        when (th) {
            is ValidationException -> {
                showError(th.errorMessage)
            }
            else -> {
                hintDialog.setValue(
                    HintDialogData(
                        title = app.getString(R.string.essentials_errors_unknown),
                        description = th.stackTraceToString(),
                        actionRes = android.R.string.ok,
                        iconRes = R.drawable.ic_status_error,
                        withDefaultIconColor = false
                    )
                )
            }
        }

    fun bind(activity: FragmentActivity, resultLauncher: ActivityResultLauncher<ScannerConfig>) {
        scanBarcodeRequest.getLiveData().observe(activity) {
            activity.withPermissions(Manifest.permission.CAMERA) {
                if (!it.containsValue(false)) {
                    resultLauncher.launch(ScannerConfig.build {
                        setOverlayStringRes(R.string.main_action_notes_barcode_title)
                        setOverlayDrawableRes(R.drawable.ic_barcode)
                        setShowTorchToggle(true)
                    })
                } else {
                    Analytics.onPermissionDenied(Manifest.permission.CAMERA)
                }
            }
        }
    }

    fun bind(activity: FragmentActivity) {
        hintDialog.getLiveData().observe(activity) {
            it?.let {
                HintDialogFragment.show(activity, it)
                hintDialog.clearValue()
            }
        }
        confirmDialog.getLiveData().observe(activity) {
            it?.let {
                ConfirmDialogFragment.show(activity, it)
                confirmDialog.clearValue()
            }
        }
        selectValueDialog.getLiveData().observe(activity) {
            it?.let {
                SelectValueDialogFragment.show(activity, it)
                selectValueDialog.clearValue()
            }
        }
        selectDateDialog.getLiveData().observe(activity) {
            it?.let {
                SelectDateDialogFragment.show(activity, it)
                selectDateDialog.clearValue()
            }
        }
        selectOptionsDialog.getLiveData().observe(activity) {
            it?.let {
                SelectOptionsDialogFragment.show(activity, it)
                selectOptionsDialog.clearValue()
            }
        }
        snackbar.getLiveData().observe(activity) { text ->
            activity.findViewById<View>(android.R.id.content)?.let { view ->
                Snackbar.make(view, text, Snackbar.LENGTH_LONG).show()
            }
        }
        blocksDialog.getLiveData().observe(activity) {
            it?.let { BlocksDialogFragment.show(activity, it) }
        }
        fastActionRequest.getLiveData().observe(activity) { request ->
            request?.let {
                fastActionRequestDisposable.disposeSilently()
                val text = request.clip.text ?: return@observe
                fastActionRequestDisposable = AppContext.get().dynamicValuesRepository.get()
                    .process(text, DynamicValueConfig(clip = request.clip, fastActionRequest = true))
                    .subscribeOn(getBackgroundScheduler())
                    .observeOn(getViewScheduler())
                    .subscribe(
                        { modifiedText ->
                            val clip = request.clip
                            val action = request.action
                            if (action.requiredPreRendering || DynamicField.isDynamic(text)) {
                                val modifiedClip = Clip().apply {
                                    this.objectType = ObjectType.INTERNAL_GENERATED
                                    this.text = modifiedText.toString()
                                    this.textType = clip.textType
                                    this.snippet = clip.snippet
                                    this.tagIds = clip.tagIds
                                    this.title = clip.title
                                    this.fav = clip.fav
                                }
                                ContextActionsFragment.process(activity, force = true) { fragment ->
                                    fragment?.getTextViewRef()?.let { tv ->
                                        request.action.process(modifiedClip, tv) {
                                            fastActionRequest.clearValue()
                                        }
                                    }
                                }
                            } else {
                                request.action.process(clip, TextView(app)) {
                                    fastActionRequest.clearValue()
                                }
                            }
                        },
                        { Analytics.onError("fast_action_request", it) }
                    )
            }
        }
    }

    fun string(@StringRes id: Int): String = app.getString(id)
    fun string(@StringRes id: Int, vararg args: Any?): String = app.getString(id, *args)

    data class FastActionRequest(
        val id: Long = System.currentTimeMillis(),
        val action: FastAction,
        val clip: Clip
    )

}