package clipto.common.presentation.text

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable

import android.text.style.ImageSpan
import androidx.annotation.NonNull

class VerticalImageSpan(drawable: Drawable) : ImageSpan(drawable), MyCustomSpan {

    override fun getSize(paint: Paint, text: CharSequence?, start: Int, end: Int,
                         fontMetricsInt: Paint.FontMetricsInt?): Int {
        val drawable = drawable
        val rect: Rect = drawable.bounds
        if (fontMetricsInt != null) {
            val fmPaint: Paint.FontMetricsInt = paint.fontMetricsInt
            val fontHeight: Int = fmPaint.descent - fmPaint.ascent
            val drHeight: Int = rect.bottom - rect.top
            val centerY: Int = fmPaint.ascent + fontHeight / 2
            fontMetricsInt.ascent = centerY - drHeight / 2
            fontMetricsInt.top = fontMetricsInt.ascent
            fontMetricsInt.bottom = centerY + drHeight / 2
            fontMetricsInt.descent = fontMetricsInt.bottom
        }
        return rect.right
    }

    override fun draw(@NonNull canvas: Canvas, text: CharSequence?, start: Int, end: Int,
                      x: Float, top: Int, y: Int, bottom: Int, @NonNull paint: Paint) {
        val drawable = drawable
        canvas.save()
        val fmPaint: Paint.FontMetricsInt = paint.fontMetricsInt
        val fontHeight: Int = fmPaint.descent - fmPaint.ascent
        val centerY: Int = y + fmPaint.descent - fontHeight / 2
        val transY = centerY - (drawable.bounds.bottom - drawable.bounds.top) / 2f
        canvas.translate(x, transY)
        drawable.draw(canvas)
        canvas.restore()
    }
}