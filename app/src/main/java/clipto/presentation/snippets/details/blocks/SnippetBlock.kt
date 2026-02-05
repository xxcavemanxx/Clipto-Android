package clipto.presentation.snippets.details.blocks

import android.view.View
import clipto.common.extensions.*
import clipto.domain.Snippet
import clipto.domain.TextType
import clipto.extensions.getTextColorPrimary
import clipto.extensions.getTextColorSecondary
import clipto.extensions.toExt
import clipto.extensions.withConfig
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.snippets.details.SnippetKitDetailsFragment
import clipto.presentation.snippets.details.SnippetKitDetailsViewModel
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_snippet_item.view.*

class SnippetBlock(
    val viewModel: SnippetKitDetailsViewModel,
    val snippet: Snippet
) : BlockItem<SnippetKitDetailsFragment>() {

    override val layoutRes: Int = R.layout.block_snippet_item

    override fun areContentsTheSame(item: BlockItem<SnippetKitDetailsFragment>): Boolean =
        item is SnippetBlock && item.snippet == snippet

    override fun onInit(fragment: SnippetKitDetailsFragment, block: View) {
        block.setDebounceClickListener {
            val ref = block.tag
            if (ref is SnippetBlock) {
                viewModel.onOpen(ref.snippet)
            }
        }
        viewModel.textHelper.bind(
            textView = block.tvText,
            editable = false
        )
    }

    override fun onBind(fragment: SnippetKitDetailsFragment, block: View) {
        block.tag = this
        val ctx = block.context
        val title = snippet.title.toNullIfEmpty()
        block.tvTitle.text = title ?: block.string(R.string.clip_hint_title)
        if (title != null) {
            block.tvTitle.setTextColor(ctx.getTextColorPrimary())
            block.tvText.gone()
        } else {
            block.tvTitle.setTextColor(ctx.getTextColorSecondary())
            block.tvText.withConfig(viewModel.getTextFont(), viewModel.getTextSize())
            val textType = snippet.textType.takeIf { it.isPreviewable() } ?: TextType.TEXT_PLAIN
            textType.toExt().apply(block.tvText, snippet.text, skipDynamicFieldsRendering = true)
            block.tvText.visible()
        }
    }

}