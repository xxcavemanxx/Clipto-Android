package clipto.presentation.preview.video.url

import android.content.Context
import clipto.common.logging.L
import clipto.common.presentation.mvvm.ViewModel
import clipto.common.presentation.mvvm.model.DataLoadingState
import clipto.store.app.AppState
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UrlExtractor @Inject constructor(val appState: AppState) {

    private val executor by lazy { Executors.newSingleThreadExecutor() }

    fun extract(context: Context, url: String, callback: (data: UrlData) -> Unit) {
        executor.execute {
            val extractors = listOf(
                    DefaultVideoUrlExtractor(context),
                    YoutubeVideoUrlExtractor(context),
                    VimeoVideoUrlExtractor(context),
                    AparatVideoUrlExtractor(context),
                    CoubVideoUrlExtractor(context),
//                    PVideoUrlExtractor(context),
                    PlaybackUrlCloudExtractor(context)
            )
            for (extractor in extractors) {
                val videoId = runCatching { extractor.getVideoId(url) }.getOrNull()
                appState.setLoadingState(DataLoadingState.LOADING)
                if (videoId != null) {
                    try {
                        L.log(UrlExtractor::class.java, "try to extract with {} -> {}", url, extractor)
                        val data = extractor.extractVideoData(url, videoId)
                        L.log(UrlExtractor::class.java, "extracted: {} -> {}", url, data)
                        val playbackUrl = data?.embedUrl ?: data?.playbackUrl
                        if (data != null && playbackUrl != null) {
                            val connection = URL(playbackUrl).openConnection()
                            if (connection is HttpURLConnection) {
                                connection.requestMethod = "HEAD"
                                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                                    callback.invoke(data)
                                    return@execute
                                }
                            } else {
                                callback.invoke(data)
                                return@execute
                            }
                        }
                    } catch (th: Throwable) {
                        L.log(UrlExtractor::class.java, "extract error", th)
                    }
                }
            }
            L.log(UrlExtractor::class.java, "return default url: {}", url)
            callback.invoke(UrlData())
        }
    }

}