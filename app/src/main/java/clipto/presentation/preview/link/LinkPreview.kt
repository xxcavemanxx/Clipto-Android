package clipto.presentation.preview.link

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import clipto.common.R
import clipto.common.misc.Units
import java.io.Serializable

data class LinkPreview(
    var url: String? = null,
    var sitename: String? = null,
    var title: CharSequence? = null,
    var description: String? = null,
    var imageUrl: Any? = null,
    var thumbUrl: Any? = null,
    var mediatype: String? = null,
    var playbackUrl: String? = null,
    var embedUrl: String? = null,
    var previewPlaceholder: CharSequence? = null,
    val withPreviewPlaceholder: Boolean = false,
    val withSquarePreview: Boolean = false,
    val cornerRadiusInDp: Float = 12f
) : Serializable {
    fun isValid() = !sitename.isNullOrBlank()
            || !title.isNullOrBlank()
            || !description.isNullOrBlank()
            || imageUrl != null
            || isVideo()
            || isImage()
            || isGif()

    fun getSiteName(): String? = sitename ?: url?.let { runCatching { Uri.parse(it).host }.getOrNull() }

    fun isGif() = url?.endsWith(".gif") == true

    fun isImage() = mediatype?.startsWith("image") == true ||
            url?.let { it.endsWith(".png") || it.endsWith(".jpeg") || it.endsWith(".jpg") } == true ||
            imageUrl?.toString()?.let { it.endsWith(".png") || it.endsWith(".jpeg") || it.endsWith(".jpg") } == true

    fun isVideo() = mediatype?.startsWith("video") == true ||
            url?.endsWith(".mp4") == true

    fun isAudio() = mediatype?.startsWith("audio") == true ||
            url?.endsWith(".mp3") == true ||
            mediatype?.endsWith("/ogg") == true

    fun isPreviewable() = isGif() || isImage() || isVideo() || isAudio()

    fun getType(): Type? = Type.values().find { it.isValid(this) }

    enum class Type(val imageRes: Int) {
        VIDEO(R.drawable.ic_link_preview_play_padded) {
            override fun isValid(preview: LinkPreview): Boolean = preview.isVideo()
            override fun isMedia(): Boolean = true
        },
        AUDIO(R.drawable.ic_link_preview_play_padded) {
            override fun isValid(preview: LinkPreview): Boolean = preview.isAudio()
            override fun isMedia(): Boolean = true
        },
        IMAGE(R.drawable.ic_link_preview_zoom_padded) {
            override fun isValid(preview: LinkPreview): Boolean = true // preview.isImage()
        };

        open fun isValid(preview: LinkPreview): Boolean = true
        open fun isMedia(): Boolean = false

        fun toDrawable(context: Context, maxSize: Int): Drawable {
            if (Units.DP.toPx(52f).toInt() >= maxSize) {
                return ColorDrawable(Color.TRANSPARENT)
            }
            return context.getDrawable(imageRes)!!
        }
    }
}