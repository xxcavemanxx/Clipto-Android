package clipto.extensions

import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import clipto.AppContext
import kotlin.math.min

fun AccessibilityNodeInfoCompat?.isPassword(): Boolean = this?.isPassword ?: false
fun AccessibilityNodeInfoCompat?.isInternal(context: Context): Boolean = this?.packageName?.toString() == context.packageName
fun AccessibilityNodeInfoCompat?.isEditText(): Boolean = this?.className?.toString()?.contains("Edit") == true
fun AccessibilityNodeInfoCompat?.isAutoComplete(): Boolean = this?.className?.toString()?.contains("AutoComplete") == true
fun AccessibilityNodeInfoCompat?.isEditOrAutoComplete(): Boolean = isEditText() || isAutoComplete()
fun AccessibilityNodeInfoCompat?.getInputText(): CharSequence? = this?.let {
    val checkTextType = AppContext.get().appConfig.texpanderCheckTypeForTextAvailability(packageName)
    val textRef = text
    return when {
        checkTextType && isEditText() && textRef is String -> null
        !isShowingHintText -> textRef
        else -> null
    }
}

fun AccessibilityNodeInfoCompat?.performSetText(newText: CharSequence?, selectionStart: Int, selectionEnd: Int) = this?.run {
    val args = Bundle()
    args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText ?: "")
    performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

    val selectArgs = Bundle()
    selectArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, selectionStart)
    selectArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, selectionEnd)
    performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectArgs)
}

fun AccessibilityNodeInfoCompat?.performInsertText(insertText: CharSequence): Boolean? = this?.run {
    refresh()

    val args = Bundle()
    val currentText = getInputText() ?: ""
    val currentTextLength = currentText.length
    val newText = StringBuilder()
    var selectionStart = min(currentTextLength, textSelectionStart)
    var selectionEnd = min(currentTextLength, textSelectionEnd)

    // new text
    if (selectionStart > 0) {
        newText.append(currentText.substring(0, selectionStart))
    } else if (selectionEnd < 0) {
        newText.append(currentText)
    }
    newText.append(insertText)
    if (selectionEnd >= 0) {
        newText.append(currentText.substring(selectionEnd))
    }

    // new selection
    val isRangeSelected = currentText.isNotEmpty() && selectionEnd > selectionStart
    val diff = insertText.length - (selectionEnd - selectionStart)
    if (isRangeSelected) {
        selectionEnd += diff
        selectionStart = selectionEnd
    } else {
        selectionStart += diff
        if (currentText.isEmpty()) {
            selectionStart = newText.length + 1
            selectionEnd = selectionStart
        } else {
            selectionEnd += diff
        }
    }

    args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
    val result = performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

    val selectArgs = Bundle()
    selectArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, selectionStart)
    selectArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, selectionEnd)
    performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectArgs)

    return result
}

fun AccessibilityNodeInfoCompat?.performSelectAll() = this?.run {
    refresh()

    val args = Bundle()
    args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
    args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, text?.length ?: 0)
    performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
}

fun AccessibilityNodeInfoCompat?.performFocus() = this?.run {
    refresh()
    performAction(AccessibilityNodeInfo.ACTION_FOCUS)
}

fun AccessibilityNodeInfoCompat?.performClick() = this?.run {
    refresh()
    performAction(AccessibilityNodeInfo.ACTION_CLICK)
}

fun AccessibilityNodeInfoCompat?.performClearSelection() = this?.run {
    refresh()

    val args = Bundle()
    args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, -1)
    args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, -1)
    performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
}

fun AccessibilityNodeInfoCompat?.performCopySelection() = this?.run {
    performAction(AccessibilityNodeInfo.ACTION_COPY)
}

fun AccessibilityNodeInfoCompat?.performPaste() = this?.run {
    performAction(AccessibilityNodeInfo.ACTION_PASTE)
}