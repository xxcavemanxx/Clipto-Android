package androidx.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * https://www.androiddesignpatterns.com/2018/01/experimenting-with-nested-scrolling.html
 */
class NestedScrollViewExt : NestedScrollView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun hasOverlappingRendering(): Boolean = false

    // The NestedScrollView should steal the scroll/fling events away from
    // the RecyclerView if: (1) the user is dragging their finger down and
    // the RecyclerView is scrolled to the top of its content, or (2) the
    // user is dragging their finger up and the NestedScrollView is not
    // scrolled to the bottom of its content.

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        if (target is RecyclerView) {
            if (dy < 0 && isRvScrolledToTop(target) || dy > 0 && !isNsvScrolledToBottom(this)) {
                // Scroll the NestedScrollView's content and record the number of pixels consumed
                // (so that the RecyclerView will know not to perform the scroll as well).
                scrollBy(0, dy)
                consumed[1] = dy
                return
            }
        }
        super.onNestedPreScroll(target, dx, dy, consumed)
    }

    override fun onNestedPreFling(target: View, velX: Float, velY: Float): Boolean {
        if (target is RecyclerView) {
            if (velY < 0 && isRvScrolledToTop(target) || velY > 0 && !isNsvScrolledToBottom(this)) {
                // Fling the NestedScrollView's content and return true (so that the RecyclerView
                // will know not to perform the fling as well).
                fling(velY.toInt())
                return true
            }
        }
        return super.onNestedPreFling(target, velX, velY)
    }

    /**
     * Returns true iff the NestedScrollView is scrolled to the bottom of its
     * content (i.e. if the card's inner RecyclerView is completely visible).
     */
    private fun isNsvScrolledToBottom(nsv: NestedScrollView): Boolean {
        return !nsv.canScrollVertically(1)
    }

    /**
     * Returns true iff the RecyclerView is scrolled to the top of its
     * content (i.e. if the RecyclerView's first item is completely visible).
     */
    private fun isRvScrolledToTop(rv: RecyclerView): Boolean {
        if (rv.adapter?.itemCount == 0) {
            return true
        }
        val lm = rv.layoutManager as LinearLayoutManager
        return lm.findFirstVisibleItemPosition() == 0 && lm.findViewByPosition(0)?.top == 0
    }
}