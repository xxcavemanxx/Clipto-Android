package clipto.common.misc

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.SparseArray
import android.util.SparseIntArray
import androidx.annotation.AttrRes
import androidx.core.content.res.use
import clipto.common.R

object ThemeUtils {

    private val colorHexCache = mutableMapOf<String, Int>()
    private val colorCache: SparseIntArray = SparseIntArray()
    private val drawableCache: SparseArray<Drawable> = SparseArray()

    fun getColor(context: Context, hex: String?): Int {
        if (hex == null) return Color.TRANSPARENT
        return colorHexCache.getOrPut(hex) {
            try {
                Color.parseColor(hex)
            } catch (e: Exception) {
                getColor(context, android.R.attr.textColorPrimary)
            }
        }
    }

    fun getColor(context: Context, @AttrRes attrRes: Int): Int {
        var color = colorCache[attrRes]
        if (color == 0) {
            color = getTypedArray(context, attrRes).use { it.getColor(0, 0) }
            colorCache.append(attrRes, color)
        }
        return color
    }

    fun getDrawable(context: Context, @AttrRes attrRes: Int): Drawable? {
        var drawable = drawableCache[attrRes]
        if (drawable == null) {
            drawable = getTypedArray(context, attrRes).use { it.getDrawable(0) }
            if (drawable != null) {
                drawableCache.append(attrRes, drawable)
            }
        }
        return drawable
    }

    fun getDimensionPixelSize(context: Context, @AttrRes attrRes: Int): Float {
        val attr = getTypedArray(context, attrRes)
        return attr.use { it.getDimensionPixelSize(0, 0).toFloat() }
    }

    fun getColorAccent(context: Context): Int {
        return getColor(context, R.attr.colorAccent)
    }

    fun getColorPrimary(context: Context): Int {
        return getColor(context, R.attr.colorPrimary)
    }

    fun clearCache() {
        colorCache.clear()
        drawableCache.clear()
    }

    private fun getTypedArray(context: Context, themeAttr: Int): TypedArray {
        val theme = context.theme
        return theme.obtainStyledAttributes(intArrayOf(themeAttr))
    }

}
