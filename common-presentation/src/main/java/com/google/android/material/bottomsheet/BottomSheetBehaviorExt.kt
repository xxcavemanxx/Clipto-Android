package com.google.android.material.bottomsheet

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout

open class BottomSheetBehaviorExt<V : View> : BottomSheetBehavior<V> {

    constructor() : super()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
        return try {
            super.onInterceptTouchEvent(parent, child, event)
        } catch (e: Exception) {
            false
        }
    }

    override fun setStateInternal(state: Int) {
        try {
            super.setStateInternal(state)
        } catch (e: Exception) {
            //
        }
    }

}
