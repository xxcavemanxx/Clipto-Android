package clipto.extensions.md

import android.text.Editable
import android.text.Spanned
import io.noties.markwon.core.spans.BlockQuoteSpan
import io.noties.markwon.editor.PersistedSpans

internal class BlockQuoteEditHandler : BaseEditHandler<BlockQuoteSpan>() {

    override fun configurePersistedSpans(builder: PersistedSpans.Builder) {
        builder.persistSpan(BlockQuoteSpan::class.java) { BlockQuoteSpan(theme) }
    }

    override fun handleMarkdownSpan(
            persistedSpans: PersistedSpans,
            editable: Editable,
            input: String,
            span: BlockQuoteSpan,
            spanStart: Int,
            spanTextLength: Int) {
        val start = spanStart
        val end = start + spanTextLength
        editable.setSpan(
                persistedSpans[BlockQuoteSpan::class.java],
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    override fun markdownSpanType(): Class<BlockQuoteSpan> {
        return BlockQuoteSpan::class.java
    }

}