package clipto.common.extensions

import android.view.View

fun View.OnClickListener.debounce(timeoutInMillis: Int = 500): View.OnClickListener {
    val listener = this
    var lastClick: Long = 0
    return View.OnClickListener { view ->
        val current = System.currentTimeMillis()
        if (current - lastClick < timeoutInMillis) {
        } else {
            lastClick = current
            listener.onClick(view)
        }
    }
}