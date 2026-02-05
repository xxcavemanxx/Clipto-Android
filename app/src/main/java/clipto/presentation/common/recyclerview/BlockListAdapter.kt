package clipto.presentation.common.recyclerview

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import clipto.common.misc.ThemeUtils
import com.wb.clipboard.R
import java.util.*

class BlockListAdapter<C>(val context: C) : ListAdapter<BlockItem<C>, BlockItemViewHolder<C, BlockItem<C>>>(createDiffUtils()) {

    var listSnapshot: List<BlockItem<C>>? = null

    override fun getItemViewType(position: Int): Int = getItem(position).layoutRes

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockItemViewHolder<C, BlockItem<C>> {
        return BlockItemViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BlockItemViewHolder<C, BlockItem<C>>, position: Int) {
        val block = getItem(position)
        holder.bind(context, block)
    }

    override fun submitList(list: List<BlockItem<C>>?) {
        listSnapshot = list
        super.submitList(list)
    }

    override fun submitList(list: List<BlockItem<C>>?, commitCallback: Runnable?) {
        listSnapshot = list
        super.submitList(list, commitCallback)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun requestLayout() {
        notifyDataSetChanged()
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
                val currentBlocks = listSnapshot ?: return false

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
        private fun <C> createDiffUtils() = object : DiffUtil.ItemCallback<BlockItem<C>>() {
            override fun areItemsTheSame(oldItem: BlockItem<C>, newItem: BlockItem<C>): Boolean {
                return oldItem.javaClass == newItem.javaClass && oldItem.areItemsTheSame(newItem)
            }

            override fun areContentsTheSame(oldItem: BlockItem<C>, newItem: BlockItem<C>): Boolean =
                oldItem.areContentsTheSame(newItem)

            override fun getChangePayload(oldItem: BlockItem<C>, newItem: BlockItem<C>): Any = newItem
        }
    }

}