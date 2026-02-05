package androidx.viewpager.widget

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import clipto.common.logging.L

class ViewPagerExt : ViewPager {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(null)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return try {
            super.onInterceptTouchEvent(ev)
        } catch (e: IllegalArgumentException) {
            L.log(this, "onInterceptTouchEvent error", e)
            false
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return try {
            super.dispatchKeyEvent(event)
        } catch (e: Exception) {
            false
        }
    }

    override fun hasOverlappingRendering(): Boolean = false
}