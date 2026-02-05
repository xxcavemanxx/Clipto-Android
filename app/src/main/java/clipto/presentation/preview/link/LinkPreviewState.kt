package clipto.presentation.preview.link

import clipto.config.IAppConfig
import clipto.store.StoreObject
import clipto.store.StoreState
import clipto.store.app.AppState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkPreviewState @Inject constructor(
        appConfig: IAppConfig,
        private val appState: AppState,
        private val linkPreviewCache: LinkPreviewCache
) : StoreState(appConfig) {

    val requestPreview by lazy {
        StoreObject<LinkPreview>(
                id = "open_preview_request",
                liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    val canShowPreview by lazy {
        StoreObject(
                id = "show_preview",
                initialValue = !appState.getSettings().hideLinkPreviews,
                liveDataChangeStrategy = StoreObject.LiveDataChangeStrategy.AUTO
        )
    }

    fun isPreviewEnabled() = !appState.getSettings().doNotPreviewLinks
    fun canPreviewLinks() = canShowPreview.requireValue()
    fun getLinkPreviewCache() = linkPreviewCache
    fun getTimeout(): Int = 5_000

}