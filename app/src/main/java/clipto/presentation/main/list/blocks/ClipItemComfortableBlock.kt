package clipto.presentation.main.list.blocks

import com.wb.clipboard.databinding.BlockMainListClipComfortableBinding
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import clipto.domain.Clip
import clipto.domain.ListConfig
import clipto.extensions.updateIcon
import com.wb.clipboard.R

class ClipItemComfortableBlock<V>(
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

    override val layoutRes: Int = R.layout.block_main_list_clip_comfortable
    override fun getAttachmentsView(block: View): TextView? = BlockMainListClipComfortableBinding.bind(block).attachmentsView
    override fun getPublicLinkView(block: View): ImageView? = BlockMainListClipComfortableBinding.bind(block).publicLinkView
    override fun getTextView(block: View): TextView? = BlockMainListClipComfortableBinding.bind(block).middleTextView
    override fun getTagsView(block: View): TextView? = BlockMainListClipComfortableBinding.bind(block).tagsView
    override fun getBgView(block: View): View? = BlockMainListClipComfortableBinding.bind(block).bgView

    override fun doBind(block: View, listConfig: ListConfig) {
        val binding = BlockMainListClipComfortableBinding.bind(block)
        binding.attr2.apply {
            text = getSortByCaption(block, listConfig)
            clip.updateIcon(this)
        }
    }

    override fun updateAlpha(block: View, alpha: Float) {
        super.updateAlpha(block, alpha)
        BlockMainListClipComfortableBinding.bind(block).attr2.alpha = alpha
    }

}
