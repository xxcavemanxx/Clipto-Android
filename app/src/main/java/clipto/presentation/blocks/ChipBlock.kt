package clipto.presentation.blocks

import android.view.View
import clipto.cache.AppColorCache
import clipto.common.misc.Units
import clipto.presentation.common.recyclerview.BlockItem
import com.google.android.material.chip.Chip
import com.wb.clipboard.R

class ChipBlock<T, C>(
    val model: T,
    val checked: Boolean,
    val iconRes: Int? = null,
    val title: CharSequence?,
    val checkable: Boolean = true,
    val backgroundColor: Int? = null,
    val minHeight: Float? = null,
    val cornerRadius: Float? = null,
    val onClicked: (checked: Boolean) -> Unit
) : BlockItem<C>(), View.OnClickListener {

    override val layoutRes: Int = R.layout.block_chip

    private fun click(block: View) {
        block as Chip
        onClicked.invoke(block.isChecked)
    }

    override fun onClick(v: View?) {
        val blockRef = v?.tag
        if (blockRef is ChipBlock<*, *>) {
            blockRef.click(v)
        }
    }

    override fun areItemsTheSame(item: BlockItem<C>): Boolean {
        return super.areItemsTheSame(item) && item is ChipBlock<*, *> && item.model == model
    }

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is ChipBlock<*, *> &&
                item.backgroundColor == backgroundColor &&
                item.checkable == checkable &&
                item.iconRes == iconRes &&
                item.checked == checked &&
                item.title == title &&
                item.minHeight == minHeight &&
                item.cornerRadius == cornerRadius

    override fun onBind(context: C, block: View) {
        block as Chip
        block.tag = null
        block.text = title
        block.setOnClickListener(this)
        minHeight?.let { Units.DP.toPx(it) }?.let { minHeight ->
            if (block.chipMinHeight != minHeight) {
                block.chipMinHeight = minHeight
            }
        }
        cornerRadius?.let { Units.DP.toPx(it) }?.let { cornerRadius ->
            if (block.chipCornerRadius != cornerRadius) {
                block.chipCornerRadius = cornerRadius
                block.shapeAppearanceModel.withCornerSize(cornerRadius)
            }
        }
        if (block.isCheckable != checkable) {
            block.isCheckable = checkable
        }
        if (iconRes != null) {
            block.setChipIconResource(iconRes)
        } else {
            block.chipIcon = null
        }
        if (backgroundColor != null) {
            block.chipStrokeWidth = Units.DP.toPx(2f)
            AppColorCache.updateColor(block, backgroundColor)
        } else {
            block.chipStrokeWidth = 0f
            block.setChipBackgroundColorResource(R.color.color_chip_checkable)
        }
        if (!checkable) {
            block.isSelected = checked
        } else {
            block.isSelected = false
        }
        block.isChecked = checked
        block.tag = this
    }

}