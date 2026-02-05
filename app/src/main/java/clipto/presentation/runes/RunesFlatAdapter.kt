package clipto.presentation.runes

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import clipto.common.extensions.setVisibleOrGone
import clipto.common.logging.L
import clipto.presentation.runes.extensions.getBgColor
import clipto.presentation.runes.extensions.getIconColor
import clipto.presentation.runes.extensions.getTextColor
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.item_rune_flat.view.*

class RunesFlatAdapter(
        val fragment: RunesFragment
) : ListAdapter<RuneFlatItem, RunesFlatAdapter.ViewHolder>(diffUtils) {

    private val context = fragment.requireContext()
    private val viewModel by lazy { fragment.viewModel }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bindTo(getItem(position))
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(parent)

    inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_rune_flat, parent, false)) {

        var runeItem: RuneFlatItem? = null

        init {
            itemView.tag = this
            itemView.vExpandCollapse.setOnClickListener {
                runeItem?.rune?.let { rune ->
                    rune.setExpanded(!rune.isExpanded())
                    bindTo(runeItem)
                }
            }
            itemView.ivHint.setOnClickListener { runeItem?.rune?.let { viewModel.onShowHint(it) } }
        }

        fun bindTo(runeItem: RuneFlatItem?) {
            L.log(this, "BIND TO :: {}", runeItem?.rune?.getId())
            this.runeItem = runeItem
            runeItem?.rune?.let { rune ->
                val isActive = runeItem.isActive
                val isExpanded = rune.isExpanded()
                val iconColor = rune.getIconColor(context, isActive)
                val textColor = rune.getTextColor(context, isActive)
                val bgColor = rune.getBgColor(context, isActive)

                // name
                itemView.nameView.setTextColor(textColor)
                itemView.nameView.text = rune.getTitle()

                // icon
                itemView.iconView.setImageResource(rune.getIcon())
                itemView.iconView.imageTintList = ColorStateList.valueOf(iconColor)

                // background
                itemView.bgView.imageTintList = ColorStateList.valueOf(bgColor)

                // warning
                itemView.warningView.setVisibleOrGone(runeItem.hasWarning)

                // expand state
                itemView.ivExpand.setImageResource(if (isExpanded) R.drawable.texpander_collapse else R.drawable.texpander_expand)

                // settings
                itemView.rvRuneSettings.let { rv ->
                    rv.setVisibleOrGone(isExpanded)
                    if (!isExpanded) {
                        rv.adapter = null
                    } else {
                        rune as RuneSettingsProvider
                        rune.bind(rv, fragment)
                    }
                }
            }
        }
    }

    companion object {
        private val diffUtils = object : DiffUtil.ItemCallback<RuneFlatItem>() {
            override fun areItemsTheSame(oldItem: RuneFlatItem, newItem: RuneFlatItem): Boolean {
                return oldItem.rune.getId() == newItem.rune.getId()
            }

            override fun areContentsTheSame(oldItem: RuneFlatItem, newItem: RuneFlatItem): Boolean {
                return !oldItem.rune.isExpanded()
                        && oldItem.isActive == newItem.isActive
                        && oldItem.expanded == newItem.expanded
                        && oldItem.hasWarning == newItem.hasWarning
            }

            override fun getChangePayload(oldItem: RuneFlatItem, newItem: RuneFlatItem): Any = newItem
        }
    }
}