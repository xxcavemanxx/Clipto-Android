package clipto.presentation.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.View

class ShimmerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) {
            startAnimation(ShimmerLayout.shimmerAnimation)
        } else {
            clearAnimation()
        }
    }
}