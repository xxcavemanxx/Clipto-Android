package clipto.presentation.preview.video.url

import android.content.Context

class DefaultVideoUrlExtractor(context: Context) : AbstractVideoUrlExtractor(context) {

    override fun getVideoId(url: String): String? {
        if (url.endsWith(".mp4")) {
            return url
        }
        return null
    }

    override fun extractVideoData(url: String, videoId: String): UrlData? {
        return UrlData(playbackUrl = url)
    }

}