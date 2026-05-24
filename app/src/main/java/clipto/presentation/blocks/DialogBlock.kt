package clipto.presentation.blocks

import com.wb.clipboard.databinding.BlockDialogBinding
import android.view.View
import clipto.extensions.getTextColorPrimary
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.text.KeyValueStringWithHeader
import com.wb.clipboard.R

class DialogBlock<C>(
    private val titleRes: Int,
    private val descriptionRes: Int,
    private val value: String,
    private val enabled: Boolean = true,
    private val clickListener: View.OnClickListener
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_dialog

    override fun areItemsTheSame(item: BlockItem<C>): Boolean =
        item is DialogBlock && titleRes == item.titleRes

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is DialogBlock
                && enabled == item.enabled
                && descriptionRes == item.descriptionRes
                && value == item.value

    override fun onBind(context: C, block: View) {
        val binding = BlockDialogBinding.bind(block)
        var colorKey = block.context.getTextColorPrimary()
        val colorValue = block.context.getTextColorSecondary()
        val titleView = binding.titleView
        val valueView = binding.valueView
        if (!enabled) {
            colorKey = colorValue
        }

        KeyValueStringWithHeader(
            titleView,
            colorKey,
            colorValue,
            titleRes,
            descriptionRes
        )
        valueView.text = value

        if (enabled) {
            block.setOnClickListener(clickListener)
        } else {
            block.setOnClickListener(null)
        }

        block.isClickable = enabled
        titleView.isEnabled = enabled
        valueView.isEnabled = enabled
    }

}
