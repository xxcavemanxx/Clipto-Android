package clipto.presentation.preview.link.extractor

import clipto.common.extensions.notNull
import clipto.common.misc.GsonUtils
import clipto.presentation.preview.PreviewHelper
import clipto.presentation.preview.link.LinkPreview
import clipto.presentation.preview.link.LinkPreviewState
import clipto.store.app.AppState
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeDataApiExtractor @Inject constructor(
    private val appState: AppState,
    private val linkPreviewState: LinkPreviewState
) : LinkPreviewExtractor() {

    private val client by lazy { OkHttpClient() }
    private val signature by lazy { appState.getSignature().notNull() }
    private val packageName by lazy { appState.getPackageName() }

    override fun canExtract(url: String): Boolean = PreviewHelper.getYoutubeId(url) != null

    override fun extract(preview: LinkPreview) {
        val id = PreviewHelper.getYoutubeId(preview.url!!)!!
        val apiKey = linkPreviewState.appConfig.getYoutubeDataApiKey()
        val url = "https://youtube.googleapis.com/youtube/v3/videos?part=snippet&id=${id}&key=${apiKey}"
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Content-Type", "application/json; charset=UTF-8")
            .addHeader("X-Android-Package", packageName)
            .addHeader("X-Android-Cert", signature)
            .build()
        val resp = client.newCall(request).execute()
        val responseString = resp.body?.string() ?: return
        val response = GsonUtils.get().fromJson(responseString, Response::class.java)
        val snippet = response.items.firstOrNull()?.snippet ?: return
        preview.imageUrl = snippet.thumbnails?.getThumbnailUrl()
        preview.description = snippet.description
        preview.title = snippet.title
        preview.mediatype = "video"
    }

    private data class Response(
        @SerializedName("items") val items: List<Item>
    )

    private data class Item(
        @SerializedName("snippet") val snippet: Snippet?
    )

    private data class Snippet(
        @SerializedName("title") val title: String?,
        @SerializedName("description") val description: String?,
        @SerializedName("thumbnails") val thumbnails: Thumbnails?,
    )

    private data class Thumbnails(
        @SerializedName("default") val def: Thumbnail?,
        @SerializedName("medium") val medium: Thumbnail?,
        @SerializedName("high") val high: Thumbnail?,
        @SerializedName("standard") val standard: Thumbnail?
    ) {
        fun getThumbnailUrl(): String? = standard?.url ?: high?.url ?: medium?.url ?: def?.url
    }

    private data class Thumbnail(
        @SerializedName("url") val url: String
    )

}