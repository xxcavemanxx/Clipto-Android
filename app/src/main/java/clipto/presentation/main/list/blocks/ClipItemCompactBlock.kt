package clipto.presentation.main.list.blocks

import com.wb.clipboard.databinding.BlockMainListClipCompactBinding
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import clipto.domain.Clip
import clipto.domain.ListConfig
import clipto.extensions.updateIcon
import com.wb.clipboard.R

class ClipItemCompactBlock<V>(
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

    override val layoutRes: Int = R.layout.block_main_list_clip_compact
    override fun getTextView(block: View): TextView? = BlockMainListClipCompactBinding.bind(block).middleTextView
    override fun getCopyAction(block: View): ImageView? = BlockMainListClipCompactBinding.bind(block).actionView
    override fun getTagsView(block: View): TextView? = BlockMainListClipCompactBinding.bind(block).tagsView
    override fun getBgView(block: View): View? = BlockMainListClipCompactBinding.bind(block).bgView

    override fun doBind(block: View, listConfig: ListConfig) {
        val binding = BlockMainListClipCompactBinding.bind(block)
        getTextView(block)?.apply { clip.updateIcon(this) }
    }

}
