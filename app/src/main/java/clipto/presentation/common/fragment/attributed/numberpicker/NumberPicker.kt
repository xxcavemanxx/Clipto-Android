package clipto.presentation.common.fragment.attributed.numberpicker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.PointF
import android.os.Handler
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import clipto.common.extensions.disposeSilently
import com.wb.clipboard.R
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import it.sephiroth.android.library.uigestures.*
import it.sephiroth.android.library.xtooltip.ClosePolicy
import it.sephiroth.android.library.xtooltip.Tooltip
import kotlinx.android.synthetic.main.view_number_picker.view.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin


class NumberPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.pickerStyle,
    defStyleRes: Int = R.style.NumberPicker_None
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    interface OnNumberPickerChangeListener {
        fun onProgressChanged(numberPicker: NumberPicker, progress: Int, fromUser: Boolean)
        fun onStartTrackingTouch(numberPicker: NumberPicker)
        fun onStopTrackingTouch(numberPicker: NumberPicker)
    }

    var numberPickerChangeListener: OnNumberPickerChangeListener? = null

    lateinit var editText: EditText
    lateinit var upButton: ImageView
    lateinit var downButton: ImageView
    private lateinit var tracker: Tracker

    private var tooltipStyleId: Int

    private val delegate = UIGestureRecognizerDelegate()
    private lateinit var longGesture: UILongPressGestureRecognizer
    private lateinit var tapGesture: UITapGestureRecognizer
    private var disableGestures: Boolean = false

    private var tooltip: Tooltip? = null
    private var maxDistance: Int

    private lateinit var data: Data

    private var callback = { newValue: Int ->
        setProgress(newValue)
    }

    private var buttonInterval: Disposable? = null

    private val longGestureListener = { it: UIGestureRecognizer ->
        when (it.state) {
            UIGestureRecognizer.State.Began -> {
                requestFocus()
                editText.isSelected = false
                editText.clearFocus()

                tracker.begin(it.downLocationX, it.downLocationY)
                startInteraction()
            }
            UIGestureRecognizer.State.Ended -> {
                tracker.end()
                endInteraction()
            }
            UIGestureRecognizer.State.Changed -> {
                var diff =
                    if (data.orientation == VERTICAL) it.currentLocationY - it.downLocationY else it.currentLocationX - it.downLocationX
                if (diff > tracker.minDistance) {
                    diff = tracker.minDistance
                } else if (diff < -tracker.minDistance) {
                    diff = -tracker.minDistance
                }
                val final2 = sin((diff / tracker.minDistance) * Math.PI / 2).toFloat()

                tooltip?.let { tooltip ->
                    when (data.orientation) {
                        VERTICAL -> tooltip.offsetTo(tooltip.offsetX, final2 / 2 * tracker.minDistance)
                        HORIZONTAL -> tooltip.offsetTo(final2 / 2 * tracker.minDistance, tooltip.offsetY)
                    }
                }

                tracker.addMovement(it.currentLocationX, it.currentLocationY)
            }
        }
    }

    fun isFromTracker():Boolean = tracker.started

    private val tapGestureListener = { _: UIGestureRecognizer ->
        requestFocus()
        if (!editText.isFocused)
            editText.requestFocus()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setProgress(value: Int, fromUser: Boolean = true) {
        if (value != data.value) {
            data.value = value
            tooltip?.update(data.value.toString())

            if (editText.text.toString() != data.value.toString())
                editText.setText(data.value.toString())

            numberPickerChangeListener?.onProgressChanged(this, progress, fromUser)
        } else {
            editText.setText(data.value.toString())
        }
    }

    var progress: Int
        get() = data.value
        set(value) = setProgress(value, false)

    var minValue: Int
        get() = data.minValue
        set(value) {
            data.minValue = value
        }

    var maxValue: Int
        get() = data.maxValue
        set(value) {
            data.maxValue = value
            tilInput?.suffixText = "/ $value"
        }

    var stepSize: Int
        get() = data.stepSize
        set(value) {
            data.stepSize = value
        }

    init {
        setWillNotDraw(false)
        isFocusable = true
        isFocusableInTouchMode = true

        orientation = HORIZONTAL
        gravity = Gravity.CENTER

        val array = context.theme.obtainStyledAttributes(attrs, R.styleable.NumberPicker, defStyleAttr, defStyleRes)
        try {
            val maxValue = array.getInteger(R.styleable.NumberPicker_picker_max, 100)
            val minValue = array.getInteger(R.styleable.NumberPicker_picker_min, 0)
            val stepSize = array.getInteger(R.styleable.NumberPicker_picker_stepSize, 1)
            val orientation = array.getInteger(R.styleable.NumberPicker_picker_orientation, VERTICAL)
            val value = array.getInteger(R.styleable.NumberPicker_android_progress, 0)
            background = array.getDrawable(R.styleable.NumberPicker_android_background)
            tooltipStyleId = array.getResourceId(R.styleable.NumberPicker_picker_tooltipStyle, R.style.NumberPicker_ToolTipStyle)
            disableGestures = array.getBoolean(R.styleable.NumberPicker_picker_disableGestures, false)
            maxDistance = context.resources.getDimensionPixelSize(R.dimen.picker_distance_max)

            data = Data(value, minValue, maxValue, stepSize, orientation)

            tracker = when (array.getInteger(R.styleable.NumberPicker_picker_tracker, TRACKER_LINEAR)) {
                TRACKER_LINEAR -> LinearTracker(this, maxDistance, orientation, callback)
                TRACKER_EXPONENTIAL -> ExponentialTracker(this, maxDistance, orientation, callback)
                else -> LinearTracker(this, maxDistance, orientation, callback)
            }
            inflateChildren()

            editText.setText(data.value.toString())
        } finally {
            array.recycle()
        }

        initializeButtonActions()
        if (!disableGestures) {
            initializeGestures()
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        delegate.isEnabled = enabled
    }

    private fun hideKeyboard() {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun inflateChildren() {
        inflate(context, R.layout.view_number_picker, this)

        upButton = ivNext
        downButton = ivPrev

        editText = etInput
        editText.isFocusableInTouchMode = true
        editText.isFocusable = true
        editText.isClickable = true
        editText.isLongClickable = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initializeButtonActions() {
        upButton.setOnTouchListener { _, event ->
            if (!isEnabled) {
                false
            } else {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        requestFocus()
                        setProgress(progress + stepSize)
                        editText.clearFocus()
                        hideKeyboard()

                        upButton.requestFocus()
                        upButton.isPressed = true
                        buttonInterval.disposeSilently()
                        buttonInterval = Observable.interval(
                            ARROW_BUTTON_INITIAL_DELAY,
                            ARROW_BUTTON_FRAME_DELAY,
                            TimeUnit.MILLISECONDS,
                            Schedulers.io()
                        )
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { setProgress(progress + stepSize) }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        upButton.isPressed = false
                        buttonInterval.disposeSilently()
                        buttonInterval = null
                    }
                }

                true
            }
        }

        downButton.setOnTouchListener { _, event ->
            if (!isEnabled) {
                false
            } else {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        requestFocus()
                        setProgress(progress - stepSize)
                        editText.clearFocus()
                        hideKeyboard()

                        downButton.requestFocus()
                        downButton.isPressed = true
                        buttonInterval.disposeSilently()

                        buttonInterval = Observable.interval(
                            ARROW_BUTTON_INITIAL_DELAY,
                            ARROW_BUTTON_FRAME_DELAY,
                            TimeUnit.MILLISECONDS,
                            Schedulers.io()
                        )
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                setProgress(progress - stepSize)
                            }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        downButton.isPressed = false

                        buttonInterval.disposeSilently()
                        buttonInterval = null


                    }
                }

                true
            }
        }

        editText.setOnFocusChangeListener { _, hasFocus ->
            setBackgroundFocused(hasFocus)

            if (!hasFocus) {
                if (!editText.text.isNullOrEmpty()) {
                    setProgress(Integer.valueOf(editText.text.toString()), true)
                } else {
                    editText.setText(data.value.toString())
                }
            }
        }

        editText.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    editText.clearFocus()
                    true
                }
                else -> false
            }
        }
    }

    private fun setBackgroundFocused(hasFocus: Boolean) {
        if (hasFocus) {
            background?.state = FOCUSED_STATE_ARRAY
        } else {
            background?.state = UNFOCUSED_STATE_ARRAY
        }
    }

    private fun initializeGestures() {
        longGesture = UILongPressGestureRecognizer(context)
        longGesture.longPressTimeout = LONG_TAP_TIMEOUT
        longGesture.actionListener = longGestureListener
        longGesture.cancelsTouchesInView = false

        tapGesture = UITapGestureRecognizer(context)
        tapGesture.cancelsTouchesInView = false

        delegate.addGestureRecognizer(longGesture)
        delegate.addGestureRecognizer(tapGesture)

        tapGesture.actionListener = tapGestureListener

        delegate.isEnabled = isEnabled

        editText.setGestureDelegate(delegate)
    }

    private fun startInteraction() {
        animate().alpha(0.5f).start()

        tooltip = Tooltip.Builder(context)
            .anchor(editText, 0, 0, false)
            .styleId(tooltipStyleId)
            .arrow(true)
            .closePolicy(ClosePolicy.TOUCH_NONE)
            .overlay(false)
            .showDuration(0)
            .text(if (minValue.toString().length > maxValue.toString().length) minValue.toString() else maxValue.toString())
            .animationStyle(if (orientation == VERTICAL) R.style.NumberPicker_AnimationVertical else R.style.NumberPicker_AnimationHorizontal)
            .create()

        tooltip?.doOnPrepare { tooltip ->
            tooltip.contentView?.let { contentView ->
                val textView = contentView.findViewById<TextView>(android.R.id.text1)
                textView.measure(0, 0)
                textView.minWidth = textView.measuredWidth
            }
        }

        tooltip?.doOnShown { it.update(data.value.toString()) }
        tooltip?.show(this, if (data.orientation == VERTICAL) Tooltip.Gravity.LEFT else Tooltip.Gravity.TOP, false)

        numberPickerChangeListener?.onStartTrackingTouch(this)

    }

    private fun endInteraction() {
        animate().alpha(1.0f).start()

        tooltip?.dismiss()
        tooltip = null

        numberPickerChangeListener?.onStopTrackingTouch(this)
    }

    companion object {

        const val TRACKER_LINEAR = 0
        const val TRACKER_EXPONENTIAL = 1

        const val ARROW_BUTTON_INITIAL_DELAY = 350L
        const val ARROW_BUTTON_FRAME_DELAY = 100L
        const val LONG_TAP_TIMEOUT = 100L

        val FOCUSED_STATE_ARRAY = intArrayOf(android.R.attr.state_focused)
        val UNFOCUSED_STATE_ARRAY = intArrayOf(0, -android.R.attr.state_focused)


    }
}


