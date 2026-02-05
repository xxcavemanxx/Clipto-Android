package clipto.presentation.blocks

import android.view.View
import clipto.extensions.enableWithAlpha
import clipto.presentation.common.recyclerview.BlockItem
import com.google.android.material.button.MaterialButton
import com.wb.clipboard.R

class OutlinedButtonBlock<C>(
    private val titleRes: Int = 0,
    private val title: String? = null,
    private val enabled: Boolean = true,
    private val clickListener: View.OnClickListener,
    private val longClickListener: View.OnLongClickListener? = null
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_button_outlined

    override fun areItemsTheSame(item: BlockItem<C>): Boolean =
        item is OutlinedButtonBlock && titleRes == item.titleRes

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is OutlinedButtonBlock
                && enabled == item.enabled
                && title == item.title

    override fun onBind(context: C, block: View) {
        block as MaterialButton
        if (titleRes != 0) {
            block.setText(titleRes)
        } else {
            block.text = title
        }
        block.isEnabled = enabled
        block.setOnClickListener(clickListener)
        block.setOnLongClickListener(longClickListener)
        block.enableWithAlpha(enabled)
    }

}