package clipto.presentation.filter.details.blocks

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
import kotlinx.android.synthetic.main.block_filter_details_header_clipboard.view.*

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
        block.ivIcon.setDebounceClickListener {
            val ref = block.tag
            if (ref is HeaderClipboardBlock) {
                viewModel.appState.requestNavigateTo(R.id.action_rune_settings, RuneSettingsViewModel.withArgs(ref.rune.getId()))
                viewModel.dismiss()
            }
        }
        block.ivAction.setImageResource(R.drawable.ic_clear_all)
        block.ivAction.setDebounceClickListener { viewModel.onClearClipboard() }
        block.tvName.setDebounceClickListener { viewModel.onShowHint() }
    }

    override fun onBind(fragment: Fragment, block: View) {
        block.tag = this
        block.ivIcon.withRoundedCorners().withHighlightIndicator().withRune(rune, rune.isActive())
        block.tvName.text = StyleHelper.getFilterLabel(block.context, filter)
        if (hideHint) {
            block.tvName.isClickable = true
            block.tvName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_floating_hint, 0)
        } else {
            block.tvName.isClickable = false
            block.tvName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        }
    }

}