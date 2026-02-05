package clipto.presentation.lockscreen

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import com.wb.clipboard.BuildConfig
import com.wb.clipboard.R

class IndicatorView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var selectedCount: Int = 0
        set(value) {
            field = value
            updateIndicatorsState()
        }

    private val indicators = mutableListOf<View>()

    init {
        inflateIndicators()
    }

    fun reset(delayed: Boolean = false) {
        waitReset(delayed) {}
    }

    fun onWrongCode() {
        waitReset { startAnimation(AnimationUtils.loadAnimation(context, R.anim.shake)) }
    }

    override fun onSaveInstanceState() =
            Bundle().apply {
                putInt(INSTANCE_STATE_SELECTED_COUNT, selectedCount)
                putParcelable(INSTANCE_STATE_SUPER, super.onSaveInstanceState())
            }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            selectedCount = state.getInt(INSTANCE_STATE_SELECTED_COUNT, 0)
            return super.onRestoreInstanceState(state.getParcelable(INSTANCE_STATE_SUPER))
        }
        super.onRestoreInstanceState(state)
    }

    private fun waitReset(delayed: Boolean = false, onResetComplete: () -> Unit) {
        if (delayed) {
            updateIndicatorsState()
            postDelayed({
                selectedCount = 0
                onResetComplete.invoke()
            }, 350)
        } else {
            selectedCount = 0
            onResetComplete.invoke()
        }
    }

    private fun inflateIndicators() {
        val inflater = LayoutInflater.from(context)
        for (i in 0 until INDICATORS_AMOUNT) {
            val view = inflater.inflate(R.layout.view_indicator, this, false)
            addView(view)
            indicators.add(view)
        }
        updateIndicatorsState()
    }

    private fun updateIndicatorsState() {
        indicators.forEachIndexed { index, v -> v.isActivated = selectedCount > index }
    }

    private companion object {
        const val INDICATORS_AMOUNT = BuildConfig.pinCodeLength
        const val INSTANCE_STATE_SELECTED_COUNT = "INSTANCE_STATE_SELECTED_COUNT"
        const val INSTANCE_STATE_SUPER = "INSTANCE_STATE_SUPER"
    }
}