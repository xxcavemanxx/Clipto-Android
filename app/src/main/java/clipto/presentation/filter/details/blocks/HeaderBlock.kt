package clipto.presentation.filter.details.blocks

import android.content.res.ColorStateList
import android.view.View
import androidx.fragment.app.Fragment
import clipto.common.extensions.gone
import clipto.common.extensions.invisible
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.visible
import clipto.common.misc.ThemeUtils
import clipto.domain.Filter
import clipto.extensions.getIconRes
import clipto.extensions.getTagChipColor
import clipto.extensions.getTitle
import clipto.presentation.common.StyleHelper
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.filter.details.FilterDetailsViewModel
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_filter_details_header.view.*

class HeaderBlock(
    val viewModel: FilterDetailsViewModel,
    val filter: Filter,
    val actionIcon: Int? = null,
    val actionListener: (filter: Filter, fragment: Fragment) -> Unit = { _, _ -> },
    val uid: String? = filter.uid,
    val name: String = filter.getTitle(viewModel.app),
    val color: String? = filter.color,
    val notesCount: Long = filter.notesCount,
    val hideHint: Boolean = filter.hideHint
) : BlockItem<Fragment>() {

    override val layoutRes: Int = R.layout.block_filter_details_header

    override fun areContentsTheSame(item: BlockItem<Fragment>): Boolean =
        item is HeaderBlock &&
                item.uid == uid &&
                item.name == name &&
                item.color == color &&
                item.notesCount == notesCount &&
                item.actionIcon == actionIcon &&
                item.hideHint == hideHint

    override fun onInit(fragment: Fragment, block: View) {
        block.tvName.setDebounceClickListener { viewModel.onShowHint() }
    }

    override fun onBind(fragment: Fragment, block: View) {
        val ctx = block.context

        val iconRes = filter.getIconRes()
        if (iconRes != 0) {
            val color = filter.getTagChipColor(ctx) ?: ThemeUtils.getColor(ctx, android.R.attr.textColorSecondary)
            block.ivIcon.imageTintList = ColorStateList.valueOf(color)
            block.ivIcon.setImageResource(iconRes)
            block.ivIcon.visible()
        } else {
            block.ivIcon.invisible()
        }

        block.tvName.text = StyleHelper.getFilterLabel(ctx, filter)

        if (actionIcon != null) {
            block.ivAction.tag = this
            block.ivAction.setImageResource(actionIcon)
            block.ivAction.setDebounceClickListener {
                val ref = it.tag
                if (ref is HeaderBlock) {
                    ref.actionListener.invoke(ref.filter, fragment)
                }
            }
            block.ivAction.visible()
        } else {
            block.ivAction.gone()
        }

        if (hideHint) {
            block.tvName.isClickable = true
            block.tvName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_floating_hint, 0)
        } else {
            block.tvName.isClickable = false
            block.tvName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        }
    }

}