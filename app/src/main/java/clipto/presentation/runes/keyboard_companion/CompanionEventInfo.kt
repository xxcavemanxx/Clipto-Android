package clipto.presentation.runes.keyboard_companion

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import clipto.extensions.getInputText

data class CompanionEventInfo(
        val event: AccessibilityEvent? = null,
        private val nodeInfoRef: AccessibilityNodeInfo? = event?.source,
        private val nodeId: Int = nodeInfoRef?.hashCode() ?: 0,
        private val eventType: Int? = event?.eventType,
        private val eventText: List<CharSequence>? = event?.text,
        private val eventClassName: CharSequence? = event?.className,
        private val eventPackageName: CharSequence? = event?.packageName ?: nodeInfoRef?.packageName) {

    val nodeInfo: AccessibilityNodeInfoCompat? = (nodeInfoRef ?: event?.source)?.let { AccessibilityNodeInfoCompat.wrap(it) }

    fun freshNodeInfo(): AccessibilityNodeInfoCompat? {
        nodeInfo?.refresh()
        return nodeInfo
    }

    // event
    fun getClassName(): CharSequence? = eventClassName
    fun getEventText(): List<CharSequence>? = eventText
    fun isEditText(): Boolean = eventClassName?.toString()?.contains("Edit") == true
    fun isViewClicked(): Boolean = eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
    fun isViewFocused(): Boolean = eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED
    fun isViewTextChanged(): Boolean = eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
    fun isViewTextSelectionChanged(): Boolean = eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED

    // node
    fun getId(): Int = nodeId
    fun getPackageName(): CharSequence? = eventPackageName
    fun getTextSelectionStart(): Int = nodeInfo?.textSelectionStart ?: 0
    fun getTextSelectionEnd(): Int = nodeInfo?.textSelectionEnd ?: 0
    fun getText(): CharSequence? = nodeInfo?.getInputText()
    fun getTextLength(): Int = getText()?.length ?: 0

    fun getTitle(): CharSequence? {
        val hint = nodeInfo?.hintText ?: nodeInfo?.contentDescription
        return if (!hint.isNullOrBlank()) {
            hint
        } else {
            null
        }
    }

}