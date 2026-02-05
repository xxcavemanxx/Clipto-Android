package clipto.dao.sharedprefs.data

import clipto.presentation.clip.add.data.AddClipType
import com.google.gson.annotations.SerializedName

data class AddClipScreenData(
    @SerializedName("hideEdit") val hideEdit: Boolean = false,
    @SerializedName("hideActions") val hideActions: Boolean = false,
    @SerializedName("hideInsert") val hideInsert: Boolean = false,
    @SerializedName("screenType") val screenType: AddClipType = AddClipType.EDIT,
    @SerializedName("folderId") val folderId: String? = null
)