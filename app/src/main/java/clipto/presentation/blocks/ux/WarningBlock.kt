package clipto.presentation.blocks.ux

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import clipto.extensions.TextTypeExt
import clipto.extensions.getColorNegative
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class WarningBlock<C>(
    private val titleRes: Int = 0,
    private val title: String? = null,
    private val textColor: Int? = null,
    private val backgroundColor: Int? = null,
    private val actionIcon: Int = R.drawable.bg_warning_more,
    private val clickListener: View.OnClickListener? = null
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_warning

    override fun areItemsTheSame(item: BlockItem<C>): Boolean =
        item is WarningBlock
                && title == item.title
                && titleRes == item.titleRes

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is WarningBlock
                && textColor == textColor
                && actionIcon == item.actionIcon
                && backgroundColor == item.backgroundColor

    override fun onBind(context: C, block: View) {
        block as TextView
        if (title != null) {
            TextTypeExt.MARKDOWN.apply(block, title)
        } else {
            block.setText(titleRes)
        }
        val textColorRef = textColor ?: Color.WHITE
        val backgroundColorRef = backgroundColor ?: block.context.getColorNegative()
        block.backgroundTintList = ColorStateList.valueOf(backgroundColorRef)
        block.setOnClickListener(clickListener)
        block.setTextColor(textColorRef)

        if (clickListener != null) {
            TextViewCompat.setCompoundDrawableTintList(block, ColorStateList.valueOf(textColorRef))
            block.isClickable = true
        } else {
            block.isClickable = false
        }
        block.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, actionIcon, 0)
    }

}