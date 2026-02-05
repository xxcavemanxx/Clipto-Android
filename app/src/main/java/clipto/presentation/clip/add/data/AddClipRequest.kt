package clipto.presentation.clip.add.data

import java.io.Serializable

data class AddClipRequest(
    val id: Long? = null,
    val text: String? = null,
    val title: String? = null,
    val folderId:String? = null,
    val tracked: Boolean = false,
    val scanBarcode: Boolean = false,
    val screenType: AddClipType? = null
) : Serializable