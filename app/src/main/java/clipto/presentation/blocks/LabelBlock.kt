package clipto.presentation.blocks

import com.wb.clipboard.databinding.BlockLabelBinding
import android.view.View
import clipto.extensions.getTextColorPrimary
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.text.KeyValueStringWithHeader
import com.wb.clipboard.R

class LabelBlock<C>(
    private val titleRes: Int,
    private val iconRes: Int = 0,
    private val descriptionRes: Int = 0,
    private val description: String? = null,
    private val clickListener: View.OnClickListener
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_label

    override fun areItemsTheSame(item: BlockItem<C>): Boolean =
        item is LabelBlock && titleRes == item.titleRes

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is LabelBlock
                && iconRes == item.iconRes
                && descriptionRes == item.descriptionRes
                && description == item.description

    override fun onBind(context: C, block: View) {
        val binding = BlockLabelBinding.bind(block)
        val colorKey = block.context.getTextColorPrimary()
        val colorValue = block.context.getTextColorSecondary()

        when {
            description != null -> {
                KeyValueStringWithHeader(
                    binding.titleView,
                    colorKey,
                    colorValue,
                    titleRes,
                    descriptionRes,
                    description = description
                )
            }
            descriptionRes != 0 -> {
                KeyValueStringWithHeader(
                    binding.titleView,
                    colorKey,
                    colorValue,
                    titleRes,
                    descriptionRes
                )
            }
            else -> {
                binding.titleView.setText(titleRes)
            }
        }

        binding.titleView.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes, 0, 0, 0)
        block.setOnClickListener(clickListener)
    }

}
