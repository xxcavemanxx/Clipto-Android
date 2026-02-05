package clipto.presentation.main.nav

import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clipto.common.presentation.mvvm.MvvmFragment
import clipto.presentation.common.recyclerview.BlockListAdapter
import clipto.presentation.main.nav.blocks.FilterBlock
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_main_nav.*
import java.util.*

@AndroidEntryPoint
class MainNavFragment : MvvmFragment<MainNavViewModel>() {

    override val layoutResId: Int = R.layout.fragment_main_nav
    override val viewModel: MainNavViewModel by viewModels()

    val manualSortHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

        override fun isLongPressDragEnabled(): Boolean = false

        override fun onMove(
            recyclerView: RecyclerView,
            dragged: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val draggedPosition = dragged.adapterPosition
            val targetPosition = target.adapterPosition
            val adapter = recyclerView.adapter as BlockListAdapter<*>
            val currentBlocks = viewModel.currentBlocks
            val draggedItem = currentBlocks.getOrNull(draggedPosition)
            val targetItem = currentBlocks.getOrNull(targetPosition)
            val isWrongBlock = currentBlocks.filterIndexed { index, item -> index in draggedPosition..targetPosition && item !is FilterBlock }.isNotEmpty()
            return if (!isWrongBlock && draggedItem is FilterBlock && targetItem is FilterBlock && draggedItem.manualSort && targetItem.manualSort) {
                val draggedGroup = viewModel.getFilters().findGroup(draggedItem.filter)
                val targetGroup = viewModel.getFilters().findGroup(targetItem.filter)
                if (draggedGroup !== targetGroup) {
                    false
                } else {
                    if (draggedPosition < currentBlocks.size && targetPosition < currentBlocks.size) {
                        Collections.swap(currentBlocks, draggedPosition, targetPosition)
                        adapter.notifyItemMoved(draggedPosition, targetPosition)
                    }
                    true
                }
            } else {
                false
            }
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                viewHolder?.itemView?.setBackgroundResource(R.drawable.bg_filter_item_active)
            }
            super.onSelectedChanged(viewHolder, actionState)
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            val position = viewHolder.adapterPosition
            val item = viewModel.currentBlocks.getOrNull(position)?.takeIf { it is FilterBlock }?.let { it as FilterBlock }
            if (item == null || !item.isActive) {
                viewHolder.itemView.setBackgroundResource(R.drawable.bg_filter_item_inactive)
            }
            if (item != null) {
                viewModel.onMoved(item.filter)
            }
        }

        override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int = 0
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
    })

    override fun bind(viewModel: MainNavViewModel) {
        val ctx = requireContext()

        val filterAdapter = BlockListAdapter(this)
        recyclerView.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
        manualSortHelper.attachToRecyclerView(recyclerView)
        recyclerView.adapter = filterAdapter

        viewModel.getFiltersLive().observe(viewLifecycleOwner) {
            filterAdapter.submitList(it)
        }

        viewModel.requestUpdateFilter.observe(viewLifecycleOwner) { filter ->
            if (filter == null) return@observe
            val index = filterAdapter.currentList.indexOfFirst {
                var same = false
                if (it is FilterBlock && it.filter == filter) {
                    it.filter.withFilter(filter)
                    same = true
                }
                same
            }
            if (index != -1) {
                filterAdapter.notifyItemChanged(index)
            }
        }

    }

}
