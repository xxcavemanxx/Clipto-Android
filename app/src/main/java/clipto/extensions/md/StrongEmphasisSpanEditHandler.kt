package clipto.extensions.md

import android.text.Editable
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import io.noties.markwon.core.spans.StrongEmphasisSpan
import io.noties.markwon.editor.MarkwonEditorUtils
import io.noties.markwon.editor.PersistedSpans

internal class StrongEmphasisSpanEditHandler : BaseEditHandler<StrongEmphasisSpan>() {

    override fun configurePersistedSpans(builder: PersistedSpans.Builder) {
        builder.persistSpan(Bold::class.java) { Bold() }
    }

    override fun handleMarkdownSpan(
            persistedSpans: PersistedSpans,
            editable: Editable,
            input: String,
            span: StrongEmphasisSpan,
            spanStart: Int,
            spanTextLength: Int) {
        val match = MarkwonEditorUtils.findDelimited(input, spanStart, "**", "__")
        if (match != null) {
            val start = match.start()
            val end = match.end()
            editable.setSpan(
                    persistedSpans[Bold::class.java],
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    override fun markdownSpanType(): Class<StrongEmphasisSpan> {
        return StrongEmphasisSpan::class.java
    }

    class Bold : MetricAffectingSpan() {
        override fun updateMeasureState(textPaint: TextPaint) {
            textPaint.isFakeBoldText = true
        }

        override fun updateDrawState(tp: TextPaint?) {
            tp?.isFakeBoldText = true
        }

    }
}