package clipto.extensions.html

import android.text.style.RelativeSizeSpan
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.RenderProps
import io.noties.markwon.html.HtmlTag
import io.noties.markwon.html.tag.SimpleTagHandler

class ParagraphTagHandler : SimpleTagHandler() {

    override fun getSpans(configuration: MarkwonConfiguration, renderProps: RenderProps, tag: HtmlTag): Any = RelativeSizeSpan(1f)
    override fun supportedTags(): MutableCollection<String> = arrayListOf("p", "P")
}