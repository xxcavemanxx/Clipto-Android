package clipto.presentation.runes

import com.wb.clipboard.databinding.ItemRuneBinding
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

        val binding = ItemRuneBinding.bind(itemView)
        var runeItem: RuneItem? = null

        init {
            binding.contentView.tag = this
            binding.contentView.setOnClickListener(this@RunesAdapter.debounce())
        }

        fun bindTo(runeItem: RuneItem?) {
            this.runeItem = runeItem
            runeItem?.rune?.let {
                val isActive = runeItem.isActive
                val iconColor = it.getIconColor(context, isActive)
                val textColor = it.getTextColor(context, isActive)
                val bgColor = it.getBgColor(context, isActive)

                // name
                binding.nameView.setTextColor(textColor)
                binding.nameView.text = it.getTitle()

                // icon
                binding.iconView.setImageResource(it.getIcon())
                binding.iconView.imageTintList = ColorStateList.valueOf(iconColor)

                // background
                binding.bgView.imageTintList = ColorStateList.valueOf(bgColor)

                // warning
                binding.warningView.setVisibleOrGone(runeItem.hasWarning)
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
