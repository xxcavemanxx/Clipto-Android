package clipto.presentation.blocks

import com.wb.clipboard.databinding.BlockDescriptionSecondaryBinding
import android.view.View
import clipto.common.extensions.gone
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.visible
import clipto.extensions.TextTypeExt
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class DescriptionSecondaryBlock<C>(
    val description: String,
    val textFont: Int = 0,
    val textSize: Int = 0,
    val onCancel: (() -> Unit)? = null,
    val plainText: Boolean = false,
    val maxLines: Int = Integer.MAX_VALUE
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_description_secondary

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is DescriptionSecondaryBlock &&
                item.description == description &&
                item.textFont == textFont &&
                item.textSize == textSize &&
                item.plainText == plainText &&
                item.maxLines == maxLines

    override fun onBind(context: C, block: View) {
        val binding = BlockDescriptionSecondaryBinding.bind(block)
        val textView = binding.tvText
//        block.withConfig(textFont, textSize - 2)
        if (!plainText) {
            TextTypeExt.MARKDOWN.apply(textView, description)
        } else {
            textView.text = description
        }
        textView.maxLines = maxLines

        if (onCancel != null) {
            binding.ivClose.setDebounceClickListener { onCancel.invoke() }
            binding.ivClose.visible()
        } else {
            binding.ivClose.gone()
        }
    }

}
