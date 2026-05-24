package clipto.presentation.main.list.blocks

import com.wb.clipboard.databinding.BlockMainListClipFolderBinding
import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.CallSuper
import clipto.common.extensions.findFirstWebUrl
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.setVisibleOrGone
import clipto.common.misc.Units
import clipto.domain.Clip
import clipto.domain.ListConfig
import clipto.extensions.*
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.preview.link.LinkPreview
import clipto.utils.GlideUtils
import com.wb.clipboard.R

class ClipItemFolderBlock<V>(
    clip: Clip,
    synced: Boolean,
    textLike: String?,
    isSelectedGetter: (clip: Clip) -> Boolean,
    listConfigGetter: () -> ListConfig,
    onClick: (clip: Clip) -> Boolean,
    onLongClick: (clip: Clip) -> Boolean,
    copyActionSize: Int? = null,
    onCopy: ((clip: Clip) -> Boolean)? = null,
    private val onClipIconClicked: (clip: Clip, previewUrl: String?) -> Boolean,
    private val onFetchPreview: (id: String, url: String, callback: (preview: LinkPreview) -> Unit) -> Unit,
    private val checkable: Boolean = false,
    private val folderId: String? = null,
    private val flatMode: Boolean = false,
    private val relativePathGetter: ((folderId: String?, clip: Clip, callback: (path: String) -> Unit) -> Unit)? = null
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

    override val layoutRes: Int = R.layout.block_main_list_clip_folder
    override fun getTextView(block: View): TextView? = BlockMainListClipFolderBinding.bind(block).tvName
    override fun getPublicLinkView(block: View): ImageView? = BlockMainListClipFolderBinding.bind(block).publicLinkView
    override fun getAttachmentsView(block: View): TextView? = BlockMainListClipFolderBinding.bind(block).attachmentsView
    override fun getBgView(block: View): View? = if (checkable) BlockMainListClipFolderBinding.bind(block).ivSelected else BlockMainListClipFolderBinding.bind(block).bgView

    override fun areItemsTheSame(item: BlockItem<V>): Boolean {
        return super.areItemsTheSame(item)
                && item is ClipItemFolderBlock<*>
                && item.checkable == checkable
    }

    override fun areContentsTheSame(item: BlockItem<V>): Boolean {
        return super.areContentsTheSame(item)
                && item is ClipItemFolderBlock<*>
                && item.flatMode == flatMode
                && item.folderId == folderId
    }

    @CallSuper
    override fun onInit(context: V, block: View) {
        val binding = BlockMainListClipFolderBinding.bind(block)
        super.onInit(context, block)
        binding.ivIcon.setDebounceClickListener {
            val previewUrl = it.tag?.toString()
            getRef(block)?.let { ref ->
                ref as ClipItemFolderBlock
                if (ref.onClipIconClicked.invoke(ref.clip, previewUrl)) {
                    ref.onBind(block, ref.listConfig)
                }
            }
        }
    }

    override fun doBind(block: View, listConfig: ListConfig) {
        val binding = BlockMainListClipFolderBinding.bind(block)
        val ctx = block.context
        val tvName = binding.tvName

        binding.tvAttrs.apply {
            text = getSortByCaption(block, listConfig)
            clip.updateIcon(this)
        }

        val iconRes = clip.textType.toExt().iconRes
        val icon = binding.ivIcon

        val url = clip.toLinkifiedSpannable().findFirstWebUrl()
        if (url != null) {
            if (icon.tag != url) {
                GlideUtils.clear(icon)
                onFetchPreview(System.identityHashCode(block).toString(), url) { preview ->
                    val size = Units.DP.toPx(48f).toInt()
                    GlideUtils.preview(
                        preview = preview,
                        view = icon,
                        width = size,
                        height = size,
                        placeholder = iconRes,
                        onComplete = {
                            icon.imageTintList = null
                            icon.tag = url
                        }
                    )
                }
            }
        } else {
            GlideUtils.clear(icon)
            icon.setImageResource(iconRes)
            icon.imageTintList = ColorStateList.valueOf(ctx.getTextColorSecondary())
            icon.tag = null
        }

        if (tvName.text.isNullOrBlank()) {
            tvName.text = ctx.getString(R.string.clip_hint_title)
            tvName.setTextColor(ctx.getTextColorSecondary())
        } else {
            tvName.setTextColor(ctx.getTextColorPrimary())
        }

        binding.tvPath?.apply {
            setVisibleOrGone(flatMode)
            if (flatMode) {
                relativePathGetter?.invoke(folderId, clip) { path ->
                    text = path
                }
            }
        }
    }

    override fun updateAlpha(block: View, alpha: Float) {
        super.updateAlpha(block, alpha)
        BlockMainListClipFolderBinding.bind(block).ivIcon.alpha = alpha
        BlockMainListClipFolderBinding.bind(block).tvIcon?.alpha = alpha
    }

}
