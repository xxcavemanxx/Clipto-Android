package clipto.common.extensions

import android.text.InputType
import android.widget.EditText
import android.widget.TextView

fun EditText.editableSingleLine(editable: Boolean) {
    setTextIsSelectable(true)
    if (editable) {
        setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
    } else {
        setRawInputType(InputType.TYPE_NULL)
    }
}

fun EditText.editableMultiLine(editable: Boolean) {
    setTextIsSelectable(true)
    if (editable) {
        setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
    } else {
        setRawInputType(InputType.TYPE_NULL)
    }
}

fun EditText.setTextWithSelection(text: CharSequence?) {
    setText(text)
    runCatching { setSelection(text?.length ?: 0) }
}

fun TextView.isEditable(): Boolean {
    return this.inputType != InputType.TYPE_NULL
}