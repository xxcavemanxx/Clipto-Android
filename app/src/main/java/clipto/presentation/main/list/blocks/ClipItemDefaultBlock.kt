package clipto.presentation.main.list.blocks

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import clipto.domain.Clip
import clipto.domain.ListConfig
import clipto.domain.SortBy
import clipto.extensions.updateIcon
import clipto.presentation.common.StyleHelper
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_main_list_clip_default.view.*

class ClipItemDefaultBlock<V>(
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

    override val layoutRes: Int = R.layout.block_main_list_clip_default
    override fun getAttachmentsView(block: View): TextView? = block.attachmentsView
    override fun getPublicLinkView(block: View): ImageView? = block.publicLinkView
    override fun getTextView(block: View): TextView? = block.middleTextView
    override fun getCopyAction(block: View): ImageView? = block.actionView
    override fun getTagsView(block: View): TextView? = block.tagsView
    override fun getBgView(block: View): View? = block.bgView

    override fun doBind(block: View, listConfig: ListConfig) {
        val attr1Key = block.attr1Key
        val attr1Value = block.attr1Value
        when (listConfig.sortBy) {
            SortBy.USAGE_DATE_ASC,
            SortBy.USAGE_DATE_DESC -> {
                attr1Key.setText(R.string.clip_attr_updated)
                attr1Value.text = StyleHelper.getUpdateDateValue(clip)
            }

            SortBy.MODIFY_DATE_ASC,
            SortBy.MODIFY_DATE_DESC -> {
                attr1Key.setText(R.string.clip_attr_edited)
                attr1Value.text = StyleHelper.getModifyDateValue(clip)
            }

            SortBy.DELETE_DATE_ASC,
            SortBy.DELETE_DATE_DESC -> {
                attr1Key.setText(R.string.clip_attr_deleted)
                attr1Value.text = StyleHelper.getDeleteDateValue(clip)
            }

            SortBy.USAGE_COUNT_ASC,
            SortBy.USAGE_COUNT_DESC -> {
                attr1Key.setText(R.string.clip_attr_usageCount)
                attr1Value.text = StyleHelper.getUsageCountValue(clip)
            }

            SortBy.SIZE_ASC,
            SortBy.SIZE_DESC -> {
                attr1Key.setText(R.string.clip_attr_size)
                attr1Value.text = StyleHelper.getSizeValue(clip, block.context)
            }

            SortBy.CHARACTERS_ASC,
            SortBy.CHARACTERS_DESC -> {
                attr1Key.setText(R.string.clip_attr_charsCount)
                attr1Value.text = StyleHelper.getCharactersValue(clip)
            }

            else -> {
                attr1Key.setText(R.string.clip_attr_created)
                attr1Value.text = StyleHelper.getCreateDateValue(clip)
            }
        }
        clip.updateIcon(attr1Key)
    }

    override fun updateAlpha(block: View, alpha: Float) {
        super.updateAlpha(block, alpha)
        block.attr1Value.alpha = alpha
        block.attr1Key.alpha = alpha
    }

}