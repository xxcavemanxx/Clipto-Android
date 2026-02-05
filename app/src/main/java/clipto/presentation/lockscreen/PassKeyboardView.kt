package clipto.presentation.lockscreen

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View.OnClickListener
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.wb.clipboard.BuildConfig
import com.wb.clipboard.R
import clipto.common.extensions.hapticKey
import clipto.common.extensions.setVisibleOrGone
import kotlinx.android.synthetic.main.view_passcode.view.*

class PassKeyboardView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var input: String = ""
    private val maxLength = BuildConfig.pinCodeLength

    init {
        inflate(context, R.layout.view_passcode, this)
        setDeleteButtonAppearance()
    }

    var keyboardListener: InputListener? = null
        set(value) {
            field = value
            setClickListeners()
        }

    var buttonTouchIdVisible: Boolean = false
        set(value) {
            field = value
            btnTouchId.setVisibleOrGone(value)
            setDeleteButtonAppearance()
        }

    fun reset() {
        input = ""
    }

    private val numberClickListener = OnClickListener { v ->
        when (v) {
            btn0 -> addNumber(0)
            btn1 -> addNumber(1)
            btn2 -> addNumber(2)
            btn3 -> addNumber(3)
            btn4 -> addNumber(4)
            btn5 -> addNumber(5)
            btn6 -> addNumber(6)
            btn7 -> addNumber(7)
            btn8 -> addNumber(8)
            btn9 -> addNumber(9)
            btnDelete -> deleteLast()
        }
        updateInputListener()
        setDeleteButtonAppearance()
        hapticKey()
    }

    private fun updateInputListener() {
        keyboardListener?.onInput(input.take(maxLength))

    }

    private val actionClickListener = OnClickListener { v ->
        when (v) {
            btnTouchId -> keyboardListener?.onTouchIdClick()
        }
    }

    private fun setClickListeners() {
        arrayOf(btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9, btn0, btnDelete)
                .forEach { it.setOnClickListener(numberClickListener) }

        arrayOf(btnTouchId)
                .forEach { it.setOnClickListener(actionClickListener) }
    }

    private fun setDeleteButtonAppearance() {
        val showDelete = !buttonTouchIdVisible
        TransitionManager.beginDelayedTransition(clParent, Fade().setDuration(250).addTarget(btnDelete))
        btnDelete.setVisibleOrGone(showDelete && input.isNotEmpty())
    }

    private fun addNumber(char: Int) {
        input = input.plus(char)
    }

    private fun deleteLast() {
        if (input.isNotEmpty()) {
            input = input.substring(0, input.length - 1)
        }
    }

    override fun onSaveInstanceState() =
            Bundle().apply {
                putString(INSTANCE_STATE_INPUT, input)
                putParcelable(INSTANCE_STATE_SUPER, super.onSaveInstanceState())
            }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            input = state.getString(INSTANCE_STATE_INPUT) ?: ""
            return super.onRestoreInstanceState(state.getParcelable(INSTANCE_STATE_SUPER))
        }
        super.onRestoreInstanceState(state)
    }

    private companion object {
        const val INSTANCE_STATE_INPUT = "INSTANCE_STATE_INPUT"
        const val INSTANCE_STATE_SUPER = "INSTANCE_STATE_SUPER"
    }

    interface InputListener {
        fun onInput(code: String)
        fun onForgotClicked() {}
        fun onTouchIdClick() {}
    }
}