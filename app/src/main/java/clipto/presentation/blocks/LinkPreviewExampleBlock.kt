package clipto.presentation.blocks

import android.view.View
import clipto.extensions.TextTypeExt
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_link_preview_example.view.*

class LinkPreviewExampleBlock<C>(
    private val link: String,
    private val canPreview: Boolean,
    private val hidePreview: Boolean
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_link_preview_example

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is LinkPreviewExampleBlock
                && canPreview == item.canPreview
                && hidePreview == item.hidePreview
                && link == item.link

    override fun onBind(context: C, block: View) {
        TextTypeExt.LINK.apply(block.linkPreviewView, link)
    }

}