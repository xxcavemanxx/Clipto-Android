package clipto.presentation.preview.link

import android.content.Context
import android.net.Uri
import android.webkit.URLUtil
import clipto.common.extensions.isContextDestroyed
import clipto.presentation.preview.link.extractor.CloudFunctionExtractor
import clipto.presentation.preview.link.extractor.OpenGraphExtractor
import clipto.presentation.preview.link.extractor.YoutubeDataApiExtractor
import clipto.store.internet.InternetState
import io.reactivex.android.schedulers.AndroidSchedulers
import org.jsoup.UnsupportedMimeTypeException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkPreviewFetcher @Inject constructor(
    private val internetState: InternetState,
    private val openGraphExtractor: OpenGraphExtractor,
    private val cloudFunctionExtractor: CloudFunctionExtractor,
    private val youtubeDataApiExtractor: YoutubeDataApiExtractor
) : ILinkPreviewFetcher {

    private val previewRepository: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val previewFetcher: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val previewCacheMap by lazy { hashMapOf<String, LinkPreview>() }
    private val extractors by lazy {
        listOf(
            youtubeDataApiExtractor,
            openGraphExtractor,
            cloudFunctionExtractor
        )
    }

    override fun clearPreview(spannable: LinkPreviewSpannable, url: String) {
        spannable.withPreviewCache?.remove(url)
        previewCacheMap.remove(url)
    }

    override fun fetchPreview(spannable: LinkPreviewSpannable, callback: (preview: LinkPreview) -> Unit) {
        val context = spannable.context
        val urlString = spannable.urlString
        val previewCache = spannable.withPreviewCache

        val safeCallback: (preview: LinkPreview) -> Unit = { runCatching { callback.invoke(it) } }
        val cacheable = previewCache !is LinkPreviewCache.NotCacheablePreview
        val cachedPreview =
            if (cacheable) {
                previewCacheMap[urlString]
            } else {
                previewCache?.get(urlString)
            }
        when {
            !spannable.withForcePreviews && !spannable.canPreviewLinks -> {
                spannable.defaultState()
                val preview = cachedPreview ?: LinkPreview(urlString)
                if (URLUtil.isNetworkUrl(urlString)) {
                    preview.sitename = preview.sitename ?: Uri.parse(urlString).host
                }
                safeCallback.invoke(preview)
            }
            cachedPreview != null && (cachedPreview.isValid() || !cacheable) -> {
                if (cachedPreview.isValid()) {
                    spannable.defaultState()
                    safeCallback.invoke(cachedPreview)
                } else {
                    spannable.defaultState()
                }
            }
            else -> {
                spannable.defaultState()
                previewRepository.execute {
                    runCatching {
                        val savedPreview = previewCache?.get(urlString)
                        when {
                            savedPreview != null -> {
                                previewCacheMap[urlString] = savedPreview
                                if (savedPreview.isValid()) {
                                    onUI(context) { safeCallback.invoke(savedPreview) }
                                }
                            }
                            !internetState.isConnected() -> {
                                val fetchedPreview = LinkPreview()
                                fetchedPreview.url = urlString
                                fetchedPreview.sitename = Uri.parse(urlString).host
                                onUI(context) { safeCallback.invoke(fetchedPreview) }
                            }
                            else -> {
                                onUI(context) { spannable.previewState() }
                                previewFetcher.execute {
                                    previewCacheMap[urlString]?.let {
                                        onUI(context) {
                                            if (it.isValid()) {
                                                safeCallback.invoke(it)
                                            } else {
                                                spannable.defaultState()
                                            }
                                        }
                                    } ?: run {
                                        val fetchedPreview = LinkPreview()
                                        fetchedPreview.url = urlString
                                        try {
                                            val extractor = extractors.find { it.canExtract(urlString) }!!
                                            extractor.extract(fetchedPreview)
                                            if (!fetchedPreview.isValid()) {
                                                onUI(context) { spannable.defaultState() }
                                            } else {
                                                previewCacheMap[urlString] = fetchedPreview
                                                previewCache?.put(fetchedPreview)
                                                onUI(context) { safeCallback.invoke(fetchedPreview) }
                                            }
                                        } catch (th: Throwable) {
                                            if (th is UnsupportedMimeTypeException) {
                                                if (th.mimeType.startsWith("image") || th.mimeType.startsWith("video")) {
                                                    fetchedPreview.sitename = Uri.parse(urlString).host
                                                    fetchedPreview.mediatype = th.mimeType
                                                    fetchedPreview.imageUrl = urlString
                                                    previewCacheMap[urlString] = fetchedPreview
                                                    previewCache?.put(fetchedPreview)
                                                    onUI(context) { safeCallback.invoke(fetchedPreview) }
                                                } else {
                                                    previewCacheMap[urlString] = fetchedPreview
                                                    previewCache?.put(fetchedPreview)
                                                    onUI(context) { spannable.defaultState() }
                                                }
                                            } else {
                                                onUI(context) { spannable.defaultState() }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onUI(context: Context, callback: () -> Unit) {
        if (context.isContextDestroyed()) return
        AndroidSchedulers.mainThread().scheduleDirect { callback.invoke() }
    }

}