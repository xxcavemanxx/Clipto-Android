package clipto.dao.objectbox

import clipto.common.extensions.getNotNull
import clipto.common.extensions.threadLocal
import clipto.dao.objectbox.model.LinkPreviewBox
import clipto.dao.objectbox.model.LinkPreviewBox_
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkPreviewBoxDao @Inject constructor() : AbstractBoxDao<LinkPreviewBox>() {

    private val linkPreviewByUrl = threadLocal {
        box.query()
            .equal(LinkPreviewBox_.url, "")
            .build()
    }

    override fun getType(): Class<LinkPreviewBox> = LinkPreviewBox::class.java

    fun get(url: String): LinkPreviewBox? =
        linkPreviewByUrl.getNotNull().setParameter(LinkPreviewBox_.url, url).findFirst()

    fun save(link: LinkPreviewBox) {
        boxStore.callInTx {
            val prev = get(link.url!!)
            if (prev != null) {
                link.localId = prev.localId
            }
            box.put(link)
        }
    }

    fun remove(url: String) {
        linkPreviewByUrl.getNotNull().setParameter(LinkPreviewBox_.url, url).remove()
    }

}