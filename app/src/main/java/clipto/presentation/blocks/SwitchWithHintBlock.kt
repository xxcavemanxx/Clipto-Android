package clipto.presentation.blocks

import android.view.View
import android.widget.CompoundButton
import clipto.common.extensions.disposeSilently
import clipto.common.extensions.setDebounceClickListener
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.presentation.common.dialog.hint.HintDialogData
import clipto.presentation.common.recyclerview.BlockItem
import clipto.store.internet.InternetState
import clipto.store.user.UserState
import com.wb.clipboard.R
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.block_switch_with_hint.view.*

class SwitchWithHintBlock<C>(
    private val onChecked: (checked: Boolean) -> Unit,
    private val enabled: Boolean = true,
    private val checked: Boolean,
    private val titleRes: Int,
    private val hintTitle: Int,
    private val hintIconRes: Int,
    private val hintDescription: Int = 0,
    private val hintDescriptionText: String? = null,
    private val dialogState: DialogState,
    private val userState: UserState? = null,
    private val internetState: InternetState? = null,
    private val uncheckConfirmTitle: Int? = null,
    private val uncheckConfirmDescription: Int? = null
) : BlockItem<C>() {

    private var authDisposable: Disposable? = null

    override val layoutRes: Int = R.layout.block_switch_with_hint

    override fun areItemsTheSame(item: BlockItem<C>): Boolean {
        return super.areItemsTheSame(item) && item is SwitchWithHintBlock && titleRes == item.titleRes
    }

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is SwitchWithHintBlock &&
                checked == item.checked &&
                enabled == item.enabled &&
                titleRes == item.titleRes &&
                hintTitle == item.hintTitle &&
                hintIconRes == item.hintIconRes &&
                hintDescription == item.hintDescription &&
                hintDescriptionText == item.hintDescriptionText &&
                internetState == item.internetState &&
                uncheckConfirmTitle == item.uncheckConfirmTitle &&
                uncheckConfirmDescription == item.uncheckConfirmDescription

    override fun onBind(context: C, block: View) {
        block.tag = this

        val titleView = block.titleView
        val valueView = block.valueView
        val ctx = block.context

        titleView.setText(titleRes)

        block.setOnClickListener(null)
        valueView.setOnCheckedChangeListener(null)
        valueView.isChecked = checked
        valueView.isEnabled = enabled

        var oneTimeListener: CompoundButton.OnCheckedChangeListener? = null
        oneTimeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            val ref = block.tag
            if (ref is SwitchWithHintBlock<*>) {
                valueView.setOnCheckedChangeListener(null)
                if (valueView.isChecked == isChecked) {
                    ref.onChecked.invoke(isChecked)
                } else {
                    valueView.isChecked = isChecked
                }
                valueView.setOnCheckedChangeListener(oneTimeListener)
            }
        }
        val oneTimeListenerNoProxy = oneTimeListener
        if (uncheckConfirmTitle != null && uncheckConfirmDescription != null) {
            val wrapped = oneTimeListener
            oneTimeListener = CompoundButton.OnCheckedChangeListener { v, isChecked ->
                if (isChecked) {
                    wrapped.onCheckedChanged(v, isChecked)
                } else {
                    dialogState.showConfirm(ConfirmDialogData(
                        iconRes = R.drawable.ic_attention,
                        title = ctx.getString(uncheckConfirmTitle),
                        description = ctx.getString(uncheckConfirmDescription),
                        confirmActionTextRes = R.string.button_confirm,
                        onConfirmed = { wrapped.onCheckedChanged(v, isChecked) },
                        onCanceled = { wrapped.onCheckedChanged(v, !isChecked) },
                        onClosed = { if (!it) wrapped.onCheckedChanged(v, !isChecked) }
                    ))
                }
            }
        }
        if (userState != null) {
            val wrapped = oneTimeListener
            oneTimeListener = CompoundButton.OnCheckedChangeListener { v, isChecked ->
                authDisposable.disposeSilently()
                if (!userState.isAuthorized()) oneTimeListenerNoProxy.onCheckedChanged(v, !isChecked)
                authDisposable = userState.signIn(UserState.SignInRequest.newRequireAuthRequest())
                    .subscribeOn(userState.getBackgroundScheduler())
                    .observeOn(userState.getViewScheduler())
                    .subscribe(
                        { wrapped.onCheckedChanged(v, isChecked) },
                        { oneTimeListenerNoProxy.onCheckedChanged(v, !isChecked) }
                    )
            }
        }
        if (internetState != null) {
            val wrapped = oneTimeListener
            oneTimeListener = CompoundButton.OnCheckedChangeListener { v, isChecked ->
                internetState.withInternet(
                    success = {
                        wrapped.onCheckedChanged(v, isChecked)
                    },
                    failed = {
                        oneTimeListenerNoProxy.onCheckedChanged(v, !isChecked)
                    }
                )
            }
        }
        valueView.setOnCheckedChangeListener(oneTimeListener)

        titleView.setDebounceClickListener {
            val title = ctx.getString(hintTitle)
            val description = hintDescriptionText ?: ctx.getString(hintDescription)
            dialogState.showHint(
                HintDialogData(
                    title = title,
                    description = description,
                    descriptionIsMarkdown = true,
                    iconRes = hintIconRes
                )
            )
        }
    }

}