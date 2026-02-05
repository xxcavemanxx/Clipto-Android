package clipto.presentation.preview

import android.util.Patterns
import java.util.regex.Pattern

object PreviewHelper {

    private val youtubePattern by lazy { Pattern.compile("(?:youtube(?:-nocookie)?\\.com/(?:[^/\\n\\s]+/\\S+/|(?:v|e(?:mbed)?)/|\\S*?[?&]v=)|youtu\\.be/)([a-zA-Z0-9_-]{11})") }

    private val webUrlRegex by lazy { Patterns.WEB_URL.toRegex() }

    fun isUrl(url: CharSequence?): Boolean = url != null && url.matches(webUrlRegex)

    fun getYoutubeId(url: String): String? {
        val matcher = youtubePattern.matcher(url)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

}