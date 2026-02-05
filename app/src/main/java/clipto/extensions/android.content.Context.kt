package clipto.extensions

import android.content.Context
import android.text.format.DateFormat
import android.text.style.ForegroundColorSpan
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import clipto.common.misc.AndroidUtils
import clipto.common.misc.ThemeUtils
import com.google.android.material.timepicker.TimeFormat
import com.wb.clipboard.R

fun Context.getColorContext() = getColorAttr(R.attr.colorContext)
fun Context.getColorPrimaryInverse() = getColorAttr(R.attr.colorPrimaryInverse)
fun Context.getTextColorAccent() = getColorAttr(R.attr.colorAccent)
fun Context.getTextInactiveColor() = getColorAttr(R.attr.textInactiveColor)
fun Context.getTextColorPrimary() = getColorAttr(android.R.attr.textColorPrimary)
fun Context.getTextColorSecondary() = getColorAttr(android.R.attr.textColorSecondary)
fun Context.getActionIconColorHighlight() = getColorAttr(R.attr.actionIconColorHighlight)
fun Context.getActionIconColorInactive() = getColorAttr(R.attr.actionIconColorInactive)
fun Context.getActionIconColorNormal() = getColorAttr(R.attr.actionIconColorNormal)
fun Context.getBackgroundHighlightColor() = getColorAttr(R.attr.myBackgroundHighlight)
fun Context.getColorOnSurface() = getColorAttr(R.attr.colorOnSurface)
fun Context.getColorAttr(@AttrRes attrRes: Int) = ThemeUtils.getColor(this, attrRes)
fun Context.getSize(@AttrRes attrRes: Int) = ThemeUtils.getDimensionPixelSize(this, attrRes).toInt()
fun Context.getColorNegative() = ContextCompat.getColor(this, R.color.colorNegative)
fun Context.getColorPositive() = ContextCompat.getColor(this, R.color.colorPositive)
fun Context.getColorAttention() = ContextCompat.getColor(this, R.color.colorAttention)
fun Context.getColorHint() = ContextCompat.getColor(this, R.color.colorFloatingHint)
fun Context.getDisplayWidth() = AndroidUtils.getDisplaySize(this).x
fun Context.getActionIconSize() = getSize(R.attr.actionIconSize)
fun Context.getTextColorPrimarySpan() = ForegroundColorSpan(getTextColorPrimary())
fun Context.getTextColorSecondarySpan() = ForegroundColorSpan(getTextColorSecondary())
fun Context.getTextColorAccentSpan() = ForegroundColorSpan(getTextColorAccent())
fun Context.getActionIconColorHighlightSpan() = ForegroundColorSpan(getActionIconColorHighlight())
fun Context.getMaterialTimeFormatStyle() = if (DateFormat.is24HourFormat(this)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
