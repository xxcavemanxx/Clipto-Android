package clipto.extensions

import android.view.View
import android.widget.EditTextExt
import clipto.common.extensions.showKeyboard

fun View.restoreFocus(position: Int? = null) {
    requestFocus()
    restorePosition(position)
}

fun View.restorePosition(position: Int? = null) {
    if (this is EditTextExt) {
        restoreLastTouchEvent(newPosition = position)
    }
}

fun View.restoreKeyboard() {
    showKeyboard {
        if (this is EditTextExt) {
            restoreLastTouchEvent()
        }
    }
}

fun View.enableWithAlpha(enabled: Boolean) {
    alpha = if (enabled) 1f else 0.6f
}