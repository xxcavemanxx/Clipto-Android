package clipto.presentation.preview.link.extractor

import android.webkit.URLUtil
import java.net.URI

abstract class LinkPreviewExtractor : ILinkPreviewExtractor {

    protected fun resolveURL(url: CharSequence?, part: String): String? {
        return if (URLUtil.isValidUrl(part)) {
            part
        } else {
            var baseUri: URI? = null
            runCatching {
                baseUri = URI(url.toString())
                baseUri = baseUri!!.resolve(part)
            }
            baseUri?.toString()
        }
    }

}