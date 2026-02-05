package clipto.presentation.blocks.ux

import android.view.View
import android.widget.TextView
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class EmptyStateVerticalBlock<C>(
    val titleRes: Int = R.string.error_data_empty
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_empty_state_vertical

    override fun areContentsTheSame(item: BlockItem<C>): Boolean {
        return item is EmptyStateVerticalBlock &&
                item.titleRes == titleRes
    }

    override fun onBind(context: C, block: View) {
        block as TextView
        block.setText(titleRes)
    }

}