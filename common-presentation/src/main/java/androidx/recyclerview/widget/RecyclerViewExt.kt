package androidx.recyclerview.widget

import android.content.Context
import android.util.AttributeSet

class RecyclerViewExt : RecyclerView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun hasOverlappingRendering(): Boolean = false
}