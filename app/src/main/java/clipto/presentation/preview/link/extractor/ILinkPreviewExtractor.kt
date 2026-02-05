package clipto.presentation.preview.link.extractor

import clipto.presentation.preview.link.LinkPreview

interface ILinkPreviewExtractor {

    fun canExtract(url: String): Boolean

    fun extract(preview: LinkPreview)

}