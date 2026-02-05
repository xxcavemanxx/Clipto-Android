package clipto.extensions.md

import android.text.Editable
import android.text.Spanned
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.core.spans.HeadingSpan
import io.noties.markwon.editor.PersistedSpans
import kotlin.math.max
import kotlin.math.min

internal class HeadingSpanEditHandler : BaseEditHandler<HeadingSpan>() {

    override fun configurePersistedSpans(builder: PersistedSpans.Builder) {
        builder.persistSpan(Level1::class.java) { Level1(theme) }
        builder.persistSpan(Level2::class.java) { Level2(theme) }
        builder.persistSpan(Level3::class.java) { Level3(theme) }
        builder.persistSpan(Level4::class.java) { Level4(theme) }
        builder.persistSpan(Level5::class.java) { Level5(theme) }
        builder.persistSpan(Level6::class.java) { Level6(theme) }
    }

    override fun handleMarkdownSpan(
            persistedSpans: PersistedSpans,
            editable: Editable,
            input: String,
            span: HeadingSpan,
            spanStart: Int,
            spanTextLength: Int) {
        val start = max(spanStart, input.indexOf('#', spanStart))
        val lengthUntilNewLine = input.indexOf('\n', start) - start
        val end =
                if (lengthUntilNewLine > 0) {
                    min(start + lengthUntilNewLine, input.length)
                } else {
                    min(start + spanTextLength + span.level + 1, input.length)
                }

        val levelType: Class<*>? =
                when (span.level) {
                    1 -> Level1::class.java
                    2 -> Level2::class.java
                    3 -> Level3::class.java
                    4 -> Level4::class.java
                    5 -> Level5::class.java
                    6 -> Level6::class.java
                    else -> null
                }

        editable.setSpan(
                levelType?.let { persistedSpans[it] } ?: span,
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    override fun markdownSpanType(): Class<HeadingSpan> {
        return HeadingSpan::class.java
    }

    class Level1(theme: MarkwonTheme) : HeadingSpan(theme, 1)
    class Level2(theme: MarkwonTheme) : HeadingSpan(theme, 2)
    class Level3(theme: MarkwonTheme) : HeadingSpan(theme, 3)
    class Level4(theme: MarkwonTheme) : HeadingSpan(theme, 4)
    class Level5(theme: MarkwonTheme) : HeadingSpan(theme, 5)
    class Level6(theme: MarkwonTheme) : HeadingSpan(theme, 6)
}