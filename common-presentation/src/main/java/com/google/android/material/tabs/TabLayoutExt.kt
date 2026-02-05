package com.google.android.material.tabs

import android.content.Context
import android.util.AttributeSet

open class TabLayoutExt : TabLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun hasOverlappingRendering(): Boolean = false
}