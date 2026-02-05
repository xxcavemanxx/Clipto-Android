package clipto.extensions

import android.content.Context
import clipto.AppContext
import clipto.common.misc.ThemeUtils
import com.wb.clipboard.R

fun String.getTagBackgroundColor(context: Context, defaultColor: Int? = null): Int {
    val tag = AppContext.get().getFilters().findFilterByTagId(this)
    return tag?.color?.let { ThemeUtils.getColor(context, it) }
        ?: defaultColor ?: ThemeUtils.getColor(context, R.attr.colorCustomTagBackground)
}

fun String.getTagChipColor(context: Context, withDefault: Boolean = false): Int? {
    val tag = AppContext.get().getFilters().findFilterByTagId(this)
    return tag?.getTagChipColor(context) ?: run {
        if (withDefault) ThemeUtils.getColor(context, android.R.attr.textColorPrimary) else null
    }
}
