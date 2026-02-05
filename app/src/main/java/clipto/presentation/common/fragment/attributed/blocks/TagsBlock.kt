package clipto.presentation.common.fragment.attributed.blocks

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.fragment.app.Fragment
import clipto.cache.AppColorCache
import clipto.common.extensions.doOnFirstLayout
import clipto.common.extensions.setVisibleOrGone
import clipto.domain.*
import clipto.extensions.getTagChipColor
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.view.DoubleClickListenerWrapper
import clipto.store.filter.FilterDetailsState
import com.google.android.material.chip.Chip
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_attributed_object_tags.view.*

class TagsBlock<O : AttributedObject, S : AttributedObjectScreenState<O>>(
    private val screenState: S,
    private val filterDetailsState: FilterDetailsState,
    private val onRemoveTag: (tag: Filter) -> Unit,
    private val tagIds: List<String> = screenState.value.tagIds,
    private val newTagIds: List<String>? = screenState.value.newTagIds,
    private val excludedTagIds: Set<String> = screenState.value.excludedTagIds,
    private val onEdit: () -> Unit = {},
    private val bgColorAttr: Int = R.attr.colorPrimaryInverse
) : BlockItem<Fragment>(), View.OnLongClickListener, View.OnClickListener {

    override val layoutRes: Int = R.layout.block_attributed_object_tags

    private val onDeleteTagListener by lazy {
        View.OnClickListener {
            val tag = it.tag
            val viewGroup = it.parent as ViewGroup
            viewGroup.removeView(it)
            if (tag is Filter) {
                getTagsBlock(viewGroup)?.onRemoveTag?.invoke(tag)
            }
        }
    }

    override fun areContentsTheSame(item: BlockItem<Fragment>): Boolean =
        item is TagsBlock<*, *>
                && screenState.viewMode == item.screenState.viewMode
                && tagIds == item.screenState.value.tagIds
                && newTagIds == item.screenState.value.newTagIds
                && excludedTagIds == item.screenState.value.excludedTagIds
                && bgColorAttr == item.bgColorAttr

    override fun onLongClick(v: View?): Boolean {
        val tag = v?.tag
        if (tag is Filter) {
            filterDetailsState.requestOpenFilter(tag)
        }
        return true
    }

    override fun onClick(v: View?) {
        val tag = v?.tag
        if (tag is Filter) {
            filterDetailsState.requestOpenFilter(tag)
        }
    }

    override fun onInit(fragment: Fragment, block: View) {
        val tagsGroup = block.cgTags
        block.vMode.setOnClickListener(
            DoubleClickListenerWrapper(block.context,
                { getScreenState(block).acceptDoubleClick() },
                {
                    getScreenState(block)?.whenCanBeEdited()?.let {
                        getTagsBlock(block)?.onEdit?.invoke()
                    }
                }
            )
        )

        filterDetailsState.filterState.requestUpdateFilter.getLiveChanges(block) {
            updateFilter(tagsGroup, it)
        }

        block.doOnFirstLayout {
            val width = block.width
            block.vMode.takeIf { width > 0 && it.minimumWidth != width }?.minimumWidth = width
        }
    }

    override fun onBind(fragment: Fragment, block: View) {
        val tagsGroup = block.cgTags
        tagsGroup.tag = this
        block.tag = this
        val editMode = screenState.isEditMode()
        block.vMode.setVisibleOrGone(!editMode)
        screenState.value.getTags(noExcluded = true).let { tags ->
            val unusedCount = tagsGroup.childCount - tags.size - 1
            if (unusedCount > 0) tagsGroup.removeViewsInLayout(0, unusedCount)
            tags.forEachIndexed { index, tag -> createTag(tagsGroup, index, tag, editMode) }
        }
    }

    private fun createTag(tagsGroup: ViewGroup, index: Int, tag: Filter, editMode: Boolean) {
        val context = tagsGroup.context
        val chip = tagsGroup.takeIf { index < it.childCount - 1 }?.getChildAt(index)
        if (chip !is Chip) {
            val newChip = FrameLayout.inflate(context, R.layout.chip_clip_tag, null) as Chip
            tag.getTagChipColor(context, true)?.let { AppColorCache.updateColor(newChip, it, bgColorAttr = bgColorAttr) }
            newChip.setOnCloseIconClickListener(onDeleteTagListener)
            newChip.setOnLongClickListener(this)
            newChip.setOnClickListener(this)
            newChip.isCloseIconVisible = editMode
            newChip.text = tag.name
            newChip.tag = tag
            tagsGroup.addView(newChip, tagsGroup.childCount - 1)
        } else {
            tag.getTagChipColor(context, true)?.let { AppColorCache.updateColor(chip, it, bgColorAttr = bgColorAttr) }
            chip.isCloseIconVisible = editMode
            chip.text = tag.name
            chip.tag = tag
        }
    }

    private fun updateFilter(tagsGroup: ViewGroup, tag: Filter) {
        findSameTag(tagsGroup, tag)?.let { chip ->
            chip.text = tag.name
            tag.getTagChipColor(tagsGroup.context, true)?.let { AppColorCache.updateColor(chip, it, bgColorAttr = bgColorAttr) }
        }
    }

    private fun findSameTag(tagsGroup: ViewGroup, tag: Filter): Chip? = tagsGroup.children.find { it is Chip && it.tag == tag } as Chip?

    private fun getTagsBlock(block: View?): TagsBlock<*, *>? {
        val tag = block?.tag
        if (tag is TagsBlock<*, *>) {
            return tag
        }
        return null
    }

    private fun getScreenState(block: View?): AttributedObjectScreenState<*>? {
        return getTagsBlock(block)?.screenState
    }

}