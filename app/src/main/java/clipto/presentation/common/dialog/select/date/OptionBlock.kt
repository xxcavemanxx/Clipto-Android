package clipto.presentation.common.dialog.select.date

import com.wb.clipboard.databinding.BlockDialogSelectDateBinding
import android.view.View
import androidx.lifecycle.MutableLiveData
import clipto.common.extensions.setBold
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.setVisibleOrGone
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

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
        val binding = BlockDialogSelectDateBinding.bind(block)
        block.setDebounceClickListener {
            val ref = block.tag
            if (ref is OptionBlock) {
                ref.onClicked()
            }
        }
    }

    override fun onBind(fragment: SelectDateDialogFragment, block: View) {
        val binding = BlockDialogSelectDateBinding.bind(block)
        block.tag = this
        binding.tvName.text = option.title
        binding.tvName.setBold(option.checked)
        binding.ivSelected.setVisibleOrGone(option.checked)
    }

    private fun onClicked() {
        viewModel.onClicked(this)
    }

}