class Data(value: Int, minValue: Int, maxValue: Int, var stepSize: Int, val orientation: Int) {
    var value: Int = value
        set(value) {
            field = max(minValue, min(maxValue, value))
        }

    var maxValue: Int = maxValue
        set(newValue) {
            if (newValue < minValue) throw IllegalArgumentException("maxValue cannot be less than minValue")
            field = newValue
            if (value > newValue) value = newValue
        }

    var minValue: Int = minValue
        set(newValue) {
            if (newValue > maxValue) throw IllegalArgumentException("minValue cannot be great than maxValue")
            field = newValue
            if (newValue > value) value = newValue
        }

}

internal abstract class Tracker(
    val numberPicker: NumberPicker,
    private val maxDistance: Int,
    val orientation: Int,
    val callback: (Int) -> Unit
) {

    internal var started: Boolean = false
    internal var initialValue: Int = 0
    internal var downPosition: Float = 0f

    @Suppress("MemberVisibilityCanBePrivate")
    internal var minPoint = PointF(0f, 0f)

    open fun begin(x: Float, y: Float) {
        calcDistance()

        downPosition = if (orientation == LinearLayout.VERTICAL) -y else x
        minPoint.set((-minDistance), (-minDistance))
        initialValue = numberPicker.progress
        started = true
    }

    abstract fun addMovement(x: Float, y: Float)

    open fun end() {
        started = false
    }

    var minDistance: Float = 0f

    private fun calcDistance() {
        val loc = intArrayOf(0, 0)
        val metrics = numberPicker.resources.displayMetrics
        numberPicker.getLocationOnScreen(loc)
        loc[0] += numberPicker.width / 2
        loc[1] += numberPicker.height / 2

        minDistance = if (orientation == LinearLayout.VERTICAL) {
            min(maxDistance, min(loc[1], metrics.heightPixels - loc[1])).toFloat()
        } else {
            min(maxDistance, min(loc[0], metrics.widthPixels - loc[0])).toFloat()
        }
    }
}

