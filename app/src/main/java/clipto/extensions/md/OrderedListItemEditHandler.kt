package clipto.extensions.md

import android.text.Editable
import android.text.Spanned
import androidx.core.text.getSpans
import io.noties.markwon.core.spans.OrderedListItemSpan
import io.noties.markwon.editor.PersistedSpans
import kotlin.math.min

internal class OrderedListItemEditHandler : BaseEditHandler<OrderedListItemSpan>() {

    override fun configurePersistedSpans(builder: PersistedSpans.Builder) = Unit

    override fun handleMarkdownSpan(
            persistedSpans: PersistedSpans,
            editable: Editable,
            input: String,
            span: OrderedListItemSpan,
            spanStart: Int,
            spanTextLength: Int) {
        val start = spanStart
        val end = min(start + spanTextLength, input.length)

        val newSpan = editable.getSpans<OrderedListItemSpan>(start, end).firstOrNull() ?: span

        editable.setSpan(
                newSpan,
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    override fun markdownSpanType(): Class<OrderedListItemSpan> {
        return OrderedListItemSpan::class.java
    }
}