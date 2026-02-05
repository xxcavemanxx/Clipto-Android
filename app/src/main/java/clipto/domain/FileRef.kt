package clipto.domain

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.format.Formatter
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.widget.TextViewCompat
import clipto.AppContext
import clipto.common.extensions.getExtension
import clipto.common.extensions.getPersistableUri
import clipto.common.extensions.notNull
import clipto.common.extensions.toNullIfEmpty
import clipto.common.misc.ThemeUtils
import clipto.dao.firebase.model.UserCollection
import clipto.extensions.getTextColorSecondary
import com.wb.clipboard.R
import java.util.*

abstract class FileRef : AttributedObject() {

    open var md5: String? = null
    open var size: Long = 0
    open var type: FileType = FileType.FILE
    open var path: String? = null
    open var folder: String? = null
    open var createDate: Date? = null
    open var modifyDate: Date? = null
    open var updateDate: Date? = null
    open var mediaType: String? = null
    open var downloaded: Boolean = false
    open var uploaded: Boolean = false
    open var isFolder: Boolean = false

    open var downloadUrl: String? = null
    open var uploadSessionUrl: String? = null
    open var uploadUrl: String? = null
    open var platform: String? = null
    open var error: String? = null

    var progress: Int = 0

    fun with(from: FileRef): FileRef {
        this.md5 = from.md5
        this.size = from.size
        this.mediaType = from.mediaType
        this.downloaded = from.downloaded
        this.modifyDate = from.modifyDate
        this.uploaded = from.uploaded
        this.progress = from.progress
        this.downloadUrl = from.downloadUrl
        this.uploadSessionUrl = from.uploadSessionUrl
        this.uploadUrl = from.uploadUrl
        this.platform = from.platform
        this.updateDate = Date()
        this.error = from.error
        return this
    }

    open fun apply(from: FileRef): FileRef {
        this.md5 = from.md5
        this.size = from.size
        this.type = from.type
        this.title = from.title
        this.path = from.path
        this.folder = from.folder
        this.createDate = from.createDate
        this.modifyDate = from.modifyDate
        this.updateDate = from.updateDate
        this.mediaType = from.mediaType
        this.firestoreId = from.firestoreId
        this.downloaded = from.downloaded
        this.uploaded = from.uploaded
        this.progress = from.progress
        this.downloadUrl = from.downloadUrl
        this.uploadSessionUrl = from.uploadSessionUrl
        this.uploadUrl = from.uploadUrl
        this.platform = from.platform
        this.error = from.error
        this.isFolder = from.isFolder
        super.apply(from)
        return this
    }

    fun getUid(): String? = firestoreId
    fun isUploaded(): Boolean = uploaded
    fun hasError(): Boolean = error != null
    fun isRootFolder(): Boolean = firestoreId == ""
    fun isDownloaded(): Boolean = downloaded && !downloadUrl.isNullOrBlank()
    fun isInternalGenerated(): Boolean = !isFolder && !uploaded && size == 0L && hasError()
    fun asFolder(): FileRef {
        isFolder = true
        type = FileType.FOLDER
        return this
    }

    fun setUploadError(err: String) {
        uploadSessionUrl = null
        downloadUrl = null
        downloaded = false
        uploaded = false
        progress = 0
        error = err
    }

    fun setDownloadError(err: String) {
        downloadUrl = null
        downloaded = false
        progress = 0
        error = err
    }

    fun normalize(): FileRef {
        if (isFolder) {
            type = FileType.FOLDER
        } else {
            isFolder = type == FileType.FOLDER
        }
        if (isInternalGenerated()) {
            objectType = ObjectType.READONLY
        }
        folderId = folderId.toNullIfEmpty()
        path = if (isFolder) null else "/"
        createDate = createDate ?: Date()
        firestoreId = firestoreId?.toNullIfEmpty(trim = false)
        return this
    }

    override fun toString(): String {
        return "FileRef(type=$type, folder=$folder, firestoreId=$firestoreId, folderId=$folderId, name=$title)"
    }

