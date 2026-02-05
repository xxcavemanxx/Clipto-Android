package clipto.dao.sharedprefs.data

import clipto.presentation.file.add.data.AddFileType
import com.google.gson.annotations.SerializedName

data class AddFileScreenData(
    @SerializedName("hideFile") val hideFile: Boolean = false,
    @SerializedName("hideNote") val hideNote: Boolean = false,
    @SerializedName("hideAttachment") val hideAttachment: Boolean = false,
    @SerializedName("screenType") val screenType: AddFileType = AddFileType.FILE,
    @SerializedName("folderId") val folderId: String? = null
)