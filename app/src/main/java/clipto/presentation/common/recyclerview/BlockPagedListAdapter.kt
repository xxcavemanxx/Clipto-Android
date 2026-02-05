package clipto.presentation.common.recyclerview

import android.view.ViewGroup
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import clipto.common.extensions.getSpanCount
import clipto.common.misc.ThemeUtils
import clipto.extensions.log
import com.wb.clipboard.R
import java.util.*

open class BlockPagedListAdapter<C, I : BlockItem<C>>(val context: C) : PagedListAdapter<I, BlockItemViewHolder<C, I>>(createDiffUtils()) {

    private var fallbackLayoutRes: Int = 0

    private var listSnapshot: List<I> = emptyList()

    override fun getItemViewType(position: Int): Int {
        return fallbackLayoutRes
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockItemViewHolder<C, I> {
        return BlockItemViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BlockItemViewHolder<C, I>, position: Int) {
        val block = getItem(position)?.takeIf { it.layoutRes == fallbackLayoutRes }
        holder.bind(context, block)
    }

    override fun submitList(pagedList: PagedList<I>?) {
        initList(pagedList)
        super.submitList(pagedList)
    }

    override fun submitList(pagedList: PagedList<I>?, commitCallback: Runnable?) {
        initList(pagedList)
        super.submitList(pagedList, commitCallback)
    }

    private fun initList(pagedList: PagedList<I>?) {
        val fallbackLayoutRes = pagedList?.firstOrNull()?.layoutRes ?: 0
        if (fallbackLayoutRes != 0) {
            this.fallbackLayoutRes = fallbackLayoutRes
        }
        listSnapshot = pagedList ?: emptyList()
    }

    fun onScreenChanged(recyclerView: RecyclerView) {
        val layoutManager = recyclerView.layoutManager
        if (layoutManager is StaggeredGridLayoutManager) {
            val context = recyclerView.context
            val spanCount = layoutManager.spanCount
            val newSpanCount = context.getSpanCount()
            log("onScreenChanged :: {} -> {}", spanCount, newSpanCount)
            if (newSpanCount >= 1 && newSpanCount != spanCount) {
                recyclerView.layoutManager = StaggeredGridLayoutManager(newSpanCount, StaggeredGridLayoutManager.VERTICAL)
            }
        }
    }

    fun createTouchHelper(dataToSwap: () -> List<Any>, canMove: () -> Boolean): ItemTouchHelper {
        return ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

            override fun onMove(
                recyclerView: RecyclerView,
                dragged: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (!canMove.invoke()) return false

                val draggedPosition = dragged.adapterPosition
                val targetPosition = target.adapterPosition
                val currentBlocks = listSnapshot

                if (draggedPosition < currentBlocks.size && targetPosition < currentBlocks.size) {
                    Collections.swap(currentBlocks, draggedPosition, targetPosition)
                    notifyItemMoved(draggedPosition, targetPosition)
                    val dataList = dataToSwap.invoke()
                    if (draggedPosition < dataList.size && targetPosition < dataList.size) {
                        Collections.swap(dataList, draggedPosition, targetPosition)
                    }
                }

                return true
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                    val itemView = viewHolder?.itemView
                    val context = itemView?.context
                    if (context != null) {
                        itemView.setBackgroundColor(ThemeUtils.getColor(context, R.attr.listItemSelected))
                    }
                }
                super.onSelectedChanged(viewHolder, actionState)
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.background = null
            }

            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int = 0
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
        })
    }

    companion object {
        private fun <C, I : BlockItem<C>> createDiffUtils() = object : DiffUtil.ItemCallback<I>() {
            override fun areItemsTheSame(oldItem: I, newItem: I): Boolean {
                return oldItem.javaClass == newItem.javaClass && oldItem.areItemsTheSame(newItem)
            }

            override fun areContentsTheSame(oldItem: I, newItem: I): Boolean =
                oldItem.areContentsTheSame(newItem)

            override fun getChangePayload(oldItem: I, newItem: I): Any = newItem
        }
    }

}