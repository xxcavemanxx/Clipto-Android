package clipto.dynamic.presentation.field.blocks

import android.view.View
import androidx.fragment.app.Fragment
import clipto.common.extensions.setDebounceClickListener
import clipto.dynamic.presentation.field.DynamicFieldViewModel
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_dynamic_field_header_view.view.*

class HeaderViewBlock(
        val titleRes: Int,
        val viewModel: DynamicFieldViewModel
) : BlockItem<Fragment>() {

    override val layoutRes: Int = R.layout.block_dynamic_field_header_view

    override fun areContentsTheSame(item: BlockItem<Fragment>): Boolean {
        return item is HeaderViewBlock
                && titleRes == item.titleRes
    }

    override fun onInit(fragment: Fragment, block: View) {
        block.ivCopy.setDebounceClickListener { viewModel.onCopy() }
    }

    override fun onBind(fragment: Fragment, block: View) {
        block.tvName.setText(titleRes)
    }

}