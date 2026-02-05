package clipto.presentation.preview.video.url

import android.content.Context
import org.json.JSONObject
import java.util.*
import java.util.regex.Pattern

class CoubVideoUrlExtractor(context: Context) : AbstractVideoUrlExtractor(context) {

    companion object {
        private val regexp by lazy { Pattern.compile("(?:coub:|https?://(?:coub\\.com/(?:view|embed|coubs)/|c-cdn\\.coub\\.com/fb-player\\.swf\\?.*\\bcoub(?:ID|id)=))([\\da-z]+)") }
    }

    override fun getVideoId(url: String): String? {
        val matcher = regexp.matcher(url)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    override fun extractVideoData(url: String, videoId: String): UrlData? {
        val playerCode: String = downloadUrlContent(String.format(Locale.US, "https://coub.com/api/v2/coubs/%s.json", videoId))
                ?: return null
        val json = JSONObject(playerCode).getJSONObject("file_versions").getJSONObject("share")
        val video = decodeUrl(json.getString("default"))
        if (video != null) {
            return UrlData(playbackUrl = video, type = "other")
        }
        return null
    }

    private fun decodeUrl(input: String): String? {
//        val source = StringBuilder(input)
//        for (a in source.indices) {
//            val c = source[a]
//            val lower = Character.toLowerCase(c)
//            source.setCharAt(a, if (c == lower) Character.toUpperCase(c) else lower)
//        }
//        return try {
//            String(Base64.decode(source.toString(), Base64.DEFAULT), StandardCharsets.UTF_8)
//        } catch (ignore: Exception) {
//            null
//        }
        return input
    }

}