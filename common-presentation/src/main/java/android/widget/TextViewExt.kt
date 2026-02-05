package android.widget

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo

open class TextViewExt : TextView {

    val weakTextWatcherOne by lazy { WeakTextWatcher().also { addTextChangedListener(it) } }
    val weakTextWatcherTwo by lazy { WeakTextWatcher().also { addTextChangedListener(it) } }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun addExtraDataToAccessibilityNodeInfo(info: AccessibilityNodeInfo, extraDataKey: String, arguments: Bundle?) {
        try {
            super.addExtraDataToAccessibilityNodeInfo(info, extraDataKey, arguments)
        } catch (th: Throwable) {
            //
        }
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        return try {
            super.performAccessibilityAction(action, arguments)
        } catch (th: Throwable) {
            false
        }
    }

    override fun performLongClick(): Boolean {
        return try {
            super.performLongClick()
        } catch (th: Throwable) {
            false
        }
    }

    override fun performClick(): Boolean {
        return try {
            super.performClick()
        } catch (th: Throwable) {
            false
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return try {
            super.onKeyUp(keyCode, event)
        } catch (th: Throwable) {
            true
        }
    }
}