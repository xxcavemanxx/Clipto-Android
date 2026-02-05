package clipto.extensions

import android.widget.TextView
import clipto.domain.Font

fun TextView.withConfig(fontId: Int, fontSize: Int) {
    Font.valueOf(fontId)?.apply(this, withLoadingState = false)
    val newTextSize = fontSize.toFloat()
    if (textSize != newTextSize) {
        textSize = newTextSize
    }
}