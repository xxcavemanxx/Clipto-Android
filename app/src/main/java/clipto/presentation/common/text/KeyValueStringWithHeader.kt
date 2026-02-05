package clipto.presentation.common.text

import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.widget.TextView

class KeyValueStringWithHeader(
        textView: TextView,
        keyColor: Int,
        valueColor: Int,
        titleRes: Int,
        descriptionRes: Int = 0,
        withBoldHeader: Boolean = false,
        description: CharSequence = textView.context.getString(descriptionRes),
        title: CharSequence = textView.context.getString(titleRes)
) : KeyValueString(textView, "\n", keyColor, valueColor) {

    init {
        if (!withBoldHeader) {
            setKey(title)
            setValue(description)
            val relativeSpan = RelativeSizeSpan(0.9f)
            setSpan(relativeSpan, valueStart, length, flags)
        } else {
            setKey(title)
            setValue("\n${description}")
            setSpan(TypefaceSpan("sans-serif-medium"), 0, valueStart, flags)
            setSpan(RelativeSizeSpan(0.4f), valueStart, valueStart + 1, flags)
            setSpan(RelativeSizeSpan(0.8f), valueStart + 1, length, flags)
        }
    }

}