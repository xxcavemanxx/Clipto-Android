package clipto.presentation.blocks

import com.wb.clipboard.databinding.BlockHeaderBinding
import android.view.View
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class HeaderBlock<C>(
    private val titleRes: Int
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_header

    override fun areItemsTheSame(item: BlockItem<C>): Boolean =
        item is HeaderBlock && titleRes == item.titleRes

    override fun onBind(context: C, block: View) {
        val binding = BlockHeaderBinding.bind(block)
        binding.titleView?.setText(titleRes)
    }

}
