package clipto.presentation.blocks

import android.view.View
import android.widget.TextView
import clipto.extensions.TextTypeExt
import clipto.extensions.withConfig
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class DescriptionBlock<C>(
    val description: String,
    val textFont: Int,
    val textSize: Int
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_description

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is DescriptionBlock &&
                item.description == description &&
                item.textFont == textFont &&
                item.textSize == textSize

    override fun onBind(context: C, block: View) {
        block as TextView
        block.withConfig(textFont, textSize - 2)
        TextTypeExt.MARKDOWN.apply(block, description)
    }

}