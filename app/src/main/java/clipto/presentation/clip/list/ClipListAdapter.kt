package clipto.presentation.clip.list

import android.content.Context
import android.graphics.Point
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.*
import clipto.AppContext
import clipto.common.extensions.getSpanCount
import clipto.common.extensions.gone
import clipto.common.extensions.setVisibleOrGone
import clipto.common.extensions.visible
import clipto.common.logging.L
import clipto.common.misc.Units
import clipto.dao.objectbox.model.toBox
import clipto.domain.*
import clipto.extensions.*
import clipto.presentation.common.StyleHelper
import clipto.store.main.MainState
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.item_clip.view.*
import kotlinx.android.synthetic.main.item_clip.view.bgView
import kotlinx.android.synthetic.main.item_clip.view.attachmentsView
import kotlinx.android.synthetic.main.item_clip.view.middleTextView
import kotlinx.android.synthetic.main.item_clip.view.publicLinkView
import kotlinx.android.synthetic.main.item_clip.view.tagsView
import kotlinx.android.synthetic.main.item_clip_comfortable.view.attr2
import kotlinx.android.synthetic.main.item_clip_preview.view.*

class ClipListAdapter(
    val context: Context,
    val withMainState: MainState,
    val withCopyActionSize: Int? = null,
    val withDefaultTopPadding: Boolean = false,
    val withTextConstraint: () -> String? = { withMainState.getTextLike() },
    val withClickHandler: (clip: Clip, item: ClipViewHolder) -> Unit,
    val withCopyHandler: ((clip: Clip, item: ClipViewHolder) -> Unit)? = null,
    val withLongClickHandler: ((clip: Clip, item: ClipViewHolder) -> Unit)? = null,
    val withLinkPreviewWidth: Int = Units.displayMetrics.widthPixels - Units.DP.toPx(76f).toInt(),
    val withMultiSelect: Boolean = withCopyHandler != null
) : PagedListAdapter<Clip, ClipListAdapter.ClipViewHolder>(diffCallback), View.OnClickListener, View.OnLongClickListener {

    private val imageHeight = withLinkPreviewWidth

    private val displayWidth = context.getDisplayWidth()

    private var config = withMainState.getListConfig()

    private var itemTextSize = 0f
    private var itemTextLines = 0
    private var itemMaxCharCount = 0
    private var itemMaxTextLength = 0
    private var listStyleViewType = -1
    private var listStyle: ListStyle? = null

    private var hasCopyAction: Boolean = withCopyHandler != null
            && withMainState.getSettings().swipeActionLeft != SwipeAction.COPY
            && withMainState.getSettings().swipeActionRight != SwipeAction.COPY

    private var lastClick: Long = 0

    fun withListConfig(listConfig: ListConfig): ClipListAdapter {
        this.config = listConfig
        itemTextLines = config.textLines
        itemTextSize = config.textSize.toFloat()
        itemMaxCharCount = displayWidth / config.textSize
        itemMaxTextLength = itemMaxCharCount * (itemTextLines + 1)
        return withListStyle(listConfig.listStyle)
    }

    private fun withListStyle(listStyle: ListStyle): ClipListAdapter {
        this.listStyleViewType = listStyle.getLayoutRes()
        this.listStyle = listStyle
        return this
    }

    override fun onClick(v: View?) {
        val current = System.currentTimeMillis()
        if (!withMainState.getScreen().isContextScreen() && current - lastClick < 500) {
            // debounce
        } else {
            lastClick = current
            val holder = v?.tag
            if (holder is ClipViewHolder) {
                holder.clip?.let {
                    if (v is ImageView) {
                        withCopyHandler?.invoke(it, holder)
                    } else {
                        withClickHandler.invoke(it, holder)
                    }
                }
            }
        }
    }

    override fun onLongClick(v: View?): Boolean {
        val holder = v?.tag
        if (holder is ClipViewHolder) {
            holder.clip?.let { withLongClickHandler?.invoke(it, holder) }
        }
        return true
    }

    fun updateActive(clip: Clip?) {
//        if (clip != null && clip.isNew()) return
        val currentActiveIndex: Int = currentList
            ?.indexOfFirst { it != null && it.isActive }
            ?: -1
        val newActiveIndex: Int = clip
            ?.let { currentList }
            ?.indexOfFirst { it != null && (it.getId() == clip.getId() || it.text == clip.text) }
            ?: -1
        val changedIndex = currentList?.indexOfFirst { it != null && it.isChanged } ?: -1
        if (currentActiveIndex != newActiveIndex) {
            if (currentActiveIndex >= 0) {
                currentList?.get(currentActiveIndex)?.isActive = false
                notifyItemChanged(currentActiveIndex)
            }
            if (newActiveIndex >= 0) {
                currentList?.get(newActiveIndex)?.isActive = true
                notifyItemChanged(newActiveIndex)
            }
        }
        if (changedIndex >= 0 && changedIndex != currentActiveIndex && changedIndex != newActiveIndex) {
            notifyItemChanged(changedIndex)
        }
    }

    fun reconfigure(recyclerView: RecyclerView, newConfig: ListConfig = withMainState.getListConfig(), force: Boolean = false) {
        val newCopyAction = withCopyHandler != null
                && withMainState.getSettings().swipeActionLeft != SwipeAction.COPY
                && withMainState.getSettings().swipeActionRight != SwipeAction.COPY
        if (recyclerView.adapter != this || newCopyAction != hasCopyAction || newConfig != config || force) {
            val notifyChanged = config.listStyle != newConfig.listStyle ||
                    config.textLines != newConfig.textLines ||
                    config.textFont != newConfig.textFont ||
                    config.textSize != newConfig.textSize ||
                    newCopyAction != hasCopyAction ||
                    force
            config = newConfig
            itemTextLines = config.textLines
            itemTextSize = config.textSize.toFloat()
            itemMaxCharCount = displayWidth / config.textSize
            itemMaxTextLength = itemMaxCharCount * (itemTextLines + 1)
            hasCopyAction = newCopyAction
            L.log(this, "main_list: notify changed = {} -> {}", notifyChanged, newConfig)
            bind(recyclerView, notifyChanged = notifyChanged, force = force)
        }
    }

    private fun bind(recyclerView: RecyclerView, notifyChanged: Boolean = false, force: Boolean = false) {
        val context = recyclerView.context
        val newListStyle = config.listStyle
        if (newListStyle != listStyle || force) {
            L.log(this, "main_list: bind recycler view: {} -> {}", listStyle, newListStyle)
            if (newListStyle == ListStyle.GRID) {
                if (recyclerView.layoutManager !is StaggeredGridLayoutManager) {
                    removeItemDecorations(recyclerView)
                    val spanCount = context.getSpanCount()
                    val padding = Units.DP.toPx(8f).toInt()
                    val paddingTop = if (withDefaultTopPadding) 0 else padding
                    recyclerView.setPadding(padding, paddingTop, padding, recyclerView.paddingBottom)
                    recyclerView.layoutManager = StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)
                }
            } else if (newListStyle == ListStyle.PREVIEW) {
                removeItemDecorations(recyclerView)
                val paddingTop = if (withDefaultTopPadding) 0 else Units.DP.toPx(10f).toInt()
                recyclerView.setPadding(0, paddingTop, 0, recyclerView.paddingBottom)
                recyclerView.layoutManager = LinearLayoutManager(context)
            } else {
                removeItemDecorations(recyclerView)
                recyclerView.setPadding(0, 0, 0, recyclerView.paddingBottom)
                recyclerView.addItemDecoration(DividerItemDecoration(recyclerView.context, DividerItemDecoration.VERTICAL))
                recyclerView.layoutManager = LinearLayoutManager(context)
            }
            withListStyle(newListStyle)
        }
        if (recyclerView.adapter != this) {
            recyclerView.adapter = this
        } else if (notifyChanged) {
            notifyDataSetChanged()
        }
    }

    fun onScreenChanged(recyclerView: RecyclerView, displaySize: Point) {
        val layoutManager = recyclerView.layoutManager
        if (layoutManager is StaggeredGridLayoutManager) {
            val context = recyclerView.context
            val spanCount = layoutManager.spanCount
            val newSpanCount = context.getSpanCount(displaySize)
            if (newSpanCount > 1 && newSpanCount != spanCount) {
                recyclerView.layoutManager = StaggeredGridLayoutManager(newSpanCount, StaggeredGridLayoutManager.VERTICAL)
            }
        }
    }

    private fun removeItemDecorations(recyclerView: RecyclerView) {
        for (i in 0 until recyclerView.itemDecorationCount) {
            val decoration = recyclerView.getItemDecorationAt(i)
            if (decoration is DividerItemDecoration) {
                recyclerView.removeItemDecorationAt(i)
                break
            }
        }
    }

    override fun getItemViewType(position: Int): Int = listStyleViewType

    override fun onBindViewHolder(holder: ClipViewHolder, position: Int) =
        holder.bindTo(getItem(position))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClipViewHolder =
        ClipViewHolder(parent, viewType)

    inner class ClipViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(viewType, parent, false)
    ) {

        var alpha = 1f
        var clip: Clip? = null

        private val bgView: View = itemView.bgView
        private val attr1Key: TextView? = itemView.attr1Key
        private val attr1Value: TextView? = itemView.attr1Value
        private val attr2: TextView? = itemView.attr2
        private val tagsView: TextView? = itemView.tagsView
        private val actionView: ImageView? = itemView.actionView
        private val attachmentsView: TextView? = itemView.attachmentsView
        private val publicLinkView: ImageView? = itemView.publicLinkView
        private val middleTextView: TextView = itemView.middleTextView

        init {
            itemView.tag = this
            actionView?.tag = this
            if (withLongClickHandler != null) {
                itemView.setOnLongClickListener(this@ClipListAdapter)
            }
            withCopyActionSize?.let { size -> actionView?.updateLayoutParams { width = size; height = size } }
            actionView?.setOnClickListener(this@ClipListAdapter)
            itemView.setOnClickListener(this@ClipListAdapter)
            itemView.isFocusable = false
        }

        fun select() {
            if (withMultiSelect) {
                bgView.visible()
            }
        }

        fun unselect() {
            if (withMultiSelect) {
                bgView.gone()
            }
        }

        fun unselectHard() {
            unselect()
            updateActiveState()
        }

        fun updateActiveState() {
            middleTextView.typeface = clip.getTextTypeface()
            actionView?.setImageResource(clip.getActionIconRes())
        }

        fun bindTo(clip: Clip?) {
            this.clip = clip
            val hasPreview = clip?.textType == TextType.MARKDOWN || (clip?.textType == TextType.LINK && listStyle != ListStyle.GRID)
            val hasTitle = listStyle?.hasTitle ?: false
            val context = itemView.context

            val isNotSynced = withMainState.isNotSynced(clip)
            val stateIcon =
                when {
                    isNotSynced -> R.drawable.clip_state_not_synced
                    else -> 0
                }

            // attr1
            if (attr1Key != null && attr1Value != null) {
                when (config.sortBy) {
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
                        attr1Value.text = StyleHelper.getSizeValue(clip, context)
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
                clip?.updateIcon(attr1Key)
            } else if (attr2 != null) {
                when (config.sortBy) {
                    SortBy.USAGE_DATE_ASC,
                    SortBy.USAGE_DATE_DESC -> {
                        attr2.text = StyleHelper.getUpdateDateValue(clip)
                    }

                    SortBy.MODIFY_DATE_ASC,
                    SortBy.MODIFY_DATE_DESC -> {
                        attr2.text = StyleHelper.getModifyDateValue(clip)
                    }

                    SortBy.DELETE_DATE_ASC,
                    SortBy.DELETE_DATE_DESC -> {
                        attr2.text = StyleHelper.getDeleteDateValue(clip)
                    }

                    SortBy.USAGE_COUNT_ASC,
                    SortBy.USAGE_COUNT_DESC -> {
                        attr2.text = StyleHelper.getUsageCountValue(clip)
                    }

                    SortBy.SIZE_ASC,
                    SortBy.SIZE_DESC -> {
                        attr2.text = StyleHelper.getSizeValue(clip, context)
                    }

                    SortBy.CHARACTERS_ASC,
                    SortBy.CHARACTERS_DESC -> {
                        attr2.text = StyleHelper.getCharactersValue(clip)
                    }

                    else -> {
                        attr2.text = StyleHelper.getCreateDateValue(clip)
                    }
                }
                clip?.updateIcon(attr2)
            } else {
                clip?.updateIcon(middleTextView)
            }

            // attachments
            clip?.takeIf { it.filesCount > 0 }?.let {
                attachmentsView?.text = it.filesCount.toString()
                attachmentsView?.setVisibleOrGone(true)
            } ?: run {
                attachmentsView?.setVisibleOrGone(false)
            }

            // public link
            if (clip?.hasPublicLink() == true) {
                publicLinkView?.isEnabled = clip.publicLink?.unavailable == false
                publicLinkView?.setVisibleOrGone(true)
            } else {
                publicLinkView?.setVisibleOrGone(false)
            }

            // title
            if (hasTitle) {
                itemView.titleTextView?.apply {
                    setVisibleOrGone(!clip?.title.isNullOrBlank())
                    typeface = clip.getTextTypeface()
                    textSize = itemTextSize + 4f
                    text = clip?.title
                }
            }

            // text
            val clipText = (clip?.title?.takeIf { !hasTitle && it.isNotBlank() } ?: clip?.text)
                // reduce text size
                ?.let {
                    if (it.length > itemMaxTextLength) {
                        it.substring(0, itemMaxTextLength)
                    } else {
                        it
                    }
                }
                ?.trimStart()
                // use only first line
                ?.let {
                    if (itemTextLines == 1 && clip?.title == null) {
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
                    withTextConstraint.invoke()?.let { constraint ->
                        TextTypeExt.TEXT_PLAIN.highlight(context, clipText, constraint)
                    } ?: clipText
                }
                ?: ""

            middleTextView.apply {
                textSize = itemTextSize
                typeface = clip.getTextTypeface()

                if (hasPreview && hasTitle) {
                    val previewFactory = AppContext.get().linkPreviewFactory
                    if (clip?.textType == TextType.LINK) {
                        maxLines = if (previewFactory.linkify(this, clipText, withLinkPreviewWidth, imageHeight, true)) {
                            Integer.MAX_VALUE
                        } else {
                            itemTextLines
                        }
                    } else if (clip?.textType?.isPreviewable() == true) {
                        maxLines = itemTextLines
                        clip.textType.toExt().apply(this, clipText)
                    } else if (!previewFactory.preview(this, clipText, withLinkPreviewWidth, imageHeight, true)) {
                        maxLines = itemTextLines
                        text = clipText
                    }
                } else {
                    maxLines = itemTextLines
                    text = clipText
                }
            }

            // action
            actionView?.setVisibleOrGone(hasCopyAction)
            actionView?.setImageResource(clip.getActionIconRes())

            // tags
            tagsView?.text = clip.getTagsAsStyledLine()
            tagsView?.setCompoundDrawablesRelativeWithIntrinsicBounds(stateIcon, 0, 0, 0)

            // is selected?
            if (withMainState.isSelected(clip)) {
                select()
            } else {
                unselect()
            }

            val newAlpha = when {
                clip?.isDeleted() == true -> 0.6f
                isNotSynced -> 0.8f
                else -> 1f
            }

            if (newAlpha != alpha) {
                alpha = newAlpha
                attr1Key?.alpha = alpha
                attr1Value?.alpha = alpha
                attr2?.alpha = alpha
                tagsView?.alpha = alpha
                actionView?.alpha = alpha
                attachmentsView?.alpha = alpha
                publicLinkView?.alpha = alpha
                middleTextView.alpha = alpha
                itemView.titleTextView?.alpha = alpha
            }
        }
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<Clip>() {

            override fun areItemsTheSame(oldItem: Clip, newItem: Clip): Boolean =
                oldItem.toBox().localId == newItem.toBox().localId

            override fun areContentsTheSame(oldItem: Clip, newItem: Clip): Boolean =
                !newItem.isChanged
                        && !oldItem.isChanged
                        && oldItem.isActive == newItem.isActive
                        && oldItem.changeTimestamp == newItem.changeTimestamp

            override fun getChangePayload(oldItem: Clip, newItem: Clip): Any = newItem
        }
    }

}