package androidx.coordinatorlayout.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View

open class CoordinatorLayoutExt : CoordinatorLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun hasOverlappingRendering(): Boolean = false

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        try {
            super.onNestedPreScroll(target, dx, dy, consumed, type)
        } catch (th: Throwable) {
            //
        }
    }

    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int) {
        try {
            super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type)
        } catch (th: Throwable) {
            //
        }
    }

}