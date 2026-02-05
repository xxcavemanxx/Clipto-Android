package clipto.presentation.settings

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import clipto.common.misc.ThemeUtils
import clipto.common.misc.Units

class SettingsDividerView : View {

    companion object {
        private val P = Units.DP.toPx(16f).toInt()
    }

    private var divider: Drawable? = null
    private var dividerHeight: Int = 0
    private var dividerBottom = 0
    private var dividerRight = 0
    private var dividerLeft = 0
    private var dividerTop = 0

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    override fun hasOverlappingRendering(): Boolean = false

    private fun init(context: Context) {
        divider = ThemeUtils.getDrawable(context, android.R.attr.listDivider)
        dividerHeight = divider?.intrinsicHeight ?: 0
    }

    override fun onDraw(canvas: Canvas) {
        dividerBottom = canvas.clipBounds.bottom
        dividerTop = dividerBottom - dividerHeight
        dividerRight = right - P
        dividerLeft = left + P
        divider?.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom)
        divider?.draw(canvas)
    }

    override fun getSuggestedMinimumHeight(): Int = dividerHeight

    override fun onConfigurationChanged(newConfig: Configuration?) {
        dividerBottom = 0
    }

}