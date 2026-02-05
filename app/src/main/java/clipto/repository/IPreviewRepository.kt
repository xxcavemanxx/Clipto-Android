package clipto.repository

import clipto.presentation.preview.link.LinkPreview
import io.reactivex.Single

interface IPreviewRepository {

    fun getPreview(url: String): Single<LinkPreview>

}