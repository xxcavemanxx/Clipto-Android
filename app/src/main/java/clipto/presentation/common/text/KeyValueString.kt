package clipto.presentation.common.text

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.extensions.getActionIconColorInactive
import clipto.extensions.getActionIconColorNormal

open class KeyValueString(
        private val textView: TextView,
        private val divider: CharSequence = " ",
        keyColor: Int = textView.context.getActionIconColorNormal(),
        valueColor: Int = textView.context.getActionIconColorInactive()
) : SpannableStringBuilder() {

    val flags = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE

    private val keySpan = ForegroundColorSpan(keyColor)
    private val valueSpan = ForegroundColorSpan(valueColor)

    var keyText: CharSequence = ""
    var valueText: CharSequence = ""

    var valueStart: Int = 0

    init {
        textView.setSpannableFactory(SimpleSpanBuilder.FACTORY)
        textView.setText(this, TextView.BufferType.SPANNABLE)
    }

    fun setKey(keyTextRes: Int): KeyValueString {
        setKey(textView.context.getString(keyTextRes))
        return this
    }

    fun setKey(keyText: CharSequence): KeyValueString {
        if (this.keyText != keyText) {
            replace(0, this.keyText.length, keyText)
            replace(keyText.length, length, divider)
            this.keyText = keyText
            this.valueText = ""
            setSpan(keySpan, 0, keyText.length, flags)
        }
        return this
    }

    fun setValue(valueTextRes: Int): KeyValueString {
        setValue(textView.context.getString(valueTextRes))
        return this
    }

    fun setValue(valueText: CharSequence): KeyValueString {
        if (this.valueText != valueText) {
            this.valueText = valueText
            valueStart = keyText.length + divider.length
            replace(valueStart, length, valueText)
            setSpan(valueSpan, valueStart, length, flags)
        }
        return this
    }

}