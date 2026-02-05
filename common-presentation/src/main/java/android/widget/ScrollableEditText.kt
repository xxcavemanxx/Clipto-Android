package android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import clipto.common.logging.L
import kotlin.math.abs

class ScrollableEditText : EditTextExt {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val mScroller = OverScroller(context)
    private val mMinScroll = lineHeight / 2
    private var mScrollY = 0
    private val mFlingV = 750

    private var sHeight: Int = 0
    private var didMove = false
    private var mLastMotionY: Float = 0f
    private var mVelocityTracker: VelocityTracker? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        if (sHeight <= 0) {
            sHeight = layoutParams?.height ?: 0
        }
        super.onTouchEvent(event)
        if (mVelocityTracker == null) { // If we do not have velocity tracker
            mVelocityTracker = VelocityTracker.obtain() // then get one
        }
        mVelocityTracker?.addMovement(event) // add this movement to it
        val action = event.action // Get action type
        val y = event.y // Get the displacement for the action
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (!mScroller.isFinished) { // If scrolling, then stop now
                    mScroller.abortAnimation()
                }
                mLastMotionY = y // Save start (or end) of motion
                mScrollY = this.scrollY // Save where we ended up
                isCursorVisible = true
                didMove = false
            }
            MotionEvent.ACTION_MOVE -> {
                didMove = true
                val deltaY = (mLastMotionY - y).toInt() // Calculate distance moved since last report
                mLastMotionY = y // Save the start of this motion
                if (deltaY < 0) { // If user is moving finger up screen
                    if (mScrollY > 0) { // and we are not at top of text
                        var m = mScrollY - mMinScroll // Do not go beyond top of text
                        m = if (m < 0) {
                            mScrollY
                        } else mMinScroll
//                        scrollBy(0, -m) // Scroll the text up
                        scrollBy(0, deltaY)
                    }
                } else if (deltaY > 0) { // The user finger is moving up
                    val max: Int = lineCount * lineHeight - sHeight // Set max up value
                    if (mScrollY < max - mMinScroll) {
//                        scrollBy(0, mMinScroll) // Scroll up
                        scrollBy(0, deltaY)
                    }
                }
                postInvalidate()
            }
            MotionEvent.ACTION_UP -> {
                val velocityTracker = mVelocityTracker // Find out how fast the finger was moving
                velocityTracker?.computeCurrentVelocity(mFlingV)
                val velocityY = velocityTracker?.yVelocity?.toInt() ?: 0
                if (abs(velocityY) > mFlingV) { // if the velocity exceeds threshold
                    val maxY: Int = lineCount * lineHeight - sHeight // calculate maximum Y movement
                    L.log(this, "DO SCROLL: mScrollY={}, velocityY={}, maxY={}", mScrollY, velocityY, maxY)
                    mScroller.fling(0, mScrollY, 0, -velocityY, 0, 0, 0, maxY) // Do the filng
                } else {
                    if (mVelocityTracker != null) { // If the velocity less than threshold
                        mVelocityTracker?.recycle() // recycle the tracker
                        mVelocityTracker = null
                    }
                }
            }
        }
        mScrollY = this.scrollY
        return true
    }

    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            mScrollY = mScroller.currY
            scrollTo(0, mScrollY)
            postInvalidate()
        }
    }

}