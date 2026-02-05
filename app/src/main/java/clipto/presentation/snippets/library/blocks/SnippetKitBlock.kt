package clipto.presentation.snippets.library.blocks

import android.graphics.Color
import android.view.View
import clipto.common.extensions.gone
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.visible
import clipto.domain.SnippetKit
import clipto.extensions.getTextColorSecondary
import clipto.extensions.getTitleRes
import clipto.extensions.getUserNameLabel
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.snippets.library.SnippetKitLibraryFragment
import clipto.presentation.snippets.library.SnippetKitLibraryViewModel
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_snippet_kit_item.view.*

class SnippetKitBlock(
        val kit: SnippetKit,
        val name: String = kit.name,
        val color: String? = kit.color,
        val installs: Int = kit.installs,
        val userName: String? = kit.userName,
        val snippetsCount: Int = kit.snippetsCount,
        val viewModel: SnippetKitLibraryViewModel
) : BlockItem<SnippetKitLibraryFragment>() {

    override val layoutRes: Int = R.layout.block_snippet_kit_item

    override fun areContentsTheSame(item: BlockItem<SnippetKitLibraryFragment>): Boolean {
        return item is SnippetKitBlock
                && name == item.name
                && color == item.color
                && installs == item.installs
                && userName == item.userName
                && snippetsCount == item.snippetsCount
    }

    override fun onInit(fragment: SnippetKitLibraryFragment, block: View) {
        block.mcvCard.setDebounceClickListener {
            val ref = block.tag
            if (ref is SnippetKitBlock) {
                viewModel.onOpenSnippetKit(ref.kit)
            }
        }
    }

    override fun onBind(fragment: SnippetKitLibraryFragment, block: View) {
        block.tag = this

        val ctx = block.context

        val bgColor = color?.let { Color.parseColor(it) } ?: ctx.getTextColorSecondary()
        block.ivBackground.setBackgroundColor(bgColor)
        block.ivBackground.refreshDrawableState()

        block.tvName.text = name

        block.tvAuthor.text = kit.getUserNameLabel()

        block.tvSnippetsValue.text = snippetsCount.toString()

        block.tvDownloadsValue.text = installs.toString()

        if (viewModel.isMy(kit)) {
            block.tvStatus.setText(kit.publicStatus.getTitleRes())
            block.tvName.maxLines = 1
            block.tvStatus.visible()
        } else {
            block.tvName.maxLines = 2
            block.tvStatus.gone()
        }
    }
}