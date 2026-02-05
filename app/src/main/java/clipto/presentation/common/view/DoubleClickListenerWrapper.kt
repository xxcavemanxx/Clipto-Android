package clipto.presentation.common.view

import android.content.Context
import android.view.View
import android.widget.EditText
import clipto.common.extensions.isEditable

class DoubleClickListenerWrapper(
    val context: Context,
    private val isActivated: (v: View?) -> Boolean,
    private val listener: (v: View?) -> Unit
) : View.OnClickListener {

    private val threshold = 500L
    private var lastClickTime = 0L

    override fun onClick(v: View?) {
        if (isActivated.invoke(v)) {
            val prevClickTime = lastClickTime
            val currentClickTime = System.currentTimeMillis()
            if (currentClickTime - prevClickTime <= threshold) {
                lastClickTime = 0L
                if (v is EditText) {
                    v.setTextIsSelectable(true)
                }
                listener.invoke(v)
            } else {
                lastClickTime = currentClickTime
                if (v is EditText && !v.isEditable()) {
                    v.setTextIsSelectable(false)
                }
            }
        } else {
            listener.invoke(v)
        }
    }

}