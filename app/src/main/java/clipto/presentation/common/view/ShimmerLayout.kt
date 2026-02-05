package clipto.presentation.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.constraintlayout.widget.ConstraintLayout

class ShimmerLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) {
            startAnimation(shimmerAnimation)
        } else {
            clearAnimation()
        }
    }

    companion object {
        val shimmerAnimation = AlphaAnimation(0.4f, 1f).apply {
            duration = 500
            startOffset = 500
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
    }
}