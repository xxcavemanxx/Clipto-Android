package clipto.presentation.config

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import clipto.domain.Font
import com.google.android.material.chip.Chip
import com.wb.clipboard.R

class TextFontAdapter(
        val context: Context,
        val clickHandler: (item: TextFontItem) -> Unit
) : ListAdapter<TextFontItem, TextFontAdapter.ViewHolder>(diffCallback) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bindTo(getItem(position))
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(parent)

    inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.chip_text_font, parent, false)) {

        var item: TextFontItem? = null

        val chip: Chip = itemView as Chip

        init {
            itemView.setOnClickListener {
                item?.let { item ->
                    if (item.font != Font.MORE) {
                        currentList
                                .map { it.copy(selected = it.font == item.font) }
                                .let { changedList -> submitList(changedList) }
                        chip.isChecked = true
                    }
                    clickHandler.invoke(item)
                }
            }
        }

        fun bindTo(item: TextFontItem?) {
            this.item = item
            item?.let {
                chip.text = context.getString(it.font.titleRes)
                chip.isCheckable = item.font != Font.MORE
                chip.isChecked = it.selected
            }
        }
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<TextFontItem>() {
            override fun areItemsTheSame(oldItem: TextFontItem, newItem: TextFontItem): Boolean =
                    oldItem.font == newItem.font

            override fun areContentsTheSame(oldItem: TextFontItem, newItem: TextFontItem): Boolean =
                    oldItem.selected == newItem.selected

            override fun getChangePayload(oldItem: TextFontItem, newItem: TextFontItem): Any = newItem
        }
    }

}