package clipto.extensions.html

import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.RenderProps
import io.noties.markwon.core.spans.CodeBlockSpan
import io.noties.markwon.html.HtmlTag
import io.noties.markwon.html.tag.SimpleTagHandler

class CodeTagHandler : SimpleTagHandler() {

    override fun getSpans(configuration: MarkwonConfiguration, renderProps: RenderProps, tag: HtmlTag): Any = CodeBlockSpan(configuration.theme())
    override fun supportedTags(): MutableCollection<String> = arrayListOf("code", "CODE")
}