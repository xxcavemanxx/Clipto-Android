package clipto.presentation.preview.video.url

import android.content.Context
import java.util.regex.Pattern

class PVideoUrlExtractor(context: Context) : AbstractVideoUrlExtractor(context) {

    companion object {
        private val regexp by lazy { Pattern.compile("^https?://(?:www\\.)?[a-z]{7}\\.com/view_video\\.php\\?viewkey=([a-zA-Z0-9]+)") }
        private val videos by lazy { Pattern.compile("(?<=\\*\\/)\\w+") }
    }

    override fun getVideoId(url: String): String? {
        val matcher = regexp.matcher(url)
        if (matcher.find()) {
            return url
        }
        return null
    }

    override fun extractVideoData(url: String, videoId: String): UrlData? {
        val source: String = downloadUrlContent(url) ?: return null
        val videoList = videos.matcher(source)
        val urls = mutableListOf<String>()
        val qualityReg = "(\\d+)P_(\\d+)K".toRegex()
        val joinReg = "[\" +]".toRegex()
        while (videoList.find()) {
            val match = videoList.group()
            val value = "(?<=${match}=\")[^;]+(?=\")".toRegex().find(source)?.groupValues?.first()?.replace(joinReg, "")
            if (value != null) {
                if (value.startsWith("https")) {
                    if (urls.size == 4) {
                        break
                    }
                    urls.add(value)
                } else {
                    val newVal = urls.getOrNull(urls.size - 1)?.let { it + value } ?: value
                    urls[urls.size - 1] = newVal
                }
            }
        }
        val playbackUrl = urls
                .map { qualityReg.find(it)!!.groupValues[1].toInt() to it }
                .maxByOrNull { it.first }!!.second
        return UrlData(playbackUrl = playbackUrl)
    }

}