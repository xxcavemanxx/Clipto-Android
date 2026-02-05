package clipto.presentation.main.list.blocks

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.core.view.updateLayoutParams
import clipto.AppContext
import clipto.common.extensions.*
import clipto.domain.*
import clipto.extensions.*
import clipto.presentation.common.StyleHelper
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

abstract class ClipItemBlock<V>(
    // MODEL
    val clip: Clip,
    val synced: Boolean,
    val textLike: String?,
    val isSelectedGetter: (clip:Clip) -> Boolean,
    val listConfigGetter: () -> ListConfig,
    // ACTIONS
    private val onClick: (clip: Clip) -> Boolean,
    private val onLongClick: (clip: Clip) -> Boolean,
    // COPY
    private val copyActionSize: Int? = null,
    private val onCopy: ((clip: Clip) -> Boolean)? = null,
) : BlockItem<V>(), View.OnClickListener, View.OnLongClickListener {

    val listConfig = listConfigGetter()
    private val isSelected = isSelectedGetter(clip)
    private val isDeleted = clip.isDeleted()
    private val folderId = clip.folderId

    @CallSuper
    override fun areItemsTheSame(item: BlockItem<V>): Boolean {
        return super.areItemsTheSame(item) &&
                item is ClipItemBlock<*> &&
                clip.getId() == item.clip.getId() &&
                copyActionSize == item.copyActionSize
    }

    @CallSuper
    override fun areContentsTheSame(item: BlockItem<V>): Boolean {
        return item is ClipItemBlock<*> && !clip.isChanged && !item.clip.isChanged
                && clip.isActive == item.clip.isActive
                && clip.changeTimestamp == item.clip.changeTimestamp
                && listConfig == item.listConfig
                && textLike == item.textLike
                && isDeleted == item.isDeleted
                && isSelected == item.isSelected
                && synced == item.synced
                && folderId == item.folderId
    }

    override fun onClick(v: View?) {
        getRef(v)?.let { ref ->
            if (ref.onClick(ref.clip) && v != null) {
                ref.onBind(v, ref.listConfig)
            }
        }
    }

    override fun onLongClick(v: View?): Boolean {
        getRef(v)?.let { ref ->
            if (ref.onLongClick(ref.clip) && v != null) {
                onSelect(v, ref.isSelectedGetter(ref.clip))
            }
        }
        return true
    }

    @CallSuper
    override fun onInit(context: V, block: View) {
        block.setOnClickListener(debounce())
        block.setOnLongClickListener(this)
        block.isFocusable = false
        getCopyAction(block)?.apply {
            setDebounceClickListener {
                getRef(block)?.let { ref ->
                    if (ref.onCopy?.invoke(ref.clip) == true) {
                        ref.onBind(block, ref.listConfig)
                    }
                }
            }
            copyActionSize?.let { size ->
                updateLayoutParams { width = size; height = size }
            }
        }
    }

    final override fun onBind(context: V, block: View) {
        onBind(block, listConfigGetter())
    }

    fun onSelect(block: View, selected: Boolean) {
        getBgView(block)?.setVisibleOrGone(selected)
    }

    fun onBind(block: View, listConfig: ListConfig) {
        block.tag = null

        // SELECTION STATE
        onSelect(block, isSelectedGetter.invoke(clip))

        // COPY ACTION
        getCopyAction(block)?.apply {
            setVisibleOrGone(listConfig.hasCopyAction)
            setImageResource(clip.getActionIconRes())
        }

        // TAGS
        getTagsView(block)?.apply {
            text = clip.getTagsAsStyledLine()
            val icon = when {
                !synced -> R.drawable.clip_state_not_synced
                else -> 0
            }
            setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
        }

        // TITLE
        getTitleView(block)?.apply {
            val isTitleVisible = !clip.title.isNullOrBlank()
            setVisibleOrGone(isTitleVisible)
            if (isTitleVisible) {
                typeface = clip.getTextTypeface()
                textSize = listConfig.textSize + 4f
                text = clip.title
            } else {
                text = null
            }
        }

        // ATTACHMENTS
        getAttachmentsView(block)?.apply {
            if (clip.filesCount > 0) {
                text = clip.filesCount.toString()
                visible()
            } else {
                gone()
            }
        }

        // PUBLIC LINK
        getPublicLinkView(block)?.apply {
            if (clip.hasPublicLink()) {
                isEnabled = clip.publicLink?.unavailable == false
                visible()
            } else {
                gone()
            }
        }

        // TEXT
        getTextView(block)?.apply {
            textSize = listConfig.textSize.toFloat()
            typeface = clip.getTextTypeface()

            val hasPreview = clip.textType == TextType.MARKDOWN || (clip.textType == TextType.LINK && listConfig.listStyle != ListStyle.GRID)
            val hasTitle = listConfig.listStyle.hasTitle
            val clipText = getText(block, listConfig)
            if (hasPreview && hasTitle) {
                val previewFactory = AppContext.get().linkPreviewFactory
                if (clip.textType == TextType.LINK) {
                    maxLines = if (previewFactory.linkify(this, clipText, listConfig.previewSize, listConfig.previewSize, true)) {
                        Integer.MAX_VALUE
                    } else {
                        listConfig.textLines
                    }
                } else if (clip.textType.isPreviewable()) {
                    maxLines = listConfig.textLines
                    clip.textType.toExt().apply(this, clipText)
                } else if (!previewFactory.preview(this, clipText, listConfig.previewSize, listConfig.previewSize, true)) {
                    maxLines = listConfig.textLines
                    text = clipText
                }
            } else {
                maxLines = listConfig.textLines
                text = clipText
            }
        }

        // ALPHA
        val alpha = when {
            isDeleted -> 0.6f
            !synced -> 0.8f
            else -> 1f
        }
        updateAlpha(block, alpha)

        // SPECIFIC
        doBind(block, listConfig)

        block.tag = this
    }

    @CallSuper
    protected open fun updateAlpha(block: View, alpha: Float) {
        getAttachmentsView(block)?.alpha = alpha
        getPublicLinkView(block)?.alpha = alpha
        getCopyAction(block)?.alpha = alpha
        getTitleView(block)?.alpha = alpha
        getTextView(block)?.alpha = alpha
        getTagsView(block)?.alpha = alpha
    }

    protected fun getRef(block: View?): ClipItemBlock<*>? = block?.tag as? ClipItemBlock<*>

    protected fun getSortByCaption(block: View, listConfig: ListConfig): CharSequence {
        return when (listConfig.sortBy) {
            SortBy.USAGE_DATE_ASC,
            SortBy.USAGE_DATE_DESC -> {
                StyleHelper.getUpdateDateValue(clip)
            }

            SortBy.MODIFY_DATE_ASC,
            SortBy.MODIFY_DATE_DESC -> {
                StyleHelper.getModifyDateValue(clip)
            }

            SortBy.DELETE_DATE_ASC,
            SortBy.DELETE_DATE_DESC -> {
                StyleHelper.getDeleteDateValue(clip)
            }

            SortBy.USAGE_COUNT_ASC,
            SortBy.USAGE_COUNT_DESC -> {
                StyleHelper.getUsageCountValue(clip)
            }

            SortBy.SIZE_ASC,
            SortBy.SIZE_DESC -> {
                StyleHelper.getSizeValue(clip, block.context)
            }

            SortBy.CHARACTERS_ASC,
            SortBy.CHARACTERS_DESC -> {
                StyleHelper.getCharactersValue(clip)
            }

            else -> {
                StyleHelper.getCreateDateValue(clip)
            }
        }
    }

    protected fun getText(block: View, listConfig: ListConfig): CharSequence {
        val context = block.context
        val hasTitle = listConfig.listStyle.hasTitle
        return (clip.title?.takeIf { !hasTitle && it.isNotBlank() } ?: clip.text)
            // reduce text size
            ?.let {
                if (it.length > listConfig.maxTextLength) {
                    it.substring(0, listConfig.maxTextLength)
                } else {
                    it
                }
            }
            ?.trimStart()
            // use only first line
            ?.let {
                if (listConfig.textLines == 1 && clip.title == null) {
                    val newLineIndex = it.indexOf('\n')
                    if (newLineIndex > -1) {
                        it.substring(0, newLineIndex)
                    } else {
                        it
                    }
                } else {
                    it.trimEnd()
                }
            }
            // highlight text
            ?.let { clipText ->
                textLike
                    ?.let { constraint ->
                        TextTypeExt.TEXT_PLAIN.highlight(context, clipText, constraint)
                    } ?: clipText
            }
            ?: ""
    }

    open fun getPublicLinkView(block: View): ImageView? = null
    open fun getAttachmentsView(block: View): TextView? = null
    open fun getCopyAction(block: View): ImageView? = null
    open fun getTitleView(block: View): TextView? = null
    open fun getTagsView(block: View): TextView? = null

    abstract fun doBind(block: View, listConfig: ListConfig)
    abstract fun getTextView(block: View): TextView?
    abstract fun getBgView(block: View): View?

}