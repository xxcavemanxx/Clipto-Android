package clipto.extensions.md

import android.text.Editable
import android.text.Spanned
import android.text.style.StrikethroughSpan
import io.noties.markwon.editor.MarkwonEditorUtils
import io.noties.markwon.editor.PersistedSpans

internal class StrikethroughEditHandler : BaseEditHandler<StrikethroughSpan>() {

    override fun configurePersistedSpans(builder: PersistedSpans.Builder) {
        builder.persistSpan(StrikethroughSpan::class.java) { StrikethroughSpan() }
    }

    override fun handleMarkdownSpan(
            persistedSpans: PersistedSpans,
            editable: Editable,
            input: String,
            span: StrikethroughSpan,
            spanStart: Int,
            spanTextLength: Int) {
        val match = MarkwonEditorUtils.findDelimited(input, spanStart, "~~")
        if (match != null) {
            val start = match.start()
            val end = match.end()
            editable.setSpan(
                    persistedSpans[StrikethroughSpan::class.java],
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    override fun markdownSpanType(): Class<StrikethroughSpan> {
        return StrikethroughSpan::class.java
    }
}