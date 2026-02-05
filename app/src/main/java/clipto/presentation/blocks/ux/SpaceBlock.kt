package clipto.presentation.blocks.ux

import android.content.Context
import android.view.View
import androidx.core.view.updateLayoutParams
import clipto.common.misc.AndroidUtils
import clipto.common.misc.Units
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class SpaceBlock<C>(
    val heightInDp: Int = -1,
    val widthInDp: Int = -1
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_space

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is SpaceBlock &&
                heightInDp == item.heightInDp &&
                widthInDp == item.widthInDp

    override fun onBind(context: C, block: View) {
        block.updateLayoutParams {
            if (heightInDp != -1) {
                height = Units.DP.toPx(heightInDp.toFloat()).toInt()
            }
            if (widthInDp != -1) {
                width = Units.DP.toPx(widthInDp.toFloat()).toInt()
            }
        }
    }

    companion object {
        fun <C> xl(): SpaceBlock<C> = SpaceBlock(28)
        fun <C> lg(): SpaceBlock<C> = SpaceBlock(24)
        fun <C> md(): SpaceBlock<C> = SpaceBlock(20)
        fun <C> sm(): SpaceBlock<C> = SpaceBlock(16)
        fun <C> xs(): SpaceBlock<C> = SpaceBlock(12)
        fun <C> dp8(): SpaceBlock<C> = SpaceBlock(8)
        fun <C> xxs(): SpaceBlock<C> = SpaceBlock(4)
        fun <C> fullSize(ctx: Context, minusHeightInDp: Int = 0): SpaceBlock<C> = SpaceBlock(
            Units.PX.toDp(AndroidUtils.getDisplaySize(ctx).y.toFloat()).toInt() - minusHeightInDp
        )

        fun <C> screenSize(ctx: Context, multiplier: Float = 0.5f): SpaceBlock<C> = SpaceBlock(
            Units.PX.toDp(AndroidUtils.getDisplaySize(ctx).y.toFloat() * multiplier).toInt()
        )
    }

}