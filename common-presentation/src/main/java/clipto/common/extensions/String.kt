package clipto.common.extensions

import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder

private val textToSpannableCache = mutableMapOf<String, Spannable>()

fun String?.isFileExists(): Boolean =
    this?.let {
        File(it).exists()
    } ?: false

fun String?.toFile(): File? {
    return try {
        this?.let {
            val uri = Uri.parse(it)
            if (uri.scheme == "file") {
                uri.toFile()
            } else {
                null
            }
        }
    } catch (th: Throwable) {
        null
    }
}

fun String?.notNull(): String = this ?: ""

fun String?.dashIfNullOrEmpty(): String = this?.takeIf { it.isNotBlank() } ?: "-"

fun String?.ifNotEmpty(): String? = this?.takeIf { it.isNotEmpty() }

fun String?.length(): Int = this?.length ?: 0

fun String?.inBrackets(): String = "(${notNull()})"

fun String?.inDashes(): String = "|${notNull()}|"

fun String?.trimSpaces(): String? = this?.replace("\n ", "\n")

fun String?.toLinkifiedSpannable(): Spannable {
    val textRef = this ?: ""
    return textToSpannableCache.getOrPut(textRef) {
        SpannableString.valueOf(textRef)
            .withPhonesAndEmails()
            .withUrls()
    }
}

fun String?.getExtension(): String? {
    if (this == null) {
        return null
    }
    val index = lastIndexOf(".")
    if (index >= 0) {
        return substring(index + 1).toLowerCase()
    }
    return null
}

fun String?.getMimeType(ext: String?): String? {
    if (this == null || ext == null) {
        return null
    }
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    if (!mimeType.isNullOrBlank()) {
        return mimeType
    }
    return mimeTypes[ext]
}

fun String?.cutString(max: Int = 200): String? {
    if (this == null) {
        return null
    }
    if (length <= max) {
        return this
    }
    val lastIndex = indexOfAny(arrayListOf(".", "\n\n"), max, true)
    if (lastIndex != -1 && lastIndex < length - 1) {
        return "${substring(0, lastIndex)}…"
    }
    return this
}

fun String.getFirst(max: Int = 50): String {
    if (length <= max) {
        return this
    }
    return "${substring(0, max)}…"
}

fun String.encode(): String = runCatching { URLEncoder.encode(this, "UTF-8") }.getOrElse { this }
fun String.decode(): String = runCatching { URLDecoder.decode(this, "UTF-8") }.getOrElse { this }

private val mimeTypes = mapOf(
    "js" to "application/javascript",
    "json" to "application/json",
    "md" to "text/markdown",
    "mhtml" to "text/html"
)