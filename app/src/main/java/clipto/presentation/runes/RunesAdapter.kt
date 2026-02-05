package clipto.presentation.runes

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import clipto.common.extensions.debounce
import clipto.common.extensions.setVisibleOrGone
import clipto.domain.IRune
import clipto.presentation.runes.extensions.getBgColor
import clipto.presentation.runes.extensions.getIconColor
import clipto.presentation.runes.extensions.getTextColor
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.item_rune.view.*

class RunesAdapter(
    val context: Context,
    private val onClickListener: (rune: IRune) -> Unit
) : ListAdapter<RuneItem, RunesAdapter.ViewHolder>(diffUtils), View.OnClickListener {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).rune.getId().hashCode().toLong()
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bindTo(getItem(position))
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(parent)

    override fun onClick(v: View?) {
        val viewHolder = v?.tag
        if (viewHolder is ViewHolder) {
            viewHolder.runeItem?.let { onClickListener.invoke(it.rune) }
        }
    }

    inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_rune, parent, false)
    ) {

        var runeItem: RuneItem? = null

        init {
            itemView.contentView.tag = this
            itemView.contentView.setOnClickListener(this@RunesAdapter.debounce())
        }

        fun bindTo(runeItem: RuneItem?) {
            this.runeItem = runeItem
            runeItem?.rune?.let {
                val isActive = runeItem.isActive
                val iconColor = it.getIconColor(context, isActive)
                val textColor = it.getTextColor(context, isActive)
                val bgColor = it.getBgColor(context, isActive)

                // name
                itemView.nameView.setTextColor(textColor)
                itemView.nameView.text = it.getTitle()

                // icon
                itemView.iconView.setImageResource(it.getIcon())
                itemView.iconView.imageTintList = ColorStateList.valueOf(iconColor)

                // background
                itemView.bgView.imageTintList = ColorStateList.valueOf(bgColor)

                // warning
                itemView.warningView.setVisibleOrGone(runeItem.hasWarning)
            }
        }
    }

    companion object {
        private val diffUtils = object : DiffUtil.ItemCallback<RuneItem>() {
            override fun areItemsTheSame(oldItem: RuneItem, newItem: RuneItem): Boolean {
                return oldItem.rune.getId() == newItem.rune.getId()
            }

            override fun areContentsTheSame(oldItem: RuneItem, newItem: RuneItem): Boolean {
                return oldItem.isActive == newItem.isActive && oldItem.hasWarning == newItem.hasWarning
            }

            override fun getChangePayload(oldItem: RuneItem, newItem: RuneItem): Any = newItem
        }
    }
}