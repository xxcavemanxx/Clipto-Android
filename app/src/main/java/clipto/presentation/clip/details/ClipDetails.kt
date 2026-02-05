package clipto.presentation.clip.details

import clipto.domain.Clip
import clipto.domain.PublicLink
import clipto.domain.TextType

data class ClipDetails(
    var type: TextType,
    var fav: Boolean,
    val folderId:String?,
    val tagIds: List<String> = emptyList(),
    val fileIds: List<String> = emptyList(),
    val snippetKitIds: List<String> = emptyList(),
    val publicLink: PublicLink? = null,
    val clip: Clip = Clip.NULL
)