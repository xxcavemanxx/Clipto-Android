package clipto.presentation.file.add.data

import java.io.Serializable

data class AddFilesRequest(
    val files: List<FileData>
) : Serializable