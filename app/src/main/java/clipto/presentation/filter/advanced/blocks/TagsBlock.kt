package clipto.presentation.filter.advanced.blocks

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.children
import clipto.cache.AppColorCache
import clipto.domain.Filter
import clipto.extensions.getTagChipColor
import clipto.presentation.common.StyleHelper
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.filter.advanced.AdvancedFilterFragment
import clipto.presentation.filter.advanced.AdvancedFilterViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.wb.clipboard.R

class TagsBlock(
        private val viewModel: AdvancedFilterViewModel,
        private val filter: Filter,
        private val tagIds: List<String> = filter.tagIds,
        private val onClicked: (filter: Filter, checked: Boolean) -> Unit
) : BlockItem<AdvancedFilterFragment>(), View.OnClickListener {

    override val layoutRes: Int = R.layout.block_advanced_filter_tags

    override fun areContentsTheSame(item: BlockItem<AdvancedFilterFragment>): Boolean =
            item is TagsBlock && item.tagIds == tagIds

    override fun onBind(fragment: AdvancedFilterFragment, block: View) {
        val tagsGroup = block as ChipGroup
        tagsGroup.tag = this
        viewModel.getTags(filter).forEach { tag -> createTag(tagsGroup, tag) }
        tagsGroup.children.forEach {
            it as Chip
            val tag = it.tag as Filter
            it.isChecked = tagIds.contains(tag.uid)
        }
    }

    override fun onClick(v: View?) {
        if (v is Chip) {
            val tag = v.tag
            val block = (v.parent as? ViewGroup)?.tag
            if (tag is Filter && block is TagsBlock) {
                block.onClicked.invoke(tag, v.isChecked)
            }
        }
    }

    private fun createTag(tagsGroup: ViewGroup, tag: Filter) {
        val context = tagsGroup.context
        val chip = findSameTag(tagsGroup, tag)
        if (chip !is Chip) {
            val newChip = FrameLayout.inflate(context, R.layout.chip_filter_tag, null) as Chip
            tag.getTagChipColor(context, true)?.let { AppColorCache.updateColor(newChip, it) }
            newChip.setOnClickListener(this)
            newChip.text = StyleHelper.getFilterLabel(context, tag)
            newChip.tag = tag
            tagsGroup.addView(newChip)
        } else {
            tag.getTagChipColor(context, true)?.let { AppColorCache.updateColor(chip, it) }
            chip.text = StyleHelper.getFilterLabel(context, tag)
            chip.tag = tag
        }
    }

    private fun findSameTag(tagsGroup: ViewGroup, tag: Filter): Chip? = tagsGroup.children.find { it is Chip && it.tag == tag } as Chip?

}