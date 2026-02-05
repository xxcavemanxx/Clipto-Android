package clipto.domain

import android.text.style.ClickableSpan
import android.widget.EditText
import androidx.core.text.getSpans
import clipto.AppContext
import clipto.common.extensions.showKeyboard
import kotlin.math.max
import kotlin.math.min

data class AttributedObjectScreenState<O : AttributedObject>(
    val value: O,
    val viewMode: ViewMode,
    val focusMode: FocusMode,
    val title: CharSequence? = null,
    val iconRes: Int? = null,
    val iconColor: Int? = null
) {
    fun isPreviewMode(): Boolean = viewMode == ViewMode.PREVIEW
    fun isEditMode(): Boolean = viewMode == ViewMode.EDIT
    fun isViewMode(): Boolean = viewMode == ViewMode.VIEW
}

fun <O : AttributedObject> AttributedObjectScreenState<O>?.isEditable(): Boolean = !this.isReadOnly() && this?.value?.isDeleted() != true

fun <O : AttributedObject> AttributedObjectScreenState<O>?.isPreviewMode(): Boolean = this != null && this.isPreviewMode()

fun <O : AttributedObject> AttributedObjectScreenState<O>?.isReadOnly(): Boolean = this != null && this.value.isReadOnly()

fun <O : AttributedObject> AttributedObjectScreenState<O>?.isEditMode(): Boolean = this != null && this.isEditMode()

fun <O : AttributedObject> AttributedObjectScreenState<O>?.isViewMode(): Boolean = this != null && this.isViewMode()

fun <O : AttributedObject> AttributedObjectScreenState<O>?.acceptDoubleClick(): Boolean {
    val thisRef = this ?: return false
    val settings = AppContext.get().getSettings()
    return settings.doubleClickToEdit
            && !thisRef.value.isReadOnly()
            && !thisRef.isEditMode()
}

fun <O : AttributedObject> AttributedObjectScreenState<O>.whenCanBeEdited(editText: EditText? = null): Boolean? {
    if (isEditable()) {
        try {
            if (editText != null && isViewMode()) {
                var selectionStart = editText.selectionStart
                var selectionEnd = editText.selectionEnd
                if (selectionStart > 0 && selectionEnd == selectionStart) {
                    val text = editText.text
                    val radius = AppContext.get().appConfig.noteLinkClickRadius()
                    selectionEnd = min(text.length, selectionEnd + radius)
                    selectionStart = max(0, selectionStart - radius)
                    val clickableSpans = text.getSpans<ClickableSpan>(selectionStart, selectionEnd)
                    if (clickableSpans.isNotEmpty()) {
                        clickableSpans.first().onClick(editText)
                        return null
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        if (isViewMode()) {
            return true
        } else {
            editText?.showKeyboard()
        }
    }
    return null
}