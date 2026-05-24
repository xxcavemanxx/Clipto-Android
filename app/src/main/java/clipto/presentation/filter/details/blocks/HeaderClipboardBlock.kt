package clipto.presentation.filter.details.blocks

import com.wb.clipboard.databinding.BlockFilterDetailsHeaderClipboardBinding
import android.view.View
import androidx.fragment.app.Fragment
import clipto.common.extensions.setDebounceClickListener
import clipto.domain.Filter
import clipto.domain.IRune
import clipto.extensions.getTitle
import clipto.presentation.common.StyleHelper
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.filter.details.FilterDetailsViewModel
import clipto.presentation.runes.RuneSettingsViewModel
import com.wb.clipboard.R

class HeaderClipboardBlock(
    val viewModel: FilterDetailsViewModel,
    val rune: IRune,
    val filter: Filter,
    val uid: String? = filter.uid,
    val name: String = filter.getTitle(viewModel.app),
    val color: String? = filter.color,
    val notesCount: Long = filter.notesCount,
    val hideHint: Boolean = filter.hideHint
) : BlockItem<Fragment>() {

    override val layoutRes: Int = R.layout.block_filter_details_header_clipboard

    override fun areContentsTheSame(item: BlockItem<Fragment>): Boolean =
        item is HeaderClipboardBlock &&
                item.uid == uid &&
                item.name == name &&
                item.color == color &&
                item.notesCount == notesCount &&
                item.hideHint == hideHint

    override fun onInit(fragment: Fragment, block: View) {
        val binding = BlockFilterDetailsHeaderClipboardBinding.bind(block)
        binding.ivIcon.setDebounceClickListener {
            val ref = block.tag
            if (ref is HeaderClipboardBlock) {
                viewModel.appState.requestNavigateTo(R.id.action_rune_settings, RuneSettingsViewModel.withArgs(ref.rune.getId()))
                viewModel.dismiss()
            }
        }
        binding.ivAction.setImageResource(R.drawable.ic_clear_all)
        binding.ivAction.setDebounceClickListener { viewModel.onClearClipboard() }
        binding.tvName.setDebounceClickListener { viewModel.onShowHint() }
    }

    override fun onBind(fragment: Fragment, block: View) {
        val binding = BlockFilterDetailsHeaderClipboardBinding.bind(block)
        block.tag = this
        binding.ivIcon.withRoundedCorners().withHighlightIndicator().withRune(rune, rune.isActive())
        binding.tvName.text = StyleHelper.getFilterLabel(block.context, filter)
        if (hideHint) {
            binding.tvName.isClickable = true
            binding.tvName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_floating_hint, 0)
        } else {
            binding.tvName.isClickable = false
            binding.tvName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        }
    }

}
