package clipto.presentation.preview.video.url

import android.content.Context
import android.webkit.URLUtil

class PlaybackUrlCloudExtractor(context: Context) : AbstractVideoUrlExtractor(context) {

    override fun getVideoId(url: String): String? {
        return null
    }

    override fun extractVideoData(url: String, videoId: String): UrlData? {
        return null
    }

}