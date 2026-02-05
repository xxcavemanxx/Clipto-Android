package clipto.extensions.md

import android.text.Editable
import android.text.Spanned
import io.noties.markwon.core.spans.EmphasisSpan
import io.noties.markwon.editor.MarkwonEditorUtils
import io.noties.markwon.editor.PersistedSpans

internal class EmphasisSpanEditHandler : BaseEditHandler<EmphasisSpan>() {

    override fun configurePersistedSpans(builder: PersistedSpans.Builder) {
        builder.persistSpan(EmphasisSpan::class.java) { EmphasisSpan() }
    }

    override fun handleMarkdownSpan(
            persistedSpans: PersistedSpans,
            editable: Editable,
            input: String,
            span: EmphasisSpan,
            spanStart: Int,
            spanTextLength: Int) {
        val match = MarkwonEditorUtils.findDelimited(input, spanStart, "*", "_")
        if (match != null) {
            val start = match.start()
            val end = match.end()
            editable.setSpan(
                    persistedSpans[EmphasisSpan::class.java],
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    override fun markdownSpanType(): Class<EmphasisSpan> {
        return EmphasisSpan::class.java
    }
}