package clipto.dao.firebase.mapper

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.core.net.toFile
import clipto.AppUtils
import clipto.common.extensions.*
import clipto.common.misc.IdUtils
import clipto.config.IAppConfig
import clipto.dao.objectbox.model.FileRefBox
import clipto.domain.ClipFile
import clipto.domain.FileRef
import clipto.domain.FileType
import clipto.domain.factory.FileRefFactory
import clipto.store.app.AppState
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileMapper @Inject constructor(
    private val app: Application,
    private val appState: AppState,
    private val appConfig: IAppConfig
) {

    private val folderFormatter = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH)

    fun createPath(folders: List<FileRef>): String {
        return "/${folders.mapNotNull { it.title }.joinToString("/")}"
    }

    fun createNewFolder(): FileRefBox {
        val fileRef = FileRefFactory.newFolder()
        val created = fileRef.createDate ?: Date()
        fileRef.firestoreId = IdUtils.autoId()
        fileRef.folderId = appState.getActiveFolderId()
        fileRef.createDate = created
        fileRef.platform = AppUtils.getPlatform()
        fileRef.asFolder()
        return fileRef
    }

    fun createNewFile(): FileRefBox {
        val fileRef = FileRefFactory.newInstance()
        val created = fileRef.createDate ?: Date()
        fileRef.folderId = appState.getActiveFolderId()
        fileRef.firestoreId = IdUtils.autoId()
        fileRef.createDate = created
        fileRef.platform = AppUtils.getPlatform()
        fileRef.folder = folderFormatter.format(created)
        return fileRef
    }

    fun mapToFileRef(uri: Uri?, fileType: FileType): FileRefBox? {
        if (uri == null) {
            return null
        }
        return runCatching {
            val fileRef = createNewFile()
            fileRef.type = fileType
            fileRef.uploadUrl = uri.toString()
            fileRef.downloadUrl = uri.toString()
            fileRef.downloaded = true

            if (uri.scheme == "file") {
                val file = uri.toFile()
                fileRef.title = file.name
                fileRef.size = file.length()
                fileRef.modifyDate = Date(file.lastModified())
            } else {
                app.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val lastModifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    val mimeTypeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val summaryIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SUMMARY)
                    val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    cursor.moveToFirst()
                    if (lastModifiedIndex != -1) {
                        fileRef.modifyDate = cursor.getLongOrNull(lastModifiedIndex)?.let { Date(it) }
                    }
                    if (mimeTypeIndex != -1) {
                        fileRef.mediaType = cursor.getStringOrNull(mimeTypeIndex)
                    }
                    if (summaryIndex != -1) {
                        fileRef.description = cursor.getStringOrNull(summaryIndex)
                    }
                    if (sizeIndex != -1) {
                        fileRef.size = cursor.getLongOrNull(sizeIndex) ?: 0
                    }
                    if (nameIndex != -1) {
                        fileRef.title = cursor.getStringOrNull(nameIndex)
                    }
                }
            }

            val ext = fileRef.title.getExtension()
            if (!ext.isNullOrBlank()) {
                fileRef.mediaType = fileRef.mediaType ?: fileRef.title.getMimeType(ext)
            }

            fileRef.takeIf { it.isValid() }
        }.getOrNull()
    }

    fun mapMetaToFileRef(meta: ClipFile.Meta): FileRefBox {
        val fileRef = FileRefFactory.newInstance()
        fileRef.platform = meta.platform
        fileRef.mediaType = meta.mediaType
        fileRef.firestoreId = meta.name
        fileRef.createDate = meta.created
        fileRef.updateDate = meta.updated
        fileRef.folder = meta.folder
        fileRef.title = meta.label
        fileRef.size = meta.size
        fileRef.type = meta.type
        fileRef.md5 = meta.md5
        fileRef.uploaded = meta.uploaded
        return fileRef
    }
}