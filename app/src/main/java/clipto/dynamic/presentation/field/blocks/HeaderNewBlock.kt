package clipto.dynamic.presentation.field.blocks

import com.wb.clipboard.databinding.BlockDynamicFieldHeaderNewBinding
import android.view.View
import androidx.fragment.app.Fragment
import clipto.common.extensions.setDebounceClickListener
import clipto.dynamic.presentation.field.DynamicFieldViewModel
import clipto.dynamic.presentation.field.model.ResultCode
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class HeaderNewBlock(
        val titleRes: Int,
        val viewModel: DynamicFieldViewModel,
        val resultCode: ResultCode
) : BlockItem<Fragment>() {

    override val layoutRes: Int = R.layout.block_dynamic_field_header_new

    override fun areContentsTheSame(item: BlockItem<Fragment>): Boolean {
        return item is HeaderNewBlock
                && titleRes == item.titleRes
                && resultCode == item.resultCode
    }

    override fun onInit(fragment: Fragment, block: View) {
        val binding = BlockDynamicFieldHeaderNewBinding.bind(block)
        binding.mbInsert.setDebounceClickListener {
            val ref = block.tag
            if (ref is HeaderNewBlock) {
                viewModel.onComplete(ref.resultCode)
            }
        }
    }

    override fun onBind(fragment: Fragment, block: View) {
        val binding = BlockDynamicFieldHeaderNewBinding.bind(block)
        block.tag = this
        val actionTitleRes = if (resultCode == ResultCode.INSERT) R.string.button_insert else R.string.menu_copy
        binding.mbInsert.setText(actionTitleRes)
        binding.tvName.setText(titleRes)
    }

}
