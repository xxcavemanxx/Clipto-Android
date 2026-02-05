package clipto.presentation.preview.video.url

import android.content.Context
import java.util.*
import java.util.regex.Pattern

class AparatVideoUrlExtractor(context: Context) : AbstractVideoUrlExtractor(context) {

    companion object {
        private val regexp by lazy { Pattern.compile("^https?://(?:www\\.)?aparat\\.com/(?:v/|video/video/embed/videohash/)([a-zA-Z0-9]+)") }
        private val fileListPattern by lazy { Pattern.compile("\"src\":\"([^\"]+)\"") }
    }

    override fun getVideoId(url: String): String? {
        val matcher = regexp.matcher(url)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    override fun extractVideoData(url: String, videoId: String): UrlData? {
        val playerCode: String = downloadUrlContent(String.format(Locale.US, "http://www.aparat.com/video/video/embed/vt/frame/showvideo/yes/videohash/%s", videoId))
                ?: return null
        val fileList = fileListPattern.matcher(playerCode)
        if (fileList.find()) {
            return UrlData(playbackUrl = fileList.group(1)?.replace("\\", ""))
        }
        return null
    }

}