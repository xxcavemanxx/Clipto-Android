package clipto.presentation.preview.video.url

import android.content.Context
import android.util.SparseArray
import clipto.presentation.preview.video.url.youtube.VideoMeta
import clipto.presentation.preview.video.url.youtube.YouTubeExtractor
import clipto.presentation.preview.video.url.youtube.YtFile
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class YoutubeVideoUrlExtractor(context: Context) : AbstractYoutubeVideoUrlExtractor(context) {

    override fun extractVideoData(url: String, videoId: String): UrlData? {
        val countDownLatch = CountDownLatch(1)
        val urlData = UrlData()
        YouTubeExtractorImpl(context, countDownLatch, urlData).extract("http://youtube.com/watch?v=$videoId", true, true)
        countDownLatch.await(10, TimeUnit.SECONDS)
        return urlData
    }

    private class YouTubeExtractorImpl(context: Context, val countDownLatch: CountDownLatch, val urlData: UrlData) : YouTubeExtractor(context) {
        override fun onExtractionComplete(ytFiles: SparseArray<YtFile>?, videoMeta: VideoMeta?) {
            val url = ytFiles?.get(22)?.url ?: ytFiles?.valueAt(0)?.url
            urlData.playbackUrl = url
            countDownLatch.countDown()
        }
    }

}