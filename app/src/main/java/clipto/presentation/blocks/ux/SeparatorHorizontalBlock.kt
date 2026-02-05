package clipto.presentation.blocks.ux

import android.view.View
import clipto.common.extensions.doToPx
import clipto.common.extensions.updateMargin
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_separator_horizontal.view.*

class SeparatorHorizontalBlock<C>(
    private val marginTop: Int = 16,
    private val marginBottom: Int = 16,
    private val marginStart: Int = 16,
    private val marginEnd: Int = 16,
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_separator_horizontal
    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is SeparatorHorizontalBlock
                && marginTop == item.marginTop
                && marginBottom == item.marginBottom
                && marginStart == item.marginStart
                && marginEnd == item.marginEnd

    override fun onBind(context: C, block: View) {
        block.separator.updateMargin(
            top = marginTop.doToPx(),
            bottom = marginBottom.doToPx(),
            left = marginStart.doToPx(),
            right = marginEnd.doToPx()
        )
    }

}