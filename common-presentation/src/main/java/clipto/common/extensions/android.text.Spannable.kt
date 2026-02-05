package clipto.common.extensions

import android.text.Spannable
import android.text.style.URLSpan
import android.text.util.Linkify
import androidx.core.text.util.LinkifyCompat
import androidx.core.util.PatternsCompat
import clipto.common.misc.GsonUtils
import java.util.*

val urlSchemes = arrayOf("https://", "http://", "rtsp://")

fun Spannable.findFirstPhone(): String? = getSpans(0, length, URLSpan::class.java)
        .firstOrNull { it.url.startsWith("tel:") }?.let { it.url.substring("tel:".length) }

fun Spannable.findFirstEmail(): String? = getSpans(0, length, URLSpan::class.java)
        .firstOrNull { it.url.startsWith("mailto:") }?.let { it.url.substring("mailto:".length) }

fun Spannable.findFirstWebUrl(): String? = getSpans(0, length, URLSpan::class.java)
        .firstOrNull { it.url.startsWith(urlSchemes[0]) || it.url.startsWith(urlSchemes[1]) || it.url.startsWith(urlSchemes[2]) }
        ?.let { it.url }

fun Spannable.withPhonesAndEmails(): Spannable {
    LinkifyCompat.addLinks(this, Linkify.PHONE_NUMBERS or Linkify.EMAIL_ADDRESSES)
    return this
}

fun Spannable.withUrls(): Spannable {
    LinkifyCompat.addLinks(
            this,
            PatternsCompat.AUTOLINK_WEB_URL,
            "https://",
            urlSchemes,
            Linkify.MatchFilter { s, start, end ->
                if (Linkify.sUrlMatchFilter.acceptMatch(s, start, end)) {
                    val str = s.subSequence(start, end)
//                    val prevChar = s.getOrNull(start - 1)
//                    val nextChar = s.getOrNull(end)
                    if (!str.startsWith("http") && !str.startsWith("rtsp") && !str.startsWith("ftp")) {
//                        if ((prevChar != null && !prevChar.isWhitespace()) || (nextChar != null && !nextChar.isWhitespace())) {
//                            return@MatchFilter false
//                        }
//                        val dotCount = str.count { it == '.' }
//                        if (dotCount == 1) {
//                            return@MatchFilter true
//                        }
                        return@MatchFilter false
                    }
                    var openPlaceholder = s.subSequence(0, start).lastIndexOf("{{")
                    if (openPlaceholder != -1) {
                        var closePlaceholder = s.subSequence(end, s.length).indexOf("}}")
                        if (closePlaceholder != -1) {
                            openPlaceholder += 1
                            closePlaceholder += end + 1
                            val json = s.substring(openPlaceholder, closePlaceholder)
                            val valid = GsonUtils.toObjectSilent(json, Map::class.java) != null
                            return@MatchFilter !valid
                        }
                    }
                    return@MatchFilter true
                }
                false
            },
            null
    )
    return this
}