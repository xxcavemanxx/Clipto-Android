package clipto.common.presentation.text

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.Spanned
import android.text.style.ImageSpan
import android.widget.TextView
import java.lang.ref.WeakReference

class AnimatableDrawableCallback(
        private val viewRef: WeakReference<TextView>,
        private val span: ImageSpan
) : Drawable.Callback {

    override fun invalidateDrawable(who: Drawable) {
        viewRef.get()?.let { view ->
            val text = view.text
            if (text is Spannable && view.isAttachedToWindow) {
                val start = text.getSpanStart(span)
                val end = text.getSpanEnd(span)
                if (start != -1 && end != -1) {
                    text.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else if (who is Animatable) {
                    who.stop()
                }
            }
        } ?: run {
            if (who is Animatable) who.stop()
            viewRef.clear()
        }
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        viewRef.get()?.scheduleDrawable(who, what, `when`)
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        viewRef.get()?.unscheduleDrawable(who)
    }

}