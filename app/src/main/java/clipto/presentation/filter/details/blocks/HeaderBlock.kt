package clipto.presentation.filter.details.blocks

import com.wb.clipboard.databinding.BlockFilterDetailsHeaderBinding
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
        val binding = BlockFilterDetailsHeaderBinding.bind(block)
        binding.tvName.setDebounceClickListener { viewModel.onShowHint() }
    }

    override fun onBind(fragment: Fragment, block: View) {
        val binding = BlockFilterDetailsHeaderBinding.bind(block)
        val ctx = block.context

        val iconRes = filter.getIconRes()
        if (iconRes != 0) {
            val color = filter.getTagChipColor(ctx) ?: ThemeUtils.getColor(ctx, android.R.attr.textColorSecondary)
            binding.ivIcon.imageTintList = ColorStateList.valueOf(color)
            binding.ivIcon.setImageResource(iconRes)
            binding.ivIcon.visible()
        } else {
            binding.ivIcon.invisible()
        }

        binding.tvName.text = StyleHelper.getFilterLabel(ctx, filter)

        if (actionIcon != null) {
            binding.ivAction.tag = this
            binding.ivAction.setImageResource(actionIcon)
            binding.ivAction.setDebounceClickListener {
                val ref = it.tag
                if (ref is HeaderBlock) {
                    ref.actionListener.invoke(ref.filter, fragment)
                }
            }
            binding.ivAction.visible()
        } else {
            binding.ivAction.gone()
        }

        if (hideHint) {
            binding.tvName.isClickable = true
            binding.tvName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_floating_hint, 0)
        } else {
            binding.tvName.isClickable = false
            binding.tvName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        }
    }

}
