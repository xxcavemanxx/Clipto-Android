package clipto.presentation.preview.link.extractor

import clipto.presentation.preview.link.LinkPreview
import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudFunctionExtractor @Inject constructor() : LinkPreviewExtractor() {

    override fun canExtract(url: String): Boolean = true

    override fun extract(preview: LinkPreview) {
        val params = mapOf<String, Any?>("url" to preview.url)
        val func = FirebaseFunctions.getInstance().getHttpsCallable("getLinkPreview")
        val resultData = Tasks.await(func.call(params)).data
        if (resultData is Map<*, *>) {
            preview.description = resultData["description"]?.toString()
            preview.mediatype = resultData["mediaType"]?.toString()
            preview.imageUrl = resultData["imageUrl"]?.toString()
            preview.title = resultData["title"]?.toString()
        }
    }

}