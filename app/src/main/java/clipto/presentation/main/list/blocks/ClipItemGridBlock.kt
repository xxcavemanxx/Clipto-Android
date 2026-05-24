package clipto.presentation.main.list.blocks

import com.wb.clipboard.databinding.BlockMainListClipGridBinding
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import clipto.domain.Clip
import clipto.domain.ListConfig
import clipto.extensions.updateIcon
import com.wb.clipboard.R

class ClipItemGridBlock<V>(
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

    override val layoutRes: Int = R.layout.block_main_list_clip_grid
    override fun getAttachmentsView(block: View): TextView? = BlockMainListClipGridBinding.bind(block).attachmentsView
    override fun getPublicLinkView(block: View): ImageView? = BlockMainListClipGridBinding.bind(block).publicLinkView
    override fun getTitleView(block: View): TextView? = BlockMainListClipGridBinding.bind(block).titleTextView
    override fun getTextView(block: View): TextView? = BlockMainListClipGridBinding.bind(block).middleTextView
    override fun getTagsView(block: View): TextView? = BlockMainListClipGridBinding.bind(block).tagsView
    override fun getBgView(block: View): View? = BlockMainListClipGridBinding.bind(block).bgView

    override fun doBind(block: View, listConfig: ListConfig) {
        val binding = BlockMainListClipGridBinding.bind(block)
        binding.attr2.apply {
            text = getSortByCaption(block, listConfig)
            clip.updateIcon(this)
        }
    }

    override fun updateAlpha(block: View, alpha: Float) {
        super.updateAlpha(block, alpha)
        BlockMainListClipGridBinding.bind(block).attr2.alpha = alpha
    }

}
