package clipto.extensions.md

import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.editor.EditHandler

internal abstract class BaseEditHandler<T> : EditHandler<T> {

    protected lateinit var theme: MarkwonTheme

    override fun init(markwon: Markwon) {
        theme = markwon.configuration().theme()
    }
}