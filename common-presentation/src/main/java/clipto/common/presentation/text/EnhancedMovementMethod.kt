package clipto.common.presentation.text

import android.graphics.Point
import android.text.Selection
import android.text.Spannable
import android.text.method.ArrowKeyMovementMethod
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.widget.TextView
import androidx.core.text.getSpans

object EnhancedMovementMethod : ArrowKeyMovementMethod() {

    override fun onTouchEvent(widget: TextView?, buffer: Spannable?, event: MotionEvent?): Boolean {
        // First make sure we are making a safe call
        if (event != null && widget != null && buffer != null) {
            // Return true only if event is handled
            if (handleMotion(event, widget, buffer)) {
                return true
            }
        }

        return super.onTouchEvent(widget, buffer, event)
    }

    /** Handle the motion action event for the widget and its text buffer */
    private fun handleMotion(event: MotionEvent, widget: TextView, buffer: Spannable): Boolean {
        var handled = false

        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_UP) {
            // Get click position
            val target = Point().apply {
                x = event.x.toInt() - widget.totalPaddingLeft + widget.scrollX
                y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
            }

            // Get span line and offset
            val line = widget.layout.getLineForVertical(target.y)
            var offset = widget.layout.getOffsetForHorizontal(line, target.x.toFloat())

            val maxTargetX = widget.layout.getPrimaryHorizontal(offset)
            if (target.x > maxTargetX) {
                offset += 1
            } else if (maxTargetX == 0f && target.x <= maxTargetX) {
                return false
            }

            if (event.action == MotionEvent.ACTION_DOWN) {
                // Highlight clickable span and return true if handled
                handled = handled || buffer.execute<ClickableSpan>(offset) {
                    Selection.setSelection(buffer, buffer.getSpanStart(it), buffer.getSpanEnd(it))
                }
            }

            if (event.action == MotionEvent.ACTION_UP) {
                // Handle clickable callbacks and return true if handled
                handled = handled || buffer.execute<ClickableSpan>(offset) {
                    it.onClick(widget)
                }
                if (handled) {
                    widget.cancelPendingInputEvents()
                    widget.cancelLongPress()
                }
            }
        }

        return handled
    }
}

private inline fun <reified T : Any> Spannable.execute(offset: Int, fn: (T) -> Unit): Boolean {
    val spans = getSpans<T>(offset, offset)
    if (spans.isNotEmpty()) {
        fn.invoke(spans.first())
        return true
    }
    return false
}