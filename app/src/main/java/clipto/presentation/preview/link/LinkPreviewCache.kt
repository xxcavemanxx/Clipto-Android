package clipto.presentation.preview.link

interface LinkPreviewCache {

    fun get(url: String): LinkPreview?
    fun put(metadata: LinkPreview)
    fun remove(url: String)

    companion object {
        fun of(preview: LinkPreview) = NotCacheablePreview(preview)
    }

    class NotCacheablePreview(val preview: LinkPreview) : LinkPreviewCache {
        override fun get(url: String): LinkPreview = preview
        override fun put(metadata: LinkPreview) = Unit
        override fun remove(url: String) = Unit
    }
}