internal class ExponentialTracker(
    numberPicker: NumberPicker,
    maxDistance: Int,
    orientation: Int,
    callback: (Int) -> Unit
) : Tracker(numberPicker, maxDistance, orientation, callback) {

    private var time: Long = 1000L
    private var direction: Int = 0

    private val handler = Handler()

    private var runnable: Runnable = object : Runnable {
        override fun run() {
            if (!started) return

            if (direction > 0)
                callback.invoke(numberPicker.progress + numberPicker.stepSize)
            else if (direction < 0)
                callback.invoke(numberPicker.progress - numberPicker.stepSize)

            if (started)
                handler.postDelayed(this, time)
        }
    }

    override fun begin(x: Float, y: Float) {
        super.begin(x, y)
        direction = 0
        time = MAX_TIME_DELAY
        handler.post(runnable)
    }

    override fun addMovement(x: Float, y: Float) {
        val currentPosition = if (orientation == LinearLayout.VERTICAL) -y else x

        val diff: Float = max(-minDistance, min(currentPosition - downPosition, minDistance))
        val perc: Float = (diff / minDistance)

        direction = when {
            perc > 0 -> 1
            perc < 0 -> -1
            else -> 0
        }

        time = (MAX_TIME_DELAY - ((MAX_TIME_DELAY - MIN_TIME_DELAY).toFloat() * abs(perc))).toLong()
    }

    override fun end() {
        super.end()
        handler.removeCallbacks(runnable)
    }

    companion object {
        const val MAX_TIME_DELAY = 200L
        const val MIN_TIME_DELAY = 100L
    }
}

internal class LinearTracker(
    numberPicker: NumberPicker,
    maxDistance: Int,
    orientation: Int,
    callback: (Int) -> Unit
) : Tracker(numberPicker, maxDistance, orientation, callback) {


    override fun addMovement(x: Float, y: Float) {
        val currentPosition = if (orientation == LinearLayout.VERTICAL) -y else x

        val diff: Float = max(-minDistance, min(currentPosition - downPosition, minDistance))
        val perc: Float = (diff / minDistance)
        var finalValue: Int = initialValue + (abs(numberPicker.maxValue - numberPicker.minValue) * perc).toInt()

        var diffValue = finalValue - numberPicker.progress

        if (numberPicker.stepSize > 1) {
            if (diffValue % numberPicker.stepSize != 0) {
                diffValue -= (diffValue % numberPicker.stepSize)
            }
        }

        finalValue = numberPicker.progress + diffValue

        callback.invoke(finalValue)
    }
}

