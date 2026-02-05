package clipto.presentation.main.list.blocks

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import clipto.domain.Clip
import clipto.domain.ListConfig
import clipto.extensions.updateIcon
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_main_list_clip_compact.view.*

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
    override fun getTextView(block: View): TextView? = block.middleTextView
    override fun getCopyAction(block: View): ImageView? = block.actionView
    override fun getTagsView(block: View): TextView? = block.tagsView
    override fun getBgView(block: View): View? = block.bgView

    override fun doBind(block: View, listConfig: ListConfig) {
        getTextView(block)?.apply { clip.updateIcon(this) }
    }

}