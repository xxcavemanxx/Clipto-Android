package clipto.presentation.blocks

import android.view.View
import clipto.extensions.enableWithAlpha
import clipto.presentation.common.recyclerview.BlockItem
import com.google.android.material.button.MaterialButton
import com.wb.clipboard.R

class PrimaryButtonBlock<C>(
    private val titleRes: Int,
    private val clickListener: View.OnClickListener,
    private val longClickListener: View.OnLongClickListener? = null,
    private val enabled: Boolean = true
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_button_primary

    override fun areItemsTheSame(item: BlockItem<C>): Boolean =
        item is PrimaryButtonBlock && titleRes == item.titleRes

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is PrimaryButtonBlock
                && titleRes == item.titleRes
                && enabled == item.enabled

    override fun onBind(context: C, block: View) {
        block as MaterialButton
        block.setText(titleRes)
        block.isEnabled = enabled
        block.setOnClickListener(clickListener)
        block.setOnLongClickListener(longClickListener)
        block.enableWithAlpha(enabled)
    }

}