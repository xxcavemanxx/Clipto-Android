package clipto.presentation.blocks.layout

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.children
import clipto.presentation.blocks.ChipBlock
import clipto.presentation.common.recyclerview.BlockItem
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.wb.clipboard.R

class ChipsFlowBlock<T, C>(
    private val items: List<ChipBlock<T, C>>,
    private val modelType: Class<T>
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_chips_flow

    override fun areItemsTheSame(item: BlockItem<C>): Boolean {
        return super.areItemsTheSame(item) &&
                item is ChipsFlowBlock<*, *> &&
                item.modelType == modelType
    }

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is ChipsFlowBlock<*, *> &&
                item.items == items

    override fun onBind(context: C, block: View) {
        block as ChipGroup
        items.forEach { createChip(context, block, it) }
        block.children.iterator().forEach {
            it as Chip
            val model = (it.tag as? ChipBlock<*, *>)?.model
            if (model == null || !modelType.isAssignableFrom(model.javaClass)) {
                block.removeViewInLayout(it)
            } else {
                it.isChecked = items.any { it.model == model }
            }
        }
    }

    private fun createChip(context: C, group: ViewGroup, item: ChipBlock<T, C>) {
        val ctx = group.context
        var chip = findSameTag(group, item)
        if (chip !is Chip) {
            val newChip = FrameLayout.inflate(ctx, R.layout.block_chip, null) as Chip
            group.addView(newChip)
            chip = newChip
        }
        item.onBind(context, chip)
    }

    private fun findSameTag(group: ViewGroup, item: ChipBlock<T, C>): Chip? = group.children
        .find {
            val tag = it.tag
            tag is ChipBlock<*, *> && item.model == tag.model
        } as Chip?

}