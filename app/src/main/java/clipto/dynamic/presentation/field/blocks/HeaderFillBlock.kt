package clipto.dynamic.presentation.field.blocks

import android.view.View
import androidx.fragment.app.Fragment
import clipto.common.extensions.setDebounceClickListener
import clipto.extensions.getActionIconColorHighlight
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_dynamic_field_header_fill.view.*

class HeaderFillBlock(
        val titleRes: Int,
        val title: String? = null,
        val actionTitleRes: Int,
        val actionActive: Boolean,
        val onAction: () -> Unit,
) : BlockItem<Fragment>() {

    override val layoutRes: Int = R.layout.block_dynamic_field_header_fill

    override fun areContentsTheSame(item: BlockItem<Fragment>): Boolean {
        return item is HeaderFillBlock
                && actionTitleRes == item.actionTitleRes
                && actionActive == item.actionActive
                && titleRes == item.titleRes
                && title == item.title
    }

    override fun onInit(fragment: Fragment, block: View) {
        block.mbClearAll.setDebounceClickListener {
            val ref = block.tag
            if (ref is HeaderFillBlock) {
                ref.onAction.invoke()
            }
        }
    }

    override fun onBind(fragment: Fragment, block: View) {
        block.tag = this
        if (title != null) {
            block.tvTitle.text = title
        } else {
            block.tvTitle.setText(titleRes)
        }
        block.mbClearAll.setText(actionTitleRes)
        if (actionActive) {
            block.mbClearAll.setTextColor(block.context.getActionIconColorHighlight())
        } else {
            block.mbClearAll.setTextColor(block.context.getTextColorSecondary())
        }
    }

}