    companion object {
        fun areContentTheSame(first: FileRef, second: FileRef): Boolean {
            return first.title == second.title &&
                    first.abbreviation == second.abbreviation &&
                    first.description == second.description &&
                    first.folderId == second.folderId &&
                    first.color == second.color &&
                    first.fav == second.fav
        }

        fun areTheSame(first: FileRef, second: FileRef): Boolean {
            return first.md5 == second.md5 &&
                    first.size == second.size &&
                    first.type == second.type &&
                    first.title == second.title &&
                    first.path == second.path &&
                    first.folder == second.folder &&
                    first.modifyDate == second.modifyDate &&
                    first.updateDate == second.updateDate &&
                    first.deleteDate == second.deleteDate &&
                    first.mediaType == second.mediaType &&
                    first.uploaded == second.uploaded &&
                    first.uploadUrl == second.uploadUrl &&
                    first.platform == second.platform &&
                    first.error == second.error &&
                    first.description == second.description &&
                    first.abbreviation == second.abbreviation &&
                    first.tagIds == second.tagIds &&
                    first.snippetSetsIds == second.snippetSetsIds &&
                    first.color == second.color &&
                    first.folderId == second.folderId &&
                    first.fav == second.fav

        }
    }
}

fun FileRef.getState(): FileState {
    if (!error.isNullOrBlank() && !isUploaded()) {
        return FileState.Error
    }
    if (!isUploaded()) {
        return FileState.Upload
    }
    if (isDownloaded()) {
        return FileState.Downloaded
    }
    if (isUploaded() && !downloadUrl.isNullOrBlank() && !isDownloaded() && !isReadOnly()) {
        return FileState.Download
    }
    if (isUploaded()) {
        return FileState.Uploaded
    }
    return FileState.Normal
}

fun FileRef.getPreviewBackground(): Int {
    return if (isFolder) {
        R.drawable.bg_file_preview_transparent
    } else if (!hasError()) {
        R.drawable.bg_file_preview
    } else {
        R.drawable.bg_file_preview_error
    }
}

fun FileRef.getIconColor(ctx: Context): Int {
    return color?.let { ThemeUtils.getColor(ctx, it) } ?: ctx.getTextColorSecondary()
}

fun FileRef.getBgColor(context: Context, alpha: Int = 40): Int {
    return if (color != null) {
        ColorUtils.setAlphaComponent(getIconColor(context), alpha)
    } else {
        ThemeUtils.getColor(context, R.attr.myBackgroundHighlight)
    }
}

fun FileRef.getPreviewText(): String? {
    return title?.getExtension()
}

fun FileRef.isVideo() = mediaType?.startsWith("video") == true || title?.endsWith(".mp4") == true

fun FileRef.isAudio() = mediaType?.startsWith("audio") == true || title?.endsWith(".mp3") == true || mediaType?.endsWith("/ogg") == true

fun FileRef.isLarge() = isVideo() || isVideo()

fun FileRef.toString(context: Context): String = """
            - **${context.getString(R.string.attachments_attr_name)}:** $title
            - **${context.getString(R.string.attachments_attr_size)}:** ${Formatter.formatShortFileSize(context, size)}
            ${mediaType?.let { type -> "- **${context.getString(R.string.attachments_attr_type)}:** $type" }.notNull()}
            ${downloadUrl?.let { url -> "- **${context.getString(R.string.attachments_attr_path)}:** $url" }.notNull()}
            ${error?.let { "\n```\n$it\n```" }.notNull()}
        """.trimIndent()

fun FileRef.getPreviewUrl(context: Context, collection: UserCollection?): Any? {
    return downloadUrl?.takeIf { downloaded }?.let { context.getPersistableUri(it) }
        ?: isReadOnly().takeIf { it }?.let { uploadUrl }
        ?: uploaded.takeIf { it }?.let { collection?.getUserFileRef(this) }
}

fun FileRef.getThumbUrl(context: Context, collection: UserCollection?): Any? {
    if (collection == null || !uploaded) return null
    return downloadUrl?.takeIf { downloaded }?.let { context.getPersistableUri(it) }
        ?: uploaded.takeIf { it }?.let { collection.getUserFileThumbRef(this) }
}

fun FileRef.canShowPreview(): Boolean {
    return uploaded || downloadUrl?.takeIf { downloaded } != null
}

fun FileRef.getTextTypeface(): Typeface? {
    val appContext = AppContext.get()
    val settings = appContext.getSettings()
    val font = Font.valueOf(settings)
    return font.typeface
}

fun FileRef.updateIcon(textView: TextView) {
    val icon: Int
    val iconColor: Int
    val ctx = textView.context
    when {
        fav -> {
            iconColor = ThemeUtils.getColor(ctx, R.attr.swipeActionStarred)
            icon = R.drawable.clip_icon_fav
        }
        else -> {
            iconColor = 0
            icon = 0
        }
    }
    textView.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
    if (iconColor != 0) {
        TextViewCompat.setCompoundDrawableTintList(textView, ColorStateList.valueOf(iconColor))
    }
}