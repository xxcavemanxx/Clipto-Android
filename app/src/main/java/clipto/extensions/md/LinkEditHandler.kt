package clipto.extensions.md

import android.text.Editable
import android.text.Spanned
import androidx.core.text.getSpans
import io.noties.markwon.core.spans.LinkSpan
import io.noties.markwon.editor.PersistedSpans

internal class LinkEditHandler : BaseEditHandler<LinkSpan>() {

    override fun configurePersistedSpans(builder: PersistedSpans.Builder) = Unit

    override fun handleMarkdownSpan(
            persistedSpans: PersistedSpans,
            editable: Editable,
            input: String,
            span: LinkSpan,
            spanStart: Int,
            spanTextLength: Int) {
        val start = input.indexOf('[', spanStart) + 1
        var end = input.indexOf(']', start)
        if (end <= start) {
            end = start + spanTextLength
        }
        val newSpan = editable.getSpans<LinkSpan>(start, end).firstOrNull() ?: span
//        editable.getSpans<LinkSpan>(start, end).filter { it != newSpan }.forEach { editable.removeSpan(it) }

        editable.setSpan(
                newSpan,
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    override fun markdownSpanType(): Class<LinkSpan> {
        return LinkSpan::class.java
    }

}