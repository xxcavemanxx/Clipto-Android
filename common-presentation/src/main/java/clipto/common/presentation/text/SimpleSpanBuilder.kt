package clipto.common.presentation.text

import android.graphics.Paint.FontMetricsInt
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.LineHeightSpan
import androidx.core.text.BidiFormatter

class SimpleSpanBuilder(
    private val length: Int = 16,
    private val stringBuilder: StringBuilder = StringBuilder(length),
    private val spanSections: ArrayList<SpanSection> = ArrayList(5)
) {

    fun append(text: CharSequence, vararg spans: Any): SimpleSpanBuilder {
        if (!spans.isNullOrEmpty()) {
            spanSections.add(SpanSection(text, stringBuilder.length, *spans))
        }
        stringBuilder.append(text)
        return this
    }

    fun copy(): SimpleSpanBuilder = SimpleSpanBuilder(
        stringBuilder = StringBuilder(stringBuilder),
        spanSections = ArrayList(spanSections)
    )

    fun build(): SpannableStringBuilder {
        val ssb = SpannableStringBuilder(stringBuilder.toString())
        for (section in spanSections) {
            section.apply(ssb)
        }
        return ssb
    }

    override fun toString(): String {
        return stringBuilder.toString()
    }

    inner class SpanSection constructor(
        private val text: CharSequence,
        private val startIndex: Int,
        private vararg val spans: Any
    ) {

        fun apply(spanStringBuilder: SpannableStringBuilder) {
            for (span in spans) {
                spanStringBuilder.setSpan(span, startIndex, startIndex + text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    class LineOverlapSpan : LineHeightSpan {
        override fun chooseHeight(text: CharSequence, start: Int, end: Int, spanstartv: Int, v: Int, fm: FontMetricsInt) {
            fm.bottom += fm.top
            fm.descent += fm.top
        }
    }

    companion object {
        val FACTORY: Spannable.Factory = object : Spannable.Factory() {
            override fun newSpannable(source: CharSequence): Spannable {
                return if (source is Spannable) source else SpannableString(source)
            }
        }
        val EDITABLE_FACTORY: Editable.Factory = object : Editable.Factory() {
            override fun newEditable(source: CharSequence?): Editable {
                return if (source is Editable) source else SpannableStringBuilder(source)
            }
        }
    }
}

fun CharSequence.toRtlSafe(): CharSequence {
    val bidi = BidiFormatter.getInstance()
    return bidi.unicodeWrap(this)
}