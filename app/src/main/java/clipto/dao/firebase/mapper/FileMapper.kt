package clipto.dao.firebase.mapper

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.core.net.toFile
import clipto.AppUtils
import clipto.analytics.Analytics
import clipto.common.extensions.*
import clipto.common.misc.IdUtils
import clipto.config.IAppConfig
import clipto.dao.firebase.FirebaseDaoHelper
import clipto.dao.objectbox.model.FileRefBox
import clipto.domain.ClipFile
import clipto.domain.FileRef
import clipto.domain.FileType
import clipto.domain.factory.FileRefFactory
import clipto.store.app.AppState
import com.google.firebase.firestore.DocumentChange
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

    fun toMap(from: FileRef): Map<String, Any?> {
        val map = mutableMapOf(
            FirebaseDaoHelper.ATTR_FILE_MD5 to from.md5,
            FirebaseDaoHelper.ATTR_FILE_SIZE to from.size,
            FirebaseDaoHelper.ATTR_FILE_TYPE to from.type.typeId,
            FirebaseDaoHelper.ATTR_FILE_NAME to from.title,
            FirebaseDaoHelper.ATTR_FILE_PATH to from.path,
            FirebaseDaoHelper.ATTR_FILE_FOLDER to from.folder,
            FirebaseDaoHelper.ATTR_FILE_CREATED to from.createDate,
            FirebaseDaoHelper.ATTR_FILE_UPDATED to from.updateDate,
            FirebaseDaoHelper.ATTR_FILE_MODIFIED to from.modifyDate,
            FirebaseDaoHelper.ATTR_FILE_DELETED to from.deleteDate,
            FirebaseDaoHelper.ATTR_FILE_MEDIA_TYPE to from.mediaType,
            FirebaseDaoHelper.ATTR_FILE_UPLOADED to from.uploaded,
            FirebaseDaoHelper.ATTR_FILE_UPLOAD_URL to from.uploadUrl,
            FirebaseDaoHelper.ATTR_FILE_PLATFORM to from.platform,
            FirebaseDaoHelper.ATTR_FILE_ERROR to from.error,
            FirebaseDaoHelper.ATTR_FILE_ABBREVIATION to from.abbreviation,
            FirebaseDaoHelper.ATTR_FILE_DESCRIPTION to from.description,
            FirebaseDaoHelper.ATTR_FILE_TAG_IDS to from.tagIds,
            FirebaseDaoHelper.ATTR_FILE_KIT_IDS to from.snippetSetsIds,
            FirebaseDaoHelper.ATTR_FILE_FOLDER_ID to from.folderId,
            FirebaseDaoHelper.ATTR_FILE_COLOR to from.color,
            FirebaseDaoHelper.ATTR_FILE_FAV to from.fav,
            FirebaseDaoHelper.ATTR_DEVICE_ID to appState.getInstanceId(),
            FirebaseDaoHelper.ATTR_API_VERSION to appConfig.getApiVersion(),
            FirebaseDaoHelper.ATTR_CHANGE_TIMESTAMP to FirebaseDaoHelper.getServerTimestamp(),
        )
        return FirebaseDaoHelper.normalizeMap(map, deleteFromCloud = true)
    }

    fun fromDocChange(change: DocumentChange): FileRefBox? = runCatching {
        FileRefFactory.newInstance()
            .also { fileRef ->
                val from = change.document
                fileRef.firestoreId = from.id
                fileRef.md5 = from.getString(FirebaseDaoHelper.ATTR_FILE_MD5)
                fileRef.size = from.getLong(FirebaseDaoHelper.ATTR_FILE_SIZE) ?: 0L
                fileRef.type = FileType.byId(from.getLong(FirebaseDaoHelper.ATTR_FILE_TYPE)?.toInt())
                fileRef.title = from.getString(FirebaseDaoHelper.ATTR_FILE_NAME)
                fileRef.path = from.getString(FirebaseDaoHelper.ATTR_FILE_PATH)
                fileRef.folder = from.getString(FirebaseDaoHelper.ATTR_FILE_FOLDER)
                fileRef.createDate = from.getDate(FirebaseDaoHelper.ATTR_FILE_CREATED)
                fileRef.updateDate = from.getDate(FirebaseDaoHelper.ATTR_FILE_UPDATED)
                fileRef.modifyDate = from.getDate(FirebaseDaoHelper.ATTR_FILE_MODIFIED)
                fileRef.deleteDate = from.getDate(FirebaseDaoHelper.ATTR_FILE_DELETED)
                fileRef.mediaType = from.getString(FirebaseDaoHelper.ATTR_FILE_MEDIA_TYPE)
                fileRef.uploaded = from.getBoolean(FirebaseDaoHelper.ATTR_FILE_UPLOADED) ?: false
                fileRef.uploadUrl = from.getString(FirebaseDaoHelper.ATTR_FILE_UPLOAD_URL)
                fileRef.platform = from.getString(FirebaseDaoHelper.ATTR_FILE_PLATFORM)
                fileRef.error = from.getString(FirebaseDaoHelper.ATTR_FILE_ERROR)
                fileRef.abbreviation = from.getString(FirebaseDaoHelper.ATTR_FILE_ABBREVIATION)
                fileRef.description = from.getString(FirebaseDaoHelper.ATTR_FILE_DESCRIPTION)
                fileRef.tagIds = from.get(FirebaseDaoHelper.ATTR_FILE_TAG_IDS).castToListOfStrings()
                fileRef.snippetSetsIds = from.get(FirebaseDaoHelper.ATTR_FILE_KIT_IDS).castToListOfStrings()
                fileRef.folderId = from.getString(FirebaseDaoHelper.ATTR_FILE_FOLDER_ID)
                fileRef.color = from.getString(FirebaseDaoHelper.ATTR_FILE_COLOR)
                fileRef.fav = from.getBoolean(FirebaseDaoHelper.ATTR_FILE_FAV) ?: false
            }
    }.onFailure { Analytics.onError("FileMapper", it) }.getOrNull()

    fun fromMap(from: Map<String, Any?>): FileRefBox? = runCatching {
        FileRefFactory.newInstance()
            .also { fileRef ->
                fileRef.firestoreId = from[FirebaseDaoHelper.ATTR_FILE_ID]?.toString()
                fileRef.md5 = from[FirebaseDaoHelper.ATTR_FILE_MD5]?.toString()
                fileRef.size = from[FirebaseDaoHelper.ATTR_FILE_SIZE].castToLong() ?: 0L
                fileRef.type = FileType.byId(from[FirebaseDaoHelper.ATTR_FILE_TYPE].castToInt())
                fileRef.title = from[FirebaseDaoHelper.ATTR_FILE_NAME]?.toString()
                fileRef.path = from[FirebaseDaoHelper.ATTR_FILE_PATH]?.toString()
                fileRef.folder = from[FirebaseDaoHelper.ATTR_FILE_FOLDER]?.toString()
                fileRef.createDate = DateMapper.toDate(from[FirebaseDaoHelper.ATTR_FILE_CREATED] ?: from[FirebaseDaoHelper.ATTR_FILE_CREATE_DATE])
                fileRef.updateDate = DateMapper.toDate(from[FirebaseDaoHelper.ATTR_FILE_UPDATED] ?: from[FirebaseDaoHelper.ATTR_FILE_UPDATE_DATE])
                fileRef.modifyDate = DateMapper.toDate(from[FirebaseDaoHelper.ATTR_FILE_MODIFIED] ?: from[FirebaseDaoHelper.ATTR_FILE_MODIFY_DATE])
                fileRef.deleteDate = DateMapper.toDate(from[FirebaseDaoHelper.ATTR_FILE_DELETED] ?: from[FirebaseDaoHelper.ATTR_FILE_DELETE_DATE])
                fileRef.mediaType = from[FirebaseDaoHelper.ATTR_FILE_MEDIA_TYPE]?.toString()
                fileRef.uploaded = from[FirebaseDaoHelper.ATTR_FILE_UPLOADED].castToBoolean() ?: false
                fileRef.uploadUrl = from[FirebaseDaoHelper.ATTR_FILE_UPLOAD_URL]?.toString()
                fileRef.platform = from[FirebaseDaoHelper.ATTR_FILE_PLATFORM]?.toString()
                fileRef.error = from[FirebaseDaoHelper.ATTR_FILE_ERROR]?.toString()
                fileRef.abbreviation = from[FirebaseDaoHelper.ATTR_FILE_ABBREVIATION]?.toString()
                fileRef.description = from[FirebaseDaoHelper.ATTR_FILE_DESCRIPTION]?.toString()
                fileRef.downloadUrl = from[FirebaseDaoHelper.ATTR_FILE_DOWNLOAD_URL]?.toString()
                fileRef.tagIds = from[FirebaseDaoHelper.ATTR_FILE_TAG_IDS].castToListOfStrings()
                fileRef.snippetSetsIds = from[FirebaseDaoHelper.ATTR_FILE_KIT_IDS].castToListOfStrings()
                fileRef.folderId = from[FirebaseDaoHelper.ATTR_FILE_FOLDER_ID]?.toString()
                fileRef.color = from[FirebaseDaoHelper.ATTR_FILE_COLOR]?.toString()
                fileRef.fav = from[FirebaseDaoHelper.ATTR_FILE_FAV]?.castToBoolean() ?: false
            }
    }.onFailure { Analytics.onError("FileMapper", it) }.getOrNull()

}