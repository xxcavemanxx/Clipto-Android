package clipto.presentation.preview.link.extractor

import android.net.Uri
import android.webkit.URLUtil
import clipto.config.IAppConfig
import clipto.presentation.preview.link.LinkPreview
import clipto.presentation.preview.link.LinkPreviewState
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenGraphExtractor @Inject constructor(
    private val appConfig: IAppConfig,
    private val linkPreviewState: LinkPreviewState
) : LinkPreviewExtractor() {

    override fun canExtract(url: String): Boolean = true

    override fun extract(preview: LinkPreview) = extract(preview, followRedirects = true)

    private fun extract(preview: LinkPreview, followRedirects: Boolean) {
        val url = preview.url!!
        val doc = Jsoup.connect(url)
            .timeout(linkPreviewState.getTimeout())
            .maxBodySize(appConfig.getMaxOpenGraphSizeInKb() * 1024)
            .followRedirects(true)
            .get()!!
        preview.title = getTitle(doc)
        preview.description = getDescription(doc)
        preview.mediatype = getMediaType(doc)
        preview.sitename = getSiteName(url, doc)
        preview.imageUrl = getImageUrl(url, doc)

        if (followRedirects && preview.imageUrl == null) {
            val titleUrl = preview.title?.toString()
            if (titleUrl != null && URLUtil.isValidUrl(titleUrl)) {
                val newPreview = LinkPreview()
                newPreview.url = titleUrl
                extract(newPreview, followRedirects = false)
                preview.title = newPreview.title
                preview.description = newPreview.description
                preview.mediatype = newPreview.mediatype
                preview.sitename = newPreview.sitename
                preview.imageUrl = newPreview.imageUrl
            }
        }
    }

    private fun metaTagContent(doc: Document, type: String, attr: String) = doc.select("meta[${attr}='${type}']")?.attr("content")?.takeIf { !it.isNullOrBlank() }

    private fun metaTag(doc: Document, type: String, attr: String) = doc.select("meta[${attr}='${type}']")?.takeIf { it.size > 0 }

    private fun getTitle(doc: Document): String? {
        return metaTagContent(doc, "og:title", "property")
            ?: metaTagContent(doc, "og:title", "name")
            ?: doc.title()
    }

    private fun getDescription(doc: Document): String? {
        return metaTagContent(doc, "description", "name")
            ?: metaTagContent(doc, "Description", "name")
            ?: metaTagContent(doc, "og:description", "property")
    }

    private fun getSiteName(url: String, doc: Document): String? {
        return metaTagContent(doc, "og:site_name", "property")
            ?: metaTagContent(doc, "og:site_name", "name")
            ?: Uri.parse(url).host
    }

    private fun getMediaType(doc: Document): String? {
        val node = metaTag(doc, "medium", "name")
        return if (node != null) {
            val content = node.attr("content")
            if (content == "image") "photo" else content
        } else {
            metaTagContent(doc, "og:type", "property")
        }
    }

    private fun getImageUrl(url: String, doc: Document): String? {
        val imageElements = metaTag(doc, "og:image", "property") ?: metaTag(doc, "og:image", "name")

        if (imageElements != null) {
            val image = imageElements.attr("content")
            if (!image.isNullOrBlank()) {
                return resolveURL(url, image)
            }
        }

        var src = doc.select("link[rel=image_src]")?.attr("href")
        if (!src.isNullOrBlank()) {
            return resolveURL(url, src)
        } else {
            src = doc.select("link[rel=apple-touch-icon]")?.attr("href")
            if (!src.isNullOrBlank()) {
                return resolveURL(url, src)
            } else {
                src = doc.select("link[rel=icon]")?.attr("href")
                if (!src.isNullOrBlank()) {
                    return resolveURL(url, src)
                }
            }
        }

        return null
    }

}