package clipto.presentation.common.dialog.select.date

import android.view.View
import androidx.lifecycle.MutableLiveData
import clipto.common.extensions.setBold
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.setVisibleOrGone
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_dialog_select_date.view.*

class OptionBlock(
    private val viewModel: SelectDateDialogViewModel,
    val live: MutableLiveData<List<BlockItem<SelectDateDialogFragment>>>,
    val option: SelectDateDialogRequest.Option,
    val data: SelectDateDialogRequest
) : BlockItem<SelectDateDialogFragment>() {

    override val layoutRes: Int = R.layout.block_dialog_select_date

    override fun areItemsTheSame(item: BlockItem<SelectDateDialogFragment>): Boolean {
        return super.areItemsTheSame(item) &&
                item is OptionBlock &&
                item.option.model == option.model
    }

    override fun areContentsTheSame(item: BlockItem<SelectDateDialogFragment>): Boolean {
        return item is OptionBlock &&
                item.option.title == option.title &&
                item.option.checked == option.checked
    }

    override fun onInit(fragment: SelectDateDialogFragment, block: View) {
        block.setDebounceClickListener {
            val ref = block.tag
            if (ref is OptionBlock) {
                ref.onClicked()
            }
        }
    }

    override fun onBind(fragment: SelectDateDialogFragment, block: View) {
        block.tag = this
        block.tvName.text = option.title
        block.tvName.setBold(option.checked)
        block.ivSelected.setVisibleOrGone(option.checked)
    }

    private fun onClicked() {
        viewModel.onClicked(this)
    }

}