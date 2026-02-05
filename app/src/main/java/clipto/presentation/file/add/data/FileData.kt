package clipto.presentation.file.add.data

import android.net.Uri
import clipto.domain.FileType

data class FileData(
    val uri: Uri,
    val fileType: FileType
)