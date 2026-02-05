package clipto.presentation.blocks.ux

import android.view.View
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class ZeroStateVerticalBlock<C> : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_zero_state_vertical

    override fun onBind(context: C, block: View) = Unit

}