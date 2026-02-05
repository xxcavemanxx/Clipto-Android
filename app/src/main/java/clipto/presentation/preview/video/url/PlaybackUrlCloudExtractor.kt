package clipto.presentation.preview.video.url

import android.content.Context
import android.webkit.URLUtil
import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import clipto.common.logging.L

class PlaybackUrlCloudExtractor(context: Context) : AbstractVideoUrlExtractor(context) {

    override fun getVideoId(url: String): String? {
        if (URLUtil.isNetworkUrl(url)) {
            return url
        }
        return null
    }

    override fun extractVideoData(url: String, videoId: String): UrlData? {
        val params = mapOf<String, Any?>("url" to url)
        val func = FirebaseFunctions.getInstance().getHttpsCallable("getLinkPlaybackUrl")
        val resultData = Tasks.await(func.call(params)).data
        L.log(this, "extracted: {}", resultData)
        if (resultData is Map<*, *>) {
            return UrlData(
                    playbackUrl = resultData["playbackUrl"]?.toString(),
                    embedUrl = resultData["embedUrl"]?.toString()
            )
        }
        return null
    }

}