package clipto.presentation.main.list.blocks

import com.wb.clipboard.databinding.BlockMainListClipPreviewBinding
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import clipto.domain.Clip
import clipto.domain.ListConfig
import clipto.extensions.updateIcon
import com.wb.clipboard.R

class ClipItemPreviewBlock<V>(
    clip: Clip,
    synced: Boolean,
    textLike: String?,
    isSelectedGetter: (clip:Clip) -> Boolean,
    listConfigGetter: () -> ListConfig,
    onClick: (clip: Clip) -> Boolean,
    onLongClick: (clip: Clip) -> Boolean,
    copyActionSize: Int? = null,
    onCopy: ((clip: Clip) -> Boolean)? = null,
) : ClipItemBlock<V>(
    clip = clip,
    synced = synced,
    textLike = textLike,
    isSelectedGetter = isSelectedGetter,
    listConfigGetter = listConfigGetter,
    onClick = onClick,
    onLongClick = onLongClick,
    copyActionSize = copyActionSize,
    onCopy = onCopy
) {

    override val layoutRes: Int = R.layout.block_main_list_clip_preview
    override fun getAttachmentsView(block: View): TextView? = BlockMainListClipPreviewBinding.bind(block).attachmentsView
    override fun getPublicLinkView(block: View): ImageView? = BlockMainListClipPreviewBinding.bind(block).publicLinkView
    override fun getTitleView(block: View): TextView? = BlockMainListClipPreviewBinding.bind(block).titleTextView
    override fun getTextView(block: View): TextView? = BlockMainListClipPreviewBinding.bind(block).middleTextView
    override fun getTagsView(block: View): TextView? = BlockMainListClipPreviewBinding.bind(block).tagsView
    override fun getBgView(block: View): View? = BlockMainListClipPreviewBinding.bind(block).bgView

    override fun doBind(block: View, listConfig: ListConfig) {
        val binding = BlockMainListClipPreviewBinding.bind(block)
        binding.attr2.apply {
            text = getSortByCaption(block, listConfig)
            clip.updateIcon(this)
        }
    }

    override fun updateAlpha(block: View, alpha: Float) {
        super.updateAlpha(block, alpha)
        BlockMainListClipPreviewBinding.bind(block).attr2.alpha = alpha
    }

}
