package clipto.presentation.preview.link.extractor

import clipto.presentation.preview.link.LinkPreview
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudFunctionExtractor @Inject constructor() : LinkPreviewExtractor() {

    override fun canExtract(url: String): Boolean = false

    override fun extract(preview: LinkPreview) {
        // No-op since Firebase Functions are disabled
    }

}