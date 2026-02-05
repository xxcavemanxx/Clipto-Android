package clipto.presentation.preview.link

import android.text.SpannableStringBuilder
import android.text.style.URLSpan
import android.text.util.Linkify
import android.widget.TextView
import clipto.common.extensions.withUrls
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.extensions.log
import clipto.presentation.preview.PreviewHelper
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkPreviewFactory @Inject constructor(
    private val previewState: LinkPreviewState,
    private val previewFetcher: LinkPreviewFetcher
) {

    fun preview(
        textView: TextView,
        width: Int,
        height: Int,
        preview: LinkPreview
    ) {
        preview.url?.let { url ->
            textView.maxLines = Integer.MAX_VALUE
            textView.setSpannableFactory(SimpleSpanBuilder.FACTORY)
            val decodedUrl = runCatching { URLDecoder.decode(url, "UTF-8") }.getOrNull() ?: url
            val text = LinkPreviewSpannable(
                withPreviewCache = LinkPreviewCache.of(preview),
                withLinkPreviewFetcher = previewFetcher,
                withLinkPreviewState = previewState,
                withForcePreviews = true,
                withTextView = textView,
                withUrl = decodedUrl,
                withHeight = height,
                withWidth = width
            )
            textView.setText(text, TextView.BufferType.SPANNABLE)
            text.loadPreview()
        }
    }

    fun preview(
        textView: TextView,
        url: CharSequence?,
        width: Int,
        height: Int,
        withForcePreviews: Boolean = false
    ): Boolean {
        return if (url != null && PreviewHelper.isUrl(url)) {
            textView.maxLines = Integer.MAX_VALUE
            textView.setSpannableFactory(SimpleSpanBuilder.FACTORY)
            val decodedUrl = runCatching { URLDecoder.decode(url.toString(), "UTF-8") }.getOrNull()
                ?: url
            val text = LinkPreviewSpannable(
                withPreviewCache = previewState.getLinkPreviewCache(),
                withLinkPreviewFetcher = previewFetcher,
                withLinkPreviewState = previewState,
                withForcePreviews = withForcePreviews,
                withTextView = textView,
                withUrl = decodedUrl,
                withHeight = height,
                withWidth = width
            )
            textView.setText(text, TextView.BufferType.SPANNABLE)
            text.loadPreview()
            true
        } else {
            false
        }
    }

    fun linkify(
        textView: TextView,
        text: CharSequence?,
        width: Int,
        height: Int,
        withForcePreviews: Boolean = false
    ): Boolean {
        if (text == null) {
            textView.text = text
            return false
        }
        val appliedText = SpannableStringBuilder(text)

        var linkified = false

        textView.autoLinkMask = Linkify.PHONE_NUMBERS or Linkify.EMAIL_ADDRESSES
        textView.setSpannableFactory(SimpleSpanBuilder.FACTORY)
        textView.setText(appliedText, TextView.BufferType.SPANNABLE)
        val newText = textView.text
        log("LinkPreviewSpannable :: linkify :: {}", newText)
        if (newText is SpannableStringBuilder) {
            newText.withUrls()
            newText.getSpans(0, newText.length, URLSpan::class.java)
                .asSequence()
                .forEach {
                    val url = it.url
                    if (PreviewHelper.isUrl(url)) {
                        linkified = true
                        LinkPreviewSpannable(
                            withPreviewCache = previewState.getLinkPreviewCache(),
                            withLinkPreviewFetcher = previewFetcher,
                            withLinkPreviewState = previewState,
                            withForcePreviews = withForcePreviews,
                            withTextView = textView,
                            withHeight = height,
                            withWidth = width,
                            withUrl = url
                        )
                    } else {
                        null
                    }?.loadPreview(newText, it)
                }
        }

        return linkified
    }

}