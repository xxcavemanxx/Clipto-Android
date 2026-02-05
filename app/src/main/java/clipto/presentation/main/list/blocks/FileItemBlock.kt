package clipto.presentation.main.list.blocks

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.CallSuper
import clipto.common.extensions.*
import clipto.common.misc.FormatUtils
import clipto.common.misc.Units
import clipto.domain.*
import clipto.extensions.TextTypeExt
import clipto.extensions.getColorPositive
import clipto.extensions.getTextColorAccent
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.file.FileScreenHelper
import clipto.utils.GlideUtils
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.wb.clipboard.R
import java.util.*

abstract class FileItemBlock<V>(
    val file: FileRef,
    private val fileScreenHelper: FileScreenHelper,
    private val isSelectedGetter: (file: FileRef) -> Boolean,
    private val onFileClicked: (file: FileRef) -> Unit,
    private val onFileIconClicked: (file: FileRef) -> Unit,
    private val onFileChanged: (callback: (file: FileRef) -> Unit) -> Unit,
    private val onLongClick: ((file: FileRef) -> Boolean)? = null,
    private val listConfigGetter: () -> ListConfig,
    private val highlight: String? = null
) : BlockItem<V>(), View.OnLongClickListener {

    private val listConfig = listConfigGetter()
    private val type: FileType = file.type
    private val color: String? = file.color
    private val fav: Boolean = file.fav
    private val name: String? = file.title
    private val size: Long = file.size
    private val error: String? = file.error
    private val mediaType: String? = file.mediaType
    private val createDate: Date? = file.createDate
    private val downloaded = file.isDownloaded()
    private val uploaded = file.isUploaded()
    private val progress = file.progress
    private val folderId = file.folderId

    private val previewSize = Units.DP.toPx(48f).toInt()

    override fun areItemsTheSame(item: BlockItem<V>): Boolean {
        return super.areItemsTheSame(item) &&
                item is FileItemBlock<*> &&
                item.file == file
    }

    override fun areContentsTheSame(item: BlockItem<V>): Boolean {
        return item is FileItemBlock<*> &&
                item.name == name &&
                item.highlight == highlight &&
                item.mediaType == mediaType &&
                item.createDate == createDate &&
                item.color == color &&
                item.size == size &&
                item.error == error &&
                item.type == type &&
                item.fav == fav &&
                item.progress == progress &&
                item.downloaded == downloaded &&
                item.uploaded == uploaded &&
                item.listConfig == listConfig &&
                item.folderId == folderId
    }

    override fun onLongClick(v: View?): Boolean {
        getRef(v)?.let { ref ->
            if (ref.onLongClick?.invoke(ref.file) == true && v != null) {
                doOnChecked(v, ref.file, ref.isSelectedGetter(ref.file))
            }
        }
        return true
    }

    override fun onInit(context: V, block: View) {
        onLongClick?.let {
            block.setOnLongClickListener(this)
        }
        block.setDebounceClickListener {
            getRef(block)?.let { ref ->
                ref.onFileClicked(ref.file)
                doOnChecked(block, ref.file, ref.isSelectedGetter(ref.file))
            }
        }
        getIconView(block).setDebounceClickListener {
            getRef(block)?.let { ref ->
                ref.onFileIconClicked(ref.file)
            }
        }
        onFileChanged { changedFile ->
            getRef(block)?.takeIf { changedFile == it.file }?.let { ref ->
                ref.file.apply(changedFile)
                applyProgressState(block, changedFile, live = true)
            }
        }
    }

    @CallSuper
    override fun onBind(context: V, block: View) {
        block.tag = this
        applyNameState(block, file)
        applyAttrsState(block, file)
        applyCheckedState(block, file)
        applyProgressState(block, file, live = false)
        applyIconState(block, file)
    }

    private fun getRef(block: View?): FileItemBlock<*>? = block?.tag as? FileItemBlock<*>

    fun onSelect(block: View, selected: Boolean) {
        getBgView(block)?.setVisibleOrGone(selected)
    }

    @CallSuper
    protected open fun doOnChecked(block: View, file: FileRef, checked: Boolean) {
        getBgView(block)?.setVisibleOrGone(checked)
    }

    open fun getBgView(block: View): View? = null

    protected abstract fun getIconTextView(block: View): TextView
    protected abstract fun getIconView(block: View): ImageView
    protected abstract fun getTitleView(block: View): TextView
    protected abstract fun getAttrsView(block: View): TextView?
    protected abstract fun getProgressImageView(block: View): ImageView
    protected abstract fun getProgressView(block: View): LinearProgressIndicator

    private fun applyNameState(block: View, file: FileRef) {
        getTitleView(block).apply {
            typeface = file.getTextTypeface()
            textSize = listConfigGetter.invoke().textSize.toFloat()
            text = TextTypeExt.TEXT_PLAIN.highlight(block.context, file.title, highlight)
            maxLines = if (file.isFolder) 2 else 1
        }
    }

    private fun applyCheckedState(block: View, file: FileRef) {
        val checked = isSelectedGetter.invoke(file)
        doOnChecked(block, file, checked)
    }

    private fun applyAttrsState(block: View, file: FileRef) {
        getAttrsView(block)?.apply {
            text = getAttrs(file, listConfig, block)
            file.updateIcon(this)
        }
    }

    protected open fun getAttrs(file: FileRef, listConfig: ListConfig, block: View): CharSequence {
        return if (file.isFolder) {
            getAttr(file, listConfig)
        } else {
            StringBuilder()
                .append(FormatUtils.formatSize(block.context, file.size))
                .append(", ")
                .append(getAttr(file, listConfig))
        }
    }

    protected fun getAttr(file: FileRef, listConfig: ListConfig): CharSequence {
        return when (listConfig.sortBy) {
            SortBy.MODIFY_DATE_ASC,
            SortBy.MODIFY_DATE_DESC -> {
                FormatUtils.formatDateTime(file.updateDate ?: file.createDate).dashIfNullOrEmpty()
            }

//            SortBy.DELETE_DATE_ASC,
//            SortBy.DELETE_DATE_DESC -> {
//                FormatUtils.formatDateTime(file.deleteDate).dashIfNullOrEmpty()
//            }

            else -> {
                FormatUtils.formatDateTime(file.createDate).dashIfNullOrEmpty()
            }
        }
    }

    private fun applyIconState(block: View, file: FileRef) {
        val ctx = block.context
        val icon = getIconView(block)
        val iconText = getIconTextView(block)
        val lpProgress = getProgressView(block)
        val ivProgress = getProgressImageView(block)

        if (file.isFolder) {
            ivProgress.gone()
            lpProgress.gone()
            iconText.gone()

            GlideUtils.clear(icon)
            icon.setBackgroundResource(file.getPreviewBackground())
            icon.backgroundTintList = ColorStateList.valueOf(file.getBgColor(ctx))
            icon.setImageResource(R.drawable.file_type_folder)
            icon.imageTintList = ColorStateList.valueOf(file.getIconColor(ctx))
            icon.tag = null
            icon.visible()
        } else {
            val preview = fileScreenHelper.getPreview(
                fileRef = file,
                cornerRadiusInDp = 16f,
                withSquarePreview = true,
                withPreviewClickable = false
            )

            iconText.text = file.getPreviewText()
            iconText.setBackgroundResource(file.getPreviewBackground())
            iconText.visible()

            if (icon.tag == null || icon.tag != preview) {
                icon.tag = null
                icon.imageTintList = null
                icon.backgroundTintList = null
                icon.background = null
                GlideUtils.thumb(preview, icon, previewSize, previewSize) {
                    icon.tag = preview
                    iconText.gone()
                }
            }

            if (preview != null && file.isUploaded()) {
                ivProgress.gone()
                lpProgress.gone()
            }
        }

    }

    private fun applyProgressState(block: View, file: FileRef, live: Boolean) {
        val uploaded = file.isUploaded()
        val downloaded = file.isDownloaded()
        val lpProgress = getProgressView(block)
        val ivProgress = getProgressImageView(block)
        if (!downloaded || !uploaded) {
            lpProgress.progress = file.progress
            val icon =
                if (!uploaded) {
                    R.drawable.ic_upload
                } else if (file.downloadUrl.isNullOrBlank()) {
                    R.drawable.ic_download
                } else {
                    R.drawable.ic_cancel_progress
                }
            val indicatorColor =
                if (!uploaded) {
                    block.context.getColorPositive()
                } else {
                    block.context.getTextColorAccent()
                }
            lpProgress.setVisibleOrGone(file.progress > 0)
            lpProgress.setIndicatorColor(indicatorColor)
            ivProgress.setImageResource(icon)
            ivProgress.visible()
        } else {
            if (live) {
                applyIconState(block, file)
            }
            ivProgress.gone()
            lpProgress.gone()
        }
    }
}