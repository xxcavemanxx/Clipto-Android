package clipto.presentation.blocks.domain

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clipto.domain.SortBy
import clipto.extensions.SortByExt
import clipto.extensions.toExt
import clipto.presentation.common.recyclerview.BlockItem
import com.google.android.material.chip.Chip
import com.wb.clipboard.R

class SortByWheelBlock<C>(
    val uid: String?,
    val sortBy: SortBy,
    val sortByItems: Array<SortByExt>,
    val onChanged: (sortBy: SortBy) -> Unit
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_sort_by_wheel

    override fun areItemsTheSame(item: BlockItem<C>): Boolean =
        item is SortByWheelBlock<*> &&
                item.uid == uid &&
                item.sortByItems.contentEquals(sortByItems)

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is SortByWheelBlock<*> && item.sortBy == sortBy

    override fun onInit(context: C, block: View) {
        block as RecyclerView
        val ctx = block.context
        val sortByItems = sortByItems
            .map {
                val sortByExt =
                    if (it.sortByOpposite == sortBy) {
                        sortBy.toExt()
                    } else {
                        it
                    }
                SortByItem(sortByExt, sortByExt.sortBy == sortBy)
            }
        val sortByAdapter = SortByAdapter(ctx, sortByItems) { onChanged(it.sortBy.sortBy) }
        block.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
        val indexOfActiveSortBy = sortByItems.indexOfFirst { it.sortBy.sortBy == sortBy }
        block.adapter = sortByAdapter
        if (indexOfActiveSortBy != -1) {
            block.smoothScrollToPosition(indexOfActiveSortBy)
        }
    }

    override fun onBind(context: C, block: View) {
        block as RecyclerView
        val adapter = block.adapter as SortByAdapter
        val index = adapter.items.indexOfFirst { it.sortBy.sortBy == sortBy || it.sortBy.sortByOpposite == sortBy }
        if (index != -1) adapter.notifyItemChanged(index)
    }

}

private class SortByAdapter(
    val context: Context,
    val items: List<SortByItem>,
    val clickHandler: (item: SortByItem) -> Unit
) : RecyclerView.Adapter<SortByAdapter.ViewHolder>() {

    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bindTo(items[position])
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(parent)

    inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.chip_sort_by, parent, false)
    ) {

        var item: SortByItem? = null

        val chip: Chip = itemView as Chip

        init {
            itemView.setOnClickListener {
                item?.let { item ->
                    if (item.selected) {
                        item.sortBy = item.sortBy.sortByOpposite.toExt()
                    }
                    item.selected = true
                    items.forEach { if (it != item) it.selected = false }
                    clickHandler.invoke(item)
                    notifyDataSetChanged()
                }
            }
        }

        fun bindTo(item: SortByItem?) {
            this.item = item
            item?.let {
                chip.text = context.getString(it.sortBy.titleRes)
                chip.isChecked = it.selected
                if (item.selected) {
                    val icon =
                        if (item.sortBy.desc) {
                            R.drawable.ic_sort_desc
                        } else {
                            R.drawable.ic_sort_asc
                        }
                    chip.setChipIconResource(icon)
                } else {
                    chip.chipIcon = null
                }
            }
        }
    }

}

private data class SortByItem(
    var sortBy: SortByExt,
    var selected: Boolean
)