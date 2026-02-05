package clipto.common.extensions

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.SystemClock
import android.view.*
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.core.view.updateLayoutParams
import androidx.transition.*
import clipto.common.logging.L
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.transitionseverywhere.extra.Scale
import java.util.*
import kotlin.math.hypot

fun View.string(stringRes: Int): String = context.getString(stringRes)

fun View.string(stringRes: Int, vararg args: Any?): String = context.getString(stringRes, *args)

fun View.hapticKey() {
    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
}

fun View.hapticKeyRelease() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE)
    }
}

fun View.animateScale(visible: Boolean, container: ViewGroup? = null, callback: () -> Unit = {}) {
    if (isVisible() == visible) return
    clearAnimation()
    if (visible) {
        post {
            val set = TransitionSet()
                .setStartDelay(200).setDuration(500).addTransition(Scale(0.3f)).addTransition(Fade())
                .addTarget(this).setInterpolator(OvershootInterpolator())
                .addListener(object : TransitionListenerAdapter() {
                    override fun onTransitionEnd(transition: Transition) {
                        callback.invoke()
                    }
                })
            TransitionManager.beginDelayedTransition(container ?: (parent as ViewGroup), set)
            setVisibleOrGone(visible)
        }
    } else {
        setVisibleOrGone(visible)
    }
}

fun View.animateVisibleInvisible(visible: Boolean, container: ViewGroup? = null, callback: () -> Unit = {}) {
    if (isVisible() == visible) return
    clearAnimation()
    if (visible) {
        post {
            val set = TransitionSet()
                .setStartDelay(200).setDuration(500).addTransition(Scale(0.3f)).addTransition(Fade())
                .addTarget(this).setInterpolator(OvershootInterpolator())
                .addListener(object : TransitionListenerAdapter() {
                    override fun onTransitionEnd(transition: Transition) {
                        callback.invoke()
                    }
                })
            TransitionManager.beginDelayedTransition(container ?: (parent as ViewGroup), set)
            setVisible(visible)
        }
    } else {
        setVisible(false)
    }
}

fun View?.isVisible(): Boolean =
    this?.let {
        when (it.visibility) {
            View.INVISIBLE -> false
            View.VISIBLE -> true
            View.GONE -> false
            else -> true
        }
    } ?: false

fun View?.setVisibleOrGone(visible: Boolean): View? {
    this?.apply { visibility = if (visible) View.VISIBLE else View.GONE }
    return this
}

fun View?.setVisible(visible: Boolean) {
    this?.apply { visibility = if (visible) View.VISIBLE else View.INVISIBLE }
}

fun View.setDebounceClickListener(f: (view: View) -> Unit) {
    this.setOnClickListener(View.OnClickListener { v -> f.invoke(v) }.debounce())
}

fun View.setOnDoubleClick(listener: View.OnClickListener) {
    var lastClickTime = 0L
    val threadhold = 300L
    setOnClickListener {
        val prevClickTime = lastClickTime
        val currentClickTime = System.currentTimeMillis()
        if (currentClickTime - prevClickTime <= threadhold) {
            L.log(this, "onDoubleClick: {}", this)
            lastClickTime = 0L
            listener.onClick(this)
        } else {
            lastClickTime = currentClickTime
        }
    }
}

fun View.setBottomSheetHeight(
    height: Float? = null,
    hideable: Boolean = true,
    noBackground: Boolean = false,
    callback: (bottomSheet: BottomSheetBehavior<View>, peekHeight: Int, parentView: View) -> Unit = { _, _, _ -> }
) {
    val screenHeight = Resources.getSystem().displayMetrics.heightPixels
    val peekHeight = (screenHeight * (height ?: 0.75f)).toInt()
    val thisRef = this
    if (!hideable) {
        thisRef.updateLayoutParams {
            this.width = ViewGroup.LayoutParams.MATCH_PARENT
            this.height = peekHeight
        }
    }
    doOnFirstLayout {
        val parentRef = thisRef.parent as View
        if (noBackground) {
            parentRef.background = ColorDrawable(Color.TRANSPARENT)
        }
        BottomSheetBehavior.from(parentRef).apply {
            this.peekHeight = peekHeight
            this.isHideable = hideable
            callback.invoke(this, peekHeight, parentRef)
        }
    }
}

fun View.doOnFirstLayout(fail: () -> Unit = {}, success: () -> Unit) {
    viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            viewTreeObserver?.removeOnGlobalLayoutListener(this)
            success.invoke()
        }
    }) ?: fail.invoke()
}

