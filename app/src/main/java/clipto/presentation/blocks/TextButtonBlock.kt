package clipto.presentation.blocks

import android.view.View
import clipto.extensions.getTextColorAccent
import clipto.presentation.common.recyclerview.BlockItem
import com.google.android.material.button.MaterialButton
import com.wb.clipboard.R

class TextButtonBlock<C>(
    private val titleRes: Int,
    private val textColor: Int? = null,
    private val clickListener: View.OnClickListener,
    private val longClickListener: View.OnLongClickListener? = null
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_button_text

    override fun areItemsTheSame(item: BlockItem<C>): Boolean =
        item is TextButtonBlock &&
                titleRes == item.titleRes &&
                textColor == item.textColor

    override fun onBind(context: C, block: View) {
        block as MaterialButton
        block.setText(titleRes)
        block.setOnClickListener(clickListener)
        block.setOnLongClickListener(longClickListener)
        block.setTextColor(textColor ?: block.context.getTextColorAccent())
    }

}