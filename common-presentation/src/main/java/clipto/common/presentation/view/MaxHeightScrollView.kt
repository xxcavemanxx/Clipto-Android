package clipto.common.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

class MaxHeightScrollView : ScrollView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val newMaxHeight = MeasureSpec.makeMeasureSpec((resources.displayMetrics.heightPixels * 0.85f).toInt(), MeasureSpec.AT_MOST)
        super.onMeasure(widthMeasureSpec, newMaxHeight)
    }

}