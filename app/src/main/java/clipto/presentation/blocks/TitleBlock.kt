package clipto.presentation.blocks

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import clipto.extensions.getTextColorPrimary
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class TitleBlock<C>(
    val titleRes: Int = 0,
    val title: String? = null,
    val textSize: Int = 16,
    val textColor: Int? = null,
    val rightIconRes: Int = 0,
    val isEnabled: Boolean = true,
    val gravity: Int = Gravity.START,
    val width: Int = ViewGroup.LayoutParams.WRAP_CONTENT
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_title

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is TitleBlock &&
                item.titleRes == titleRes &&
                item.textSize == textSize &&
                item.textColor == textColor &&
                item.rightIconRes == rightIconRes &&
                item.isEnabled == isEnabled &&
                item.title == title &&
                item.gravity == gravity &&
                item.width == width

    override fun onBind(context: C, block: View) {
        block as TextView
        block.textSize = textSize.toFloat()
        block.text = title ?: block.context.getString(titleRes)
        block.setTextColor(textColor ?: block.context.getTextColorPrimary())
        block.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, rightIconRes, 0)
        block.gravity = gravity or Gravity.CENTER_VERTICAL
        block.layoutParams?.width = width
        block.isEnabled = isEnabled
    }

}