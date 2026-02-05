package clipto.presentation.blocks.ux

import android.view.View
import clipto.common.extensions.updateMargin
import clipto.common.misc.Units
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_separator_vertical.view.*

class SeparatorVerticalBlock<C>(
    private val marginHoriz: Int = 16,
    private val marginVert: Int = 0
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_separator_vertical
    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is SeparatorVerticalBlock
                && marginHoriz == item.marginHoriz
                && marginVert == item.marginVert

    override fun onBind(context: C, block: View) {
        val h = Units.DP.toPx(marginHoriz.toFloat()).toInt()
        val v = Units.DP.toPx(marginVert.toFloat()).toInt()
        block.separator.updateMargin(
            left = h,
            right = h,
            top = v,
            bottom = v
        )
    }

}