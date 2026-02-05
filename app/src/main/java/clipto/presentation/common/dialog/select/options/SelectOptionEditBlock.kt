package clipto.presentation.common.dialog.select.options

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.MutableLiveData
import clipto.common.extensions.*
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.recyclerview.BlockItemViewHolder
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_dialog_select_option_edit.view.*

@SuppressLint("ClickableViewAccessibility")
class SelectOptionEditBlock(
    private val viewModel: SelectOptionsDialogViewModel,
    val live: MutableLiveData<List<BlockItem<SelectOptionsDialogFragment>>>,
    val option: SelectOptionsDialogRequest.Option,
    val data: SelectOptionsDialogRequest
) : BlockItem<SelectOptionsDialogFragment>() {

    override val layoutRes: Int = if (data.withTitle) R.layout.block_dialog_select_option_edit else R.layout.block_dialog_select_option_edit_no_title

    override fun areContentsTheSame(item: BlockItem<SelectOptionsDialogFragment>): Boolean {
        return item is SelectOptionEditBlock
                && item.option === option
    }

    override fun onInit(context: SelectOptionsDialogFragment, holder: BlockItemViewHolder<SelectOptionsDialogFragment, *>) {
        val block = holder.itemView
        block.ivDrag.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                context.touchHelper.startDrag(holder)
            }
            false
        }
        block.ivSave.setDebounceClickListener {
            val ref = block.tag
            if (ref is SelectOptionEditBlock) {
                viewModel.onSaveOption(ref.data, ref.option, ref.live)
            }
        }

        block.etValue.doAfterTextChanged {
            val ref = block.tag
            if (ref is SelectOptionEditBlock) {
                ref.option.value = it?.toString()?.toNullIfEmpty(trim = false)
            }
        }

        if (data.withTitle) {
            val guideline = block.guideline
            block.etValue.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    guideline.setGuidelinePercent(0.75f)
                } else {
                    guideline.setGuidelinePercent(0.5f)
                }
            }
            block.etLabel.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    guideline.setGuidelinePercent(0.25f)
                } else {
                    guideline.setGuidelinePercent(0.5f)
                }
            }
            block.etLabel.doAfterTextChanged {
                val ref = block.tag
                if (ref is SelectOptionEditBlock) {
                    ref.option.title = it?.toString()?.toNullIfEmpty(trim = true)
                }
            }
        }
    }

    override fun onBind(context: SelectOptionsDialogFragment, block: View) {
        block.tag = null
        if (data.withTitle) {
            block.etLabel.setText(option.title)
        }
        block.etValue.setTextWithSelection(option.value)
        block.etValue.showKeyboard {
            if (data.withTitle) {
                block.guideline?.setGuidelinePercent(0.75f)
            }
            block.etValue?.touch()
        }
        block.tag = this
    }

}