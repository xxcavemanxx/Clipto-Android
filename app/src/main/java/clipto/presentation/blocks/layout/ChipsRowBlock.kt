package clipto.presentation.blocks.layout

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clipto.presentation.blocks.ChipBlock
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.recyclerview.BlockListAdapter
import com.wb.clipboard.R

class ChipsRowBlock<T, C>(
    private val items: List<ChipBlock<T, C>>,
    private val scrollToPosition: Int = -1,
    private val nestedScrollingEnabled: Boolean = true
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_chips_row

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is ChipsRowBlock<*, *> &&
                item.items == items &&
                item.scrollToPosition == scrollToPosition &&
                item.nestedScrollingEnabled == nestedScrollingEnabled

    override fun onInit(context: C, block: View) {
        block as RecyclerView
        val ctx = block.context
        block.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
        block.isNestedScrollingEnabled = nestedScrollingEnabled
        val adapter = BlockListAdapter(context)
        block.adapter = adapter
    }

    override fun onBind(context: C, block: View) {
        block as RecyclerView
        block.isNestedScrollingEnabled = nestedScrollingEnabled
        val adapter = block.adapter as BlockListAdapter<C>
        adapter.submitList(items) {
            if (scrollToPosition != -1) {
                block.smoothScrollToPosition(scrollToPosition)
            }
        }
    }

}