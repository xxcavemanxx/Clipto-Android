package clipto.presentation.snippets.details.blocks

import com.wb.clipboard.databinding.BlockSnippetItemBinding
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

class SnippetBlock(
    val viewModel: SnippetKitDetailsViewModel,
    val snippet: Snippet
) : BlockItem<SnippetKitDetailsFragment>() {

    override val layoutRes: Int = R.layout.block_snippet_item

    override fun areContentsTheSame(item: BlockItem<SnippetKitDetailsFragment>): Boolean =
        item is SnippetBlock && item.snippet == snippet

    override fun onInit(fragment: SnippetKitDetailsFragment, block: View) {
        val binding = BlockSnippetItemBinding.bind(block)
        block.setDebounceClickListener {
            val ref = block.tag
            if (ref is SnippetBlock) {
                viewModel.onOpen(ref.snippet)
            }
        }
        viewModel.textHelper.bind(
            textView = binding.tvText,
            editable = false
        )
    }

    override fun onBind(fragment: SnippetKitDetailsFragment, block: View) {
        val binding = BlockSnippetItemBinding.bind(block)
        block.tag = this
        val ctx = block.context
        val title = snippet.title.toNullIfEmpty()
        binding.tvTitle.text = title ?: block.string(R.string.clip_hint_title)
        if (title != null) {
            binding.tvTitle.setTextColor(ctx.getTextColorPrimary())
            binding.tvText.gone()
        } else {
            binding.tvTitle.setTextColor(ctx.getTextColorSecondary())
            binding.tvText.withConfig(viewModel.getTextFont(), viewModel.getTextSize())
            val textType = snippet.textType.takeIf { it.isPreviewable() } ?: TextType.TEXT_PLAIN
            textType.toExt().apply(binding.tvText, snippet.text, skipDynamicFieldsRendering = true)
            binding.tvText.visible()
        }
    }

}
