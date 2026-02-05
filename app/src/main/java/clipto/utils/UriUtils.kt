package clipto.utils

import android.net.Uri

object UriUtils {

    fun getUri(url: String): Uri? {
        return runCatching { Uri.parse(url) }.getOrNull()
    }

    fun getUriHost(url: String): String? {
        return getUri(url)?.host
    }

}