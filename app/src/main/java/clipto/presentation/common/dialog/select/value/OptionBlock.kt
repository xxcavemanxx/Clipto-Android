package clipto.presentation.common.dialog.select.value

import android.content.res.ColorStateList
import android.view.View
import androidx.lifecycle.MutableLiveData
import clipto.common.extensions.*
import clipto.extensions.TextTypeExt
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_dialog_select_value.view.*

class OptionBlock<T>(
    private val viewModel: SelectValueDialogViewModel,
    val live: MutableLiveData<List<BlockItem<SelectValueDialogFragment>>>,
    val option: SelectValueDialogRequest.Option<T>,
    val data: SelectValueDialogRequest<T>,
    val highlight: String? = null
) : BlockItem<SelectValueDialogFragment>() {

    override val layoutRes: Int = R.layout.block_dialog_select_value

    override fun areItemsTheSame(item: BlockItem<SelectValueDialogFragment>): Boolean {
        return super.areItemsTheSame(item) &&
                item is OptionBlock<*> &&
                item.option.model == option.model
    }

    override fun areContentsTheSame(item: BlockItem<SelectValueDialogFragment>): Boolean {
        return item is OptionBlock<*> &&
                item.option.checked == option.checked &&
                item.option.iconRes == option.iconRes &&
                item.option.iconColor == option.iconColor &&
                item.option.title == option.title &&
                item.highlight == highlight
    }

    override fun onInit(fragment: SelectValueDialogFragment, block: View) {
        block.setDebounceClickListener {
            val ref = block.tag
            if (ref is OptionBlock<*>) {
                ref.onClicked()
            }
        }
    }

    override fun onBind(fragment: SelectValueDialogFragment, block: View) {
        block.tag = this
        var text = option.title
        if (highlight != null && text != null) {
            text = TextTypeExt.TEXT_PLAIN.highlight(block.context, text, highlight)
        }
        block.tvName.text = text
        block.tvName.setBold(option.checked)
        block.ivSelected.setVisibleOrGone(option.checked)
        if (option.iconRes != null) {
            block.ivIcon.imageTintList = ColorStateList.valueOf(option.iconColor ?: block.context.getTextColorSecondary())
            block.ivIcon.setImageResource(option.iconRes)
            block.ivIcon.visible()
        } else {
            block.ivIcon.gone()
        }
    }

    private fun onClicked() {
        viewModel.onClicked(this)
    }

}