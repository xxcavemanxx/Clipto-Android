package clipto

import android.app.Application
import android.webkit.URLUtil
import clipto.analytics.Analytics
import clipto.common.logging.L
import clipto.common.misc.IntentUtils
import clipto.common.presentation.mvvm.model.DataLoadingState
import clipto.extensions.log
import clipto.presentation.preview.image.ImagePreviewActivity
import clipto.presentation.preview.link.LinkPreview
import clipto.presentation.preview.link.LinkPreviewState
import clipto.presentation.preview.video.VideoPreviewActivity
import clipto.presentation.preview.video.url.UrlExtractor
import clipto.store.app.AppState
import clipto.store.internet.InternetState
import clipto.store.main.MainState
import com.google.firebase.storage.StorageReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLinkPreviewManager @Inject constructor(
    app: Application,
    appState: AppState,
    mainState: MainState,
    internetState: InternetState,
    linkPreviewState: LinkPreviewState,
    urlExtractor: UrlExtractor
) {

    init {
        linkPreviewState.canShowPreview.getLiveChanges({
            val canShowPreview = it.value ?: return@getLiveChanges
            mainState.requestApplyListConfig { config -> config.copy(hideLinkPreviews = !canShowPreview) }
        })
        linkPreviewState.requestPreview.getLiveChanges({
            val linkPreview = it.value ?: return@getLiveChanges
            val url = linkPreview.url
            val imageUrl = linkPreview.imageUrl
            val playbackUrl = linkPreview.playbackUrl
            val previewType = linkPreview.getType()
            val title = linkPreview.title?.toString()
            previewType?.name?.let { Analytics.onPreview(it) }
            log("preview :: {}", linkPreview)
            when (previewType) {
                LinkPreview.Type.AUDIO,
                LinkPreview.Type.VIDEO -> {
                    if (!playbackUrl.isNullOrBlank()) {
                        L.log(this, "preview: playback url = {}", playbackUrl)
                        VideoPreviewActivity.play(app, playbackUrl)
                    } else if (url != null && (URLUtil.isFileUrl(url) || URLUtil.isContentUrl(url))) {
                        L.log(this, "preview: local url = {}", url)
                        VideoPreviewActivity.play(app, url)
                    } else if (imageUrl is StorageReference) {
                        L.log(this, "preview: ref={} -> mediaType={}", imageUrl, linkPreview.mediatype)
                        internetState.withInternet({
                            appState.setLoadingState()
                            imageUrl.downloadUrl
                                .addOnSuccessListener { VideoPreviewActivity.play(app, it.toString(), title) }
                                .addOnFailureListener {
                                    appState.setLoadingState(
                                        DataLoadingState.Error(
                                            code = it.message, message = it.localizedMessage, throwable = it
                                        )
                                    )
                                }
                                .addOnCompleteListener { appState.setLoadedState() }
                        })
                    } else if (url != null) {
                        internetState.withInternet({
                            appState.setLoadingState()
                            urlExtractor.extract(app, url) { urlData ->
                                L.log(this, "extracted url: {}", urlData)
                                when {
                                    urlData.embedUrl != null -> {
                                        val newPlaybackUrl = urlData.embedUrl!!
                                        linkPreview.embedUrl = newPlaybackUrl
                                        VideoPreviewActivity.play(app, newPlaybackUrl, title, true)
                                    }
                                    urlData.playbackUrl != null -> {
                                        val newPlaybackUrl = urlData.playbackUrl!!
                                        linkPreview.playbackUrl = newPlaybackUrl
                                        VideoPreviewActivity.play(app, newPlaybackUrl, title, false)
                                    }
                                    else -> IntentUtils.open(app, url)
                                }
                                appState.setLoadedState()
                            }
                        })
                    }
                }
                LinkPreview.Type.IMAGE -> {
                    L.log(this, "check preview :: {}, {}", imageUrl, url)
                    (imageUrl ?: url)?.let {
                        ImagePreviewActivity.preview(
                            thumbUrl = linkPreview.thumbUrl,
                            title = title,
                            context = app,
                            url = it
                        )
                    }
                }
                else -> Unit
            }
        })
    }

}