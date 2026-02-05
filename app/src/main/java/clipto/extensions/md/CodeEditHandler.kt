package clipto.extensions.md

import android.text.Editable
import android.text.Spanned
import io.noties.markwon.core.spans.CodeSpan
import io.noties.markwon.editor.PersistedSpans
import kotlin.math.min

internal class CodeEditHandler : BaseEditHandler<CodeSpan>() {

    override fun configurePersistedSpans(builder: PersistedSpans.Builder) {
        builder.persistSpan(CodeSpan::class.java) { CodeSpan(theme) }
    }

    override fun handleMarkdownSpan(
            persistedSpans: PersistedSpans,
            editable: Editable,
            input: String,
            span: CodeSpan,
            spanStart: Int,
            spanTextLength: Int) {
        val start = spanStart
        var end = min(start + spanTextLength, input.length)
        for (i in end until input.length) {
            if (input[i] != '`') {
                end = i
                break
            }
        }
        editable.setSpan(
                persistedSpans[CodeSpan::class.java],
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    override fun markdownSpanType(): Class<CodeSpan> {
        return CodeSpan::class.java
    }
}