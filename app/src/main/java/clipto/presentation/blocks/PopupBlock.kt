package clipto.presentation.blocks

import android.content.res.ColorStateList
import android.view.View
import androidx.core.widget.TextViewCompat
import clipto.extensions.getTextColorPrimary
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_popup.view.*

class PopupBlock<C>(
    private val value: String,
    private val enabled: Boolean = true,
    private val clickListener: View.OnClickListener
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_popup

    override fun areItemsTheSame(item: BlockItem<C>): Boolean =
        item is PopupBlock && value == item.value

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is PopupBlock && enabled == item.enabled

    override fun onBind(context: C, block: View) {
        val color =
            if (enabled) {
                block.context.getTextColorPrimary()
            } else {
                block.context.getTextColorSecondary()
            }
        val titleView = block.titleView
        TextViewCompat.setCompoundDrawableTintList(titleView, ColorStateList.valueOf(color))
        titleView.isEnabled = enabled
        titleView.setTextColor(color)
        titleView.text = value
        if (enabled) {
            titleView.setOnClickListener(clickListener)
        } else {
            titleView.setOnClickListener(null)
        }
    }

}