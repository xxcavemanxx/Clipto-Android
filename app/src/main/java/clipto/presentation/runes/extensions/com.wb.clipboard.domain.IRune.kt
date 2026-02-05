package clipto.presentation.runes.extensions

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import clipto.common.misc.ThemeUtils
import clipto.domain.IRune
import clipto.extensions.getTextColorPrimary
import clipto.extensions.getTextColorSecondary
import com.wb.clipboard.R

fun IRune.getTextColor(context: Context, isActive: Boolean): Int {
    return if (isActive) {
        context.getTextColorPrimary()
    } else {
        context.getTextColorSecondary()
    }
}

fun IRune.getIconColor(context: Context, isActive: Boolean): Int {
    return if (isActive) {
        Color.parseColor(getColor())
    } else {
        context.getTextColorSecondary()
    }
}

fun IRune.getBgColor(context: Context, isActive: Boolean): Int {
    return if (isActive) {
        ColorUtils.setAlphaComponent(Color.parseColor(getColor()), 20)
    } else {
        ThemeUtils.getColor(context, R.attr.myBackgroundHighlight)
    }
}