package clipto.cache

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.SparseArray
import androidx.core.graphics.ColorUtils
import androidx.core.util.getOrElse
import clipto.common.logging.L
import clipto.common.misc.ThemeUtils
import com.google.android.material.chip.Chip
import com.wb.clipboard.R

object AppColorCache {

    private val chipBackgroundColorCache = SparseArray<ColorStateList>()
    private val chipStrokeColorCache = SparseArray<ColorStateList>()
    private val chipIconTintCache = SparseArray<ColorStateList>()
    private val colorOnSurfaceCache = SparseArray<Int>(100)

    fun clearCache() {
        chipBackgroundColorCache.clear()
        chipStrokeColorCache.clear()
        colorOnSurfaceCache.clear()
        chipIconTintCache.clear()
    }

    fun getColorOnSurface(color: Int): Int {
        var newColor = colorOnSurfaceCache.get(color)
        if (newColor == null) {
            newColor =
                if (ColorUtils.calculateLuminance(color) < 0.4) {
                    Color.WHITE
                } else {
                    Color.BLACK
                }
            colorOnSurfaceCache.put(color, newColor)
        }
        return newColor
    }

    fun updateColor(view: Chip?, color: Int, bgColor: Int = color, bgColorAttr: Int = R.attr.colorPrimaryInverse) {
        view?.let {
            val id = color + bgColorAttr
            it.chipBackgroundColor = chipBackgroundColorCache.getOrElse(id) {
                val state = ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_enabled, android.R.attr.state_selected),
                        intArrayOf()
                    ),
                    intArrayOf(
                        bgColor,
                        ThemeUtils.getColor(it.context, bgColorAttr)
                    )
                )
                log("updateColor: put to chipBackgroundColorCache: {}", color)
                chipBackgroundColorCache.put(id, state)
                state
            }
            it.chipStrokeColor = chipStrokeColorCache.getOrElse(color) {
                val state = ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_enabled, android.R.attr.state_selected),
                        intArrayOf()
                    ),
                    intArrayOf(
                        bgColor,
                        color
                    )
                )
                log("updateColor: put to chipStrokeColorCache: {}", color)
                chipStrokeColorCache.put(color, state)
                state
            }
            val iconColor = chipIconTintCache.getOrElse(color) {
                val state = ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_enabled, android.R.attr.state_selected),
                        intArrayOf()
                    ),
                    intArrayOf(
                        getColorOnSurface(color),
                        ThemeUtils.getColor(it.context, android.R.attr.textColorPrimary)
                    )
                )
                log("updateColor: put to chipIconTintCache: {}", color)
                chipIconTintCache.put(color, state)
                state
            }
            it.setTextColor(iconColor)
            it.chipIconTint = iconColor
        }
    }

    private fun log(text: String, vararg args: Any?) {
        L.log(AppColorCache.javaClass, text, *args)
    }

}