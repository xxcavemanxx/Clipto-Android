package clipto.presentation.preview.link

interface ILinkPreviewFetcher {

    fun fetchPreview(spannable: LinkPreviewSpannable, callback: (preview: LinkPreview) -> Unit)

    fun clearPreview(spannable: LinkPreviewSpannable, url: String)

}