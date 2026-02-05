package clipto.common.extensions

import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat

fun Drawable.tint(@ColorInt color: Int): Drawable {
    return if (this is VectorDrawable) {
        setTint(color)
        this
    } else {
        setColorFilter(color, PorterDuff.Mode.SRC_IN)
        val wrapDrawable = DrawableCompat.wrap(this).mutate()
        DrawableCompat.setTint(wrapDrawable, color)
        wrapDrawable
    }
}