package clipto.extensions

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

fun AccessibilityEvent?.isEvent(type: Int): Boolean = this?.eventType == type
fun AccessibilityEvent?.isViewClicked(): Boolean = isEvent(AccessibilityEvent.TYPE_VIEW_CLICKED)
fun AccessibilityEvent?.isWindowStateChanged(): Boolean = isEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
fun AccessibilityEvent?.isTextSelectionChanged(): Boolean = isEvent(AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED)
fun AccessibilityEvent?.isInternal(context: Context): Boolean = this?.packageName?.toString() == context.packageName
fun AccessibilityEvent?.isEditText(): Boolean = this?.className?.toString()?.contains("Edit") == true
fun AccessibilityEvent?.isEditable(): Boolean = this?.source?.let { it.isEditable && it.isVisibleToUser && it.isFocusable } ?: false
fun AccessibilityEvent?.isSystemPackage(): Boolean = this?.packageName?.startsWith("com.android.") == true || this?.packageName?.startsWith("android.") == true
fun AccessibilityEvent?.getTitle(): CharSequence? = this
        ?.let {
            val wrapper = AccessibilityNodeInfoCompat.wrap(source)
            val hint = wrapper.hintText ?: contentDescription
            if (!hint.isNullOrBlank()) {
                hint
            } else {
                it.className.substring(it.className.lastIndexOf('.') + 1)
            }
        }
