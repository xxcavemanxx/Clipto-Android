package clipto.domain

import clipto.common.extensions.length
import clipto.dao.objectbox.model.ClipBox
import java.io.Serializable
import java.util.*

data class Snippet(
    val id: String,
    val text: String?,
    val userId: String,
    val filterId: String,
    val title: String? = null,
    val created: Date,
    val updated: Date? = null,
    val abbreviation: String? = null,
    val description: String? = null,
    val fileIds: List<String> = emptyList(),
    val textType: TextType = TextType.TEXT_PLAIN
) : Serializable {

    fun getSize(): Long = (text.length() + title.length() + abbreviation.length() + description.length()).toLong()

    fun asClip(kitId: String): Clip {
        val clip = ClipBox()
        clip.snippetId = id
        clip.text = text
        clip.title = title
        clip.fileIds = fileIds
        clip.textType = textType
        clip.updateDate = updated
        clip.createDate = created
        clip.description = description
        clip.abbreviation = abbreviation
        clip.snippetSetsIds = listOf(kitId)
        clip.objectType = ObjectType.EXTERNAL_SNIPPET
        return clip
    }
}
