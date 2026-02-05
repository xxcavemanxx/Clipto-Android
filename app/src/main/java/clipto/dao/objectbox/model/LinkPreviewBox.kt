package clipto.dao.objectbox.model

import clipto.presentation.preview.link.LinkPreview
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.IndexType
import java.util.*

@Entity
class LinkPreviewBox {

    @Id
    var localId: Long = 0

    @Index(type = IndexType.VALUE)
    var url: String? = null
    var sitename: String? = null
    var title: String? = null
    var description: String? = null
    var imageurl: String? = null
    var mediatype: String? = null
    var createDate: Date? = null
    var embedUrl: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LinkPreviewBox

        if (localId != other.localId) return false

        return true
    }

    override fun hashCode(): Int = localId.hashCode()

}

fun LinkPreviewBox.toPreview(): LinkPreview =
        LinkPreview(
                url = url,
                sitename = sitename,
                title = title,
                description = description,
                imageUrl = imageurl,
                mediatype = mediatype,
                embedUrl = embedUrl
        )

fun LinkPreview.toBox(): LinkPreviewBox {
    val box = LinkPreviewBox()
    box.url = this.url
    box.sitename = this.sitename
    box.title = this.title?.toString()
    box.description = this.description
    box.imageurl = this.imageUrl?.toString()
    box.mediatype = this.mediatype
    box.embedUrl = this.embedUrl
    box.createDate = Date()
    return box
}