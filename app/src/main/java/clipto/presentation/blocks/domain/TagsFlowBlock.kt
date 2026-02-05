package clipto.presentation.blocks.domain

import android.view.View
import android.widget.ScrollView
import androidx.core.view.children
import clipto.cache.AppColorCache
import clipto.domain.Filter
import clipto.extensions.getTagChipColor
import clipto.presentation.common.StyleHelper
import clipto.presentation.common.recyclerview.BlockItem
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.wb.clipboard.R

class TagsFlowBlock<C>(
    val tags: List<Filter>,
    val selectedTagIds: List<String>,
    val onClicked: (tag: Filter) -> Unit
) : BlockItem<C>(), View.OnClickListener {

    override val layoutRes: Int = R.layout.block_tags_flow

    override fun areContentsTheSame(item: BlockItem<C>): Boolean {
        return item is TagsFlowBlock<*> &&
                item.selectedTagIds == selectedTagIds &&
                item.tags == tags
    }

    override fun onBind(context: C, block: View) {
        block as ChipGroup
        tags.forEach { createTag(block, it) }
    }

    override fun onClick(v: View?) {
        val ref = v?.tag
        if (ref is Filter) {
            onClicked(ref)
        }
    }

    private fun createTag(block: ChipGroup, tag: Filter) {
        val name = StyleHelper.getFilterLabel(block.context, tag.name!!, tag.notesCount)
        createTag(block, tag, name)
    }

    private fun createTag(block: ChipGroup, tag: Filter, name: CharSequence): Chip {
        val tagItem = findSameTag(block, tag) ?: run {
            val context = block.context
            val item = ScrollView.inflate(context, R.layout.chip_filter_tag, null) as Chip
            tag.getTagChipColor(context)?.let { AppColorCache.updateColor(item, it) }
            item.setOnClickListener(this)
            item.text = name
            item.tag = tag
            block.addView(item)
            item
        }
        tagItem.isChecked = selectedTagIds.contains(tag.uid)

        return tagItem
    }

    private fun findSameTag(block: ChipGroup, tag: Filter): Chip? = block.children.find { it is Chip && it.tag == tag } as Chip?

}