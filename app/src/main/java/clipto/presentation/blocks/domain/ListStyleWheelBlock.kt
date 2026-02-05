package clipto.presentation.blocks.domain

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clipto.domain.ListStyle
import clipto.extensions.getTitleRes
import clipto.presentation.common.recyclerview.BlockItem
import com.google.android.material.chip.Chip
import com.wb.clipboard.R

class ListStyleWheelBlock<C>(
    private val listStyle: ListStyle,
    private val onChanged: (style: ListStyle) -> Unit,
    private val listStyles: List<ListStyle> = ListStyle.NOTE_STYLES
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_list_style_wheel

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is ListStyleWheelBlock<*> &&
                item.listStyle == listStyle

    override fun onInit(context: C, block: View) {
        block as RecyclerView

        val ctx = block.context
        val listStyleItems = listStyles.map { ListStyleItem(it, it == listStyle) }
        val adapter = ListStyleAdapter(ctx, listStyleItems) {
            val ref = block.tag
            if (ref is ListStyleWheelBlock<*>) {
                ref.onChanged(it.style)
            }
        }
        block.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
        block.adapter = adapter
    }

    override fun onBind(context: C, block: View) {
        block as RecyclerView
        val adapter = block.adapter as ListStyleAdapter
        val indexOfActiveStyle = adapter.items.indexOfFirst { it.style == listStyle }
        adapter.items.forEachIndexed { index, item -> item.selected = index == indexOfActiveStyle }
        if (indexOfActiveStyle != -1) block.smoothScrollToPosition(indexOfActiveStyle)
        adapter.notifyDataSetChanged()
        block.tag = this
    }

}

private data class ListStyleItem(
    val style: ListStyle,
    var selected: Boolean
)

private class ListStyleAdapter(
    val context: Context,
    val items: List<ListStyleItem>,
    val clickHandler: (item: ListStyleItem) -> Unit
) : RecyclerView.Adapter<ListStyleAdapter.ViewHolder>() {

    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bindTo(items[position])
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(parent)

    inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.chip_list_style, parent, false)
    ) {

        var item: ListStyleItem? = null

        val chip: Chip = itemView as Chip

        init {
            itemView.setOnClickListener {
                item?.let { item ->
                    item.selected = true
                    items.forEach { if (it != item) it.selected = false }
                    clickHandler.invoke(item)
                    notifyDataSetChanged()
                }
            }
        }

        fun bindTo(item: ListStyleItem?) {
            this.item = item
            item?.let {
                chip.text = context.getString(it.style.getTitleRes())
                chip.isChecked = it.selected
            }
        }
    }

}