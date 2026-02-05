package clipto.presentation.blocks.layout

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clipto.common.misc.Units
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.recyclerview.BlockListAdapter
import com.grzegorzojdana.spacingitemdecoration.Spacing
import com.grzegorzojdana.spacingitemdecoration.SpacingItemDecoration
import com.wb.clipboard.R

class RowBlock<C>(
    private val items: List<BlockItem<C>>,
    private val scrollToPosition: Int = -1,
    private val spacingInDp: Int = 16
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_row

    override fun areItemsTheSame(item: BlockItem<C>): Boolean {
        return super.areItemsTheSame(item) && item is RowBlock<*> && spacingInDp == item.spacingInDp
    }

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is RowBlock<*> &&
                item.items == items &&
                item.spacingInDp == spacingInDp &&
                item.scrollToPosition == scrollToPosition

    override fun onInit(context: C, block: View) {
        block as RecyclerView
        val ctx = block.context
        block.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
        if (spacingInDp != 0) {
            block.addItemDecoration(
                SpacingItemDecoration(
                    Spacing(
                        horizontal = Units.DP.toPx(spacingInDp.toFloat()).toInt()
                    )
                )
            )
        }
        val adapter = BlockListAdapter(context)
        block.adapter = adapter
    }

    override fun onBind(context: C, block: View) {
        block as RecyclerView
        val adapter = block.adapter as BlockListAdapter<C>
        adapter.submitList(items) {
            if (scrollToPosition != -1) {
                block.scrollToPosition(scrollToPosition)
            }
        }
    }

}