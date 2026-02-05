package clipto.store.folder

import clipto.common.misc.AndroidUtils
import clipto.domain.FileRef

data class FolderRequest(
    val id: Int = AndroidUtils.nextId(),
    val folderRef: FileRef? = null,
    val withConfigurablePath: Boolean = true,
    val onConsumeFolder: (folder: FileRef) -> Boolean = { false }
)