package clipto.presentation.blocks

import android.view.View
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_buttons_two.view.*

class TwoButtonsBlock<C>(
    private val primaryTitleRes: Int,
    private val primaryClickListener: View.OnClickListener,
    private val secondaryTitleRes: Int,
    private val secondaryClickListener: View.OnClickListener,
    private val enabled: Boolean = true
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_buttons_two

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is TwoButtonsBlock
                && primaryTitleRes == item.primaryTitleRes
                && primaryClickListener == item.primaryClickListener
                && secondaryTitleRes == item.secondaryTitleRes
                && secondaryClickListener == item.secondaryClickListener
                && enabled == item.enabled

    override fun onBind(context: C, block: View) {
        block.primaryButton.setText(primaryTitleRes)
        block.primaryButton.setOnClickListener(primaryClickListener)
        block.primaryButton.isEnabled = enabled
        block.secondaryButton.setText(secondaryTitleRes)
        block.secondaryButton.setOnClickListener(secondaryClickListener)
        block.secondaryButton.isEnabled = enabled
    }

}