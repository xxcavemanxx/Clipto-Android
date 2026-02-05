package clipto.presentation.preview.video.url

import android.content.Context
import org.json.JSONObject
import java.util.*
import java.util.regex.Pattern

class VimeoVideoUrlExtractor(context: Context) : AbstractVideoUrlExtractor(context) {

    companion object {
        private val regexp by lazy { Pattern.compile("https?://(?:(?:www|(player))\\.)?vimeo(pro)?\\.com/(?!(?:channels|album)/[^/?#]+/?(?:$|[?#])|[^/]+/review/|ondemand/)(?:.*?/)?(?:(?:play_redirect_hls|moogaloop\\.swf)\\?clip_id=)?(?:videos?/)?([0-9]+)(?:/[\\da-f]+)?/?(?:[?&].*)?(?:[#].*)?$") }
    }

    override fun getVideoId(url: String): String? {
        val matcher = regexp.matcher(url)
        if (matcher.find()) {
            return matcher.group(3)
        }
        return null
    }

    override fun extractVideoData(url: String, videoId: String): UrlData? {
        val playerCode: String = downloadUrlContent(String.format(Locale.US, "https://player.vimeo.com/video/%s/config", videoId))
                ?: return null
        val json = JSONObject(playerCode)
        val files = json.getJSONObject("request").getJSONObject("files")
        val urlData = UrlData()
        if (files.has("hls")) {
            var hls = files.getJSONObject("hls")
            try {
                urlData.playbackUrl = hls.getString("url")
            } catch (e: Exception) {
                val defaultCdn = hls.getString("default_cdn")
                val cdns = hls.getJSONObject("cdns")
                hls = cdns.getJSONObject(defaultCdn)
                urlData.playbackUrl = hls.getString("url")
            }
            urlData.type = "hls"
        } else if (files.has("progressive")) {
            urlData.type = "other"
            val progressive = files.getJSONArray("progressive")
            val format = progressive.getJSONObject(0)
            urlData.playbackUrl = format.getString("url")
        }
        return urlData
    }

}