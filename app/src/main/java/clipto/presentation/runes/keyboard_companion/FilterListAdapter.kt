package clipto.presentation.runes.keyboard_companion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import clipto.cache.AppColorCache
import clipto.common.misc.ThemeUtils
import clipto.domain.Filter
import clipto.extensions.getTagChipColor
import clipto.extensions.getTitle
import clipto.presentation.common.StyleHelper
import clipto.presentation.runes.keyboard_companion.data.FilterData
import com.google.android.material.chip.Chip
import com.wb.clipboard.*
import kotlinx.android.synthetic.main.chip_filter_texpander.view.*

class FilterListAdapter(
        private val onFilterClicked: (filter: Filter) -> Unit
) : ListAdapter<FilterData, FilterListAdapter.FilterViewHolder>(diffUtils), View.OnClickListener {

    override fun onClick(v: View?) {
        val holder = v?.tag
        if (holder is FilterViewHolder) {
            holder.filterData?.let {
                it.isActive = !it.isActive
                onFilterClicked.invoke(it.filter)
                holder.bind(it)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterListAdapter.FilterViewHolder {
        return FilterViewHolder(parent)
    }

    override fun onBindViewHolder(holder: FilterListAdapter.FilterViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class FilterViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.chip_filter_texpander, parent, false)) {

        var filterData: FilterData? = null

        init {
            itemView.chipView.tag = this
            itemView.chipView.setOnClickListener(this@FilterListAdapter)
        }

        fun bind(filterData: FilterData) {
            this.filterData = filterData
            val filterItem = itemView.chipView as Chip
            val context = filterItem.context
            val filter = filterData.filter
            val color = filter.getTagChipColor(context)
            if (color != null) {
                AppColorCache.updateColor(filterItem, color = color, bgColorAttr = R.attr.colorContext)
            } else {
                val strokeColor = ThemeUtils.getColor(context, android.R.attr.textColorPrimary)
                val bgColor = ThemeUtils.getColor(context, R.attr.actionIconColorHighlight)
                AppColorCache.updateColor(filterItem, strokeColor, bgColor, R.attr.colorContext)
            }
            filterItem.text =
                    if (filter.type != Filter.Type.TAG) {
                        filter.getTitle(context)
                    } else {
                        StyleHelper.getFilterLabel(context, filter) ?: ""
                    }
            filterItem.isSelected = filterData.isActive
        }
    }

    companion object {

        private val diffUtils = object : DiffUtil.ItemCallback<FilterData>() {
            override fun areItemsTheSame(oldItem: FilterData, newItem: FilterData): Boolean {
                return oldItem.filter.uid == newItem.filter.uid
            }

            override fun areContentsTheSame(oldItem: FilterData, newItem: FilterData): Boolean {
                return oldItem.isActive == newItem.isActive &&
                        oldItem.count == newItem.count &&
                        oldItem.iconColor == newItem.iconColor &&
                        oldItem.iconRes == newItem.iconRes
            }

            override fun getChangePayload(oldItem: FilterData, newItem: FilterData): Any = newItem
        }
    }

}