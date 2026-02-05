package clipto.domain.factory

import clipto.common.extensions.notNull
import clipto.dao.objectbox.model.FileRefBox
import clipto.domain.FileRef

object FileRefFactory {

    fun newInstance(): FileRefBox = FileRefBox()
        .also { it.normalize() }

    fun newInstance(template: FileRef) = newInstance().apply(template)

    fun newFolderWithId(id: String?): FileRefBox = newInstance()
        .also { it.firestoreId = id.notNull() }

    fun newFolderWithParentId(id: String?) = newInstance()
        .also { it.folderId = id.notNull() }

    fun newFolder(name: String? = null) = FileRefBox()
        .also { it.asFolder() }
        .also { it.title = name }

    fun create() = FOLDER_NEW
    fun root() = newFolder(ROOT).also { it.firestoreId = "" }

    const val ROOT = " / "
    const val ROOT_PATH = "./"
    const val PATH_SEPARATOR = "/"
    private const val NEW = " + "

    private val FOLDER_NEW = newFolder(NEW)
}