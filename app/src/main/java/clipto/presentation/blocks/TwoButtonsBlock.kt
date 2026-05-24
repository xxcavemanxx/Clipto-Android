package clipto.presentation.blocks

import com.wb.clipboard.databinding.BlockButtonsTwoBinding
import android.view.View
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

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
        val binding = BlockButtonsTwoBinding.bind(block)
        binding.primaryButton.setText(primaryTitleRes)
        binding.primaryButton.setOnClickListener(primaryClickListener)
        binding.primaryButton.isEnabled = enabled
        binding.secondaryButton.setText(secondaryTitleRes)
        binding.secondaryButton.setOnClickListener(secondaryClickListener)
        binding.secondaryButton.isEnabled = enabled
    }

}
