package clipto.common.presentation.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import clipto.common.misc.ThemeUtils

class DividerView : View {

    private var divider: Drawable? = null
    private var dividerHeight: Int = 0

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
        layoutParams?.height = dividerHeight
    }

    override fun onDraw(canvas: Canvas) {
        divider?.setBounds(0, 0, right, dividerHeight)
        divider?.draw(canvas)
    }

    override fun getSuggestedMinimumHeight(): Int = dividerHeight

}