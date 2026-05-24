package clipto.dynamic.presentation.field.blocks

import com.wb.clipboard.databinding.BlockDynamicFieldHeaderFillBinding
import android.view.View
import androidx.fragment.app.Fragment
import clipto.common.extensions.setDebounceClickListener
import clipto.extensions.getActionIconColorHighlight
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class HeaderFillBlock(
        val titleRes: Int,
        val title: String? = null,
        val actionTitleRes: Int,
        val actionActive: Boolean,
        val onAction: () -> Unit,
) : BlockItem<Fragment>() {

    override val layoutRes: Int = R.layout.block_dynamic_field_header_fill

    override fun areContentsTheSame(item: BlockItem<Fragment>): Boolean {
        return item is HeaderFillBlock
                && actionTitleRes == item.actionTitleRes
                && actionActive == item.actionActive
                && titleRes == item.titleRes
                && title == item.title
    }

    override fun onInit(fragment: Fragment, block: View) {
        val binding = BlockDynamicFieldHeaderFillBinding.bind(block)
        binding.mbClearAll.setDebounceClickListener {
            val ref = block.tag
            if (ref is HeaderFillBlock) {
                ref.onAction.invoke()
            }
        }
    }

    override fun onBind(fragment: Fragment, block: View) {
        val binding = BlockDynamicFieldHeaderFillBinding.bind(block)
        block.tag = this
        if (title != null) {
            binding.tvTitle.text = title
        } else {
            binding.tvTitle.setText(titleRes)
        }
        binding.mbClearAll.setText(actionTitleRes)
        if (actionActive) {
            binding.mbClearAll.setTextColor(block.context.getActionIconColorHighlight())
        } else {
            binding.mbClearAll.setTextColor(block.context.getTextColorSecondary())
        }
    }

}
