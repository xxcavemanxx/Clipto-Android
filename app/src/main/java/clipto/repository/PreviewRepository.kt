package clipto.repository

import android.net.Uri
import clipto.extensions.log
import clipto.presentation.preview.link.LinkPreview
import clipto.presentation.preview.link.LinkPreviewCache
import clipto.presentation.preview.link.extractor.CloudFunctionExtractor
import clipto.presentation.preview.link.extractor.OpenGraphExtractor
import clipto.presentation.preview.link.extractor.YoutubeDataApiExtractor
import clipto.store.internet.InternetState
import io.reactivex.Maybe
import io.reactivex.Single
import org.jsoup.UnsupportedMimeTypeException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreviewRepository @Inject constructor(
    private val internetState: InternetState,
    private val previewCache: LinkPreviewCache,
    private val openGraphExtractor: OpenGraphExtractor,
    private val cloudFunctionExtractor: CloudFunctionExtractor,
    private val youtubeDataApiExtractor: YoutubeDataApiExtractor
) : CacheableRepository(), IPreviewRepository {

    private val extractors by lazy {
        listOf(
            youtubeDataApiExtractor,
            openGraphExtractor,
            cloudFunctionExtractor
        )
    }

    override fun getPreview(url: String): Single<LinkPreview> = Maybe
        .fromCallable {
            val preview = previewCache.get(url)
            log("getPreview :: {} -> {}", url, preview?.imageUrl)
            if (preview == null) {
                val newPreview = LinkPreview(url)
                if (internetState.isConnected()) {
                    try {
                        val extractor = extractors.find { it.canExtract(url) }!!
                        extractor.extract(newPreview)
                    } catch (th: Throwable) {
                        if (th is UnsupportedMimeTypeException) {
                            if (th.mimeType.startsWith("image") || th.mimeType.startsWith("video")) {
                                newPreview.sitename = Uri.parse(url).host
                                newPreview.mediatype = th.mimeType
                                newPreview.imageUrl = url
                            }
                        }
                    }
                }
                if (newPreview.isValid()) {
                    previewCache.put(newPreview)
                }
                newPreview
            } else {
                preview
            }
        }
        .toSingle()
        .cached(url, CACHE_UNLIMITED)

}