fun View.touch(callback: () -> Unit = {}) {
    dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0f, 0f, 0))
    dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0f, 0f, 0))
    callback.invoke()
}

fun View?.showKeyboard(callback: () -> Unit = {}) {
    this?.run {
        runCatching { requestFocus() }
        postDelayed({
            try {
                val imm = this.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.takeIf { it.isActive }?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            } catch (e: Exception) {
                L.log("Extensions", "hideKeyboard: failed to show :${e.cause}")
            }
        }, 150)
        callback.invoke()
    }
}

fun View?.hideKeyboard() {
    this?.run {
        try {
            val imm = this.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.takeIf { it.isActive }?.hideSoftInputFromWindow(this.windowToken, 0)
            clearFocus()
        } catch (e: Exception) {
            L.log("Extensions", "hideKeyboard: failed to hide :${e.cause}")
        }
    }
}

fun View?.addOnKeyboardStateListener(skipInitialCallback: Boolean = false, callback: (visible: Boolean, displayHeight: Int, keyboardHeight: Int) -> Unit) {
    var prevVisible: Boolean? = null
    if (skipInitialCallback) prevVisible = false
    this?.viewTreeObserver?.addOnGlobalLayoutListener {
        val prevVisibleRef = prevVisible
        val decorView = parent
        if (decorView is View) {
            val r = Rect()
            decorView.getWindowVisibleDisplayFrame(r)
            val height = decorView.context.resources.displayMetrics.heightPixels
            val diff = height - r.bottom
            if (diff > 0) {
                if (prevVisibleRef == null || !prevVisibleRef) {
                    prevVisible = true
                    callback.invoke(true, height, diff)
                }
            } else if (prevVisibleRef == null || prevVisibleRef) {
                prevVisible = false
                callback.invoke(false, height, 0)
            }
        }
    }
}

fun View?.updateMargin(left: Int? = null, top: Int? = null, right: Int? = null, bottom: Int? = null) {
    this?.layoutParams?.let { params ->
        if (params is ViewGroup.MarginLayoutParams) {
            left?.let { params.marginStart = it }
            top?.let { params.topMargin = it }
            right?.let { params.marginEnd = it }
            bottom?.let { params.bottomMargin = it }
        }
    }
}

fun View?.animateVisibility(visible: Boolean, transition: Transition = Fade(), endListener: (() -> Unit)? = null) {
    this?.post {
        (parent as? ViewGroup)?.let {
            endListener?.let {
                transition.addTarget(this)
                transition.addListener(object : TransitionListenerAdapter() {
                    override fun onTransitionEnd(transition: Transition) {
                        endListener.invoke()
                    }
                })
            }
            TransitionManager.beginDelayedTransition(it, transition)
        }
        if (visible) visible() else gone()
    }
}

fun View?.visible(): View? {
    this?.visibility = View.VISIBLE
    return this
}

fun View?.invisible(): View? {
    this?.visibility = View.INVISIBLE
    return this
}

fun View?.gone(): View? {
    this?.visibility = View.GONE
    return this
}

fun View.registerDraggableTouchListener(
    initialPosition: () -> Point,
    positionListener: (x: Int, y: Int) -> Unit
) {
    WindowHeaderTouchListener(context, this, initialPosition, positionListener)
}

internal class WindowHeaderTouchListener(
    context: Context,
    view: View,
    private val initialPosition: () -> Point,
    private val positionListener: (x: Int, y: Int) -> Unit
) : View.OnTouchListener {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var pointerStartX = 0
    private var pointerStartY = 0
    private var initialX = 0
    private var initialY = 0
    private var moving = false
    private var timer: Timer? = null


    init {
        view.setOnTouchListener(this)
    }


    private fun cancelLongClickTimer() {
        timer?.cancel()
        timer = null
    }


    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {

        when (motionEvent.action) {

            MotionEvent.ACTION_DOWN -> {
                pointerStartX = motionEvent.rawX.toInt()
                pointerStartY = motionEvent.rawY.toInt()
                with(initialPosition()) {
                    initialX = x
                    initialY = y
                }
                moving = false
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = motionEvent.rawX - pointerStartX
                val deltaY = motionEvent.rawY - pointerStartY
                if (moving || hypot(deltaX, deltaY) > touchSlop) {
                    cancelLongClickTimer()
                    positionListener(initialX + deltaX.toInt(), initialY + deltaY.toInt())
                    moving = true
                }
            }

            MotionEvent.ACTION_UP -> {
                cancelLongClickTimer()
                if (!moving) {
                    view.performClick()
                }
            }

        }

        return true
    }

}