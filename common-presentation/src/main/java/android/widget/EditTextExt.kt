package android.widget

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.appcompat.widget.AppCompatEditText
import clipto.common.presentation.text.SimpleSpanBuilder

open class EditTextExt : AppCompatEditText {

    init {
        setSpannableFactory(SimpleSpanBuilder.FACTORY)
        setEditableFactory(SimpleSpanBuilder.EDITABLE_FACTORY)
    }

    var fromUser = true
    var isReadOnly = false
    private var lastTouchEvent: Event? = null
    private var onTextContextMenuItem: ((id: Int) -> Unit)? = null

    val weakTextWatcherOne by lazy { WeakTextWatcher().also { addTextChangedListener(it) } }
    val weakTextWatcherTwo by lazy { WeakTextWatcher().also { addTextChangedListener(it) } }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setText(text: CharSequence?, fromUser: Boolean) {
        this.fromUser = fromUser
        setText(text)
    }

    fun consumeFromUser(): Boolean {
        val fromUser = this.fromUser
        this.fromUser = true
        return fromUser
    }

    fun getLastTouchEvent(): Event? = lastTouchEvent

    fun restoreLastTouchEvent(event: Event? = getLastTouchEvent(), newPosition:Int? = null) {
        if (event == null) return
        runCatching {
            val position = newPosition ?: event.position
            val length = length()
            if (position >= 0 && length - 1 >= position) {
                setSelection(position)
            } else if (length > 0) {
                setSelection(length)
            } else {
                setSelection(0)
            }
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        if (selStart > 0) {
            lastTouchEvent = lastTouchEvent?.also { it.position = selStart } ?: Event(0f, 0f, selStart)
        }
        super.onSelectionChanged(selStart, selEnd)
    }

    fun consumeLastTouchEvent(): Event? {
        val event = lastTouchEvent
        lastTouchEvent = null
        return event
    }

    fun setOnTextContextMenuItemCallback(callback: (id: Int) -> Unit) {
        this.onTextContextMenuItem = callback
    }

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
            //
            false
        }
    }

    override fun performLongClick(): Boolean {
        return try {
            super.performLongClick()
        } catch (th: Throwable) {
            //
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        try {
            if (event.action == MotionEvent.ACTION_DOWN) {
                val position = runCatching { getOffsetForPosition(event.x, event.y) }.getOrDefault(0)
                this.lastTouchEvent = Event(event.x, event.y, position)
                if (!isTextSelectable) setTextIsSelectable(true)
                takeIf { !it.hasFocus() }?.requestFocus()
            }
            return super.onTouchEvent(event)
        } catch (e: RuntimeException) {
            try {
                cancelPendingInputEvents()
                cancelLongPress()
                return true
            } finally {
                text = text
            }
        }
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        if (isReadOnly && id in arrayOf(android.R.id.cut, android.R.id.paste)) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            return false
        }
        val consumed = super.onTextContextMenuItem(id)
        onTextContextMenuItem?.invoke(id)
        return consumed
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return try {
            super.onKeyUp(keyCode, event)
        } catch (th: Throwable) {
            true
        }
    }

    data class Event(
        val x: Float,
        val y: Float,
        var position: Int,
        val time: Long = System.currentTimeMillis()
    )
}

fun EditTextExt?.clearSelection(): Boolean {
    if (this == null) return false
    try {
        if (hasSelection()) {
            val selection = selectionStart
            setSelection(selection, selection)
            return true
        }
    } catch (e: Exception) {
        return false
    }
    return false
}