package clipto.dynamic.presentation.field.blocks

import com.wb.clipboard.databinding.BlockDynamicFieldHeaderEditBinding
import android.view.View
import androidx.fragment.app.Fragment
import clipto.common.extensions.setDebounceClickListener
import clipto.dynamic.presentation.field.DynamicFieldViewModel
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class HeaderEditBlock(
        val titleRes: Int,
        val viewModel: DynamicFieldViewModel
) : BlockItem<Fragment>() {

    override val layoutRes: Int = R.layout.block_dynamic_field_header_edit

    override fun areContentsTheSame(item: BlockItem<Fragment>): Boolean {
        return item is HeaderEditBlock
                && titleRes == item.titleRes
    }

    override fun onInit(fragment: Fragment, block: View) {
        val binding = BlockDynamicFieldHeaderEditBinding.bind(block)
        binding.ivDelete.setDebounceClickListener { viewModel.onDelete() }
        binding.ivCopy.setDebounceClickListener { viewModel.onCopy() }
    }

    override fun onBind(fragment: Fragment, block: View) {
        val binding = BlockDynamicFieldHeaderEditBinding.bind(block)
        binding.tvName.setText(titleRes)
    }

}
