package clipto.repository

import android.app.Application
import android.net.Uri
import android.os.Looper
import android.os.ParcelFileDescriptor
import clipto.analytics.Analytics
import clipto.api.IApi
import clipto.common.extensions.closeSilently
import clipto.common.extensions.getPersistableUri
import clipto.common.extensions.takePersistableUriPermission
import androidx.core.net.toUri
import clipto.common.misc.FormatUtils
import clipto.config.IAppConfig
import clipto.dao.TxHelper
import clipto.dao.drive.DriveServiceHelper
import clipto.dao.firebase.mapper.FileMapper
import clipto.dao.objectbox.FileBoxDao
import clipto.dao.objectbox.FilterBoxDao
import clipto.dao.objectbox.model.FileRefBox
import clipto.dao.objectbox.model.toBox
import clipto.domain.FileRef
import clipto.domain.FileType
import clipto.domain.Filter
import clipto.domain.ObjectType
import clipto.domain.factory.FileRefFactory
import clipto.exception.ValidationException
import clipto.extensions.log
import clipto.store.files.FilesState
import com.google.api.client.http.InputStreamContent
import com.wb.clipboard.R
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Single
import org.greenrobot.essentials.io.IoUtils
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.ref.WeakReference
import java.net.URL
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(
    private val app: Application,
    private val api: Lazy<IApi>,
    private val txHelper: TxHelper,
    private val appConfig: IAppConfig,
    private val fileMapper: FileMapper,
    private val filesState: FilesState,
    private val fileBoxDao: FileBoxDao,
    private val filterBoxDao: FilterBoxDao,
    private val driveHelper: DriveServiceHelper
) : IFileRepository {

    private val _downloadReadOnlyDisposableMap = mutableMapOf<Long, WeakReference<InputStream>>()

    override fun terminate(): Completable = Completable.complete()

    override fun getRelativePath(folderId: String?, fileRef: FileRef): Single<String> = Single
        .fromCallable {
            val path = fileBoxDao.getPath(fileRef)
            val folderIdRef = folderId.toNullIfEmpty()
            val indexOf = path.indexOfFirst { it.getUid() == folderIdRef }.takeIf { it >= 0 }?.let { it + 1 } ?: 0
            val relativePath = path.subList(indexOf, path.size).mapNotNull { it.title }
            relativePath.joinToString(
                prefix = FileRefFactory.ROOT_PATH,
                separator = FileRefFactory.PATH_SEPARATOR
            )
        }

    override fun getFilePath(fileRef: FileRef): Single<List<FileRef>> = Single
        .fromCallable { fileBoxDao.getPath(fileRef) }

    override fun getFiltered(filter: Filter.Snapshot): Single<List<FileRef>> = Single
        .fromCallable { fileBoxDao.getFiltered(filter).find() }

    override fun getParent(uid: String?): Single<FileRef> = Single
        .fromCallable {
            val file = fileBoxDao.getByUid(uid)
            if (file != null) {
                fileBoxDao.getByUid(file.folderId)
            } else {
                null
            }
        }

    override fun getByUid(uid: String?): Single<FileRef> = Single
        .fromCallable { fileBoxDao.getByUid(uid)?.normalize() }

    override fun getFile(fileRef: FileRef): Single<FileRef> = Single
        .fromCallable {
            val id = fileRef.toBox().id
            val uid = fileRef.getUid()
            val newFileRef = when {
                id != 0L -> fileBoxDao.getById(id)
                uid != null -> fileBoxDao.getByUid(uid) ?: fileRef
                else -> fileRef
            }
            if (fileRef.isReadOnly()) {
                newFileRef.uploadUrl = fileRef.uploadUrl
                newFileRef.mediaType = fileRef.mediaType
                newFileRef.updateDate = fileRef.updateDate
                newFileRef.createDate = fileRef.createDate
                newFileRef.title = fileRef.title
                newFileRef.size = fileRef.size
            }
            newFileRef
        }

    override fun init(): Completable = Completable.complete()

    override fun resume(): Completable = Completable.fromCallable {
        fileBoxDao.getNotUploaded().forEach { fileRef ->
            val uri = fileRef.uploadUrl?.let { app.getPersistableUri(it) }
            if (uri != null) {
                log("resume upload :: {}", uri)
                app.takePersistableUriPermission(uri)
                uploadFile(fileRef, uri)
            } else {
                saveUploadError(fileRef, app.getString(R.string.file_error_not_found))
            }
        }
        fileBoxDao.getNotDownloaded().forEach { fileRef ->
            val uri = fileRef.downloadUrl?.toUri()
            if (uri != null) {
                downloadFile(fileRef, uri)
            }
        }
    }

    override fun getFiles(fileIds: List<String>): Single<List<FileRef>> = Single.fromCallable {
        fileBoxDao.getFiles(fileIds)
    }

    override fun save(fileRef: FileRef): Single<FileRef> = Single
        .fromCallable {
            verify(fileRef)
            txHelper.inTx("save file") {
                val fileRefBox = fileRef.toBox()
                fileRefBox.updateDate = Date()
                fileBoxDao.save(fileRefBox)
                publishChange("save", fileRefBox)
                fileRefBox
            }
        }

    override fun getDownloadUrl(fileRef: FileRef): Single<String> = Single.fromCallable {
        fileRef.downloadUrl ?: ""
     }

     override fun cancelUploadProgress(fileRef: FileRef): Completable = Completable.fromCallable {
        val taskId = fileRef.toBox().id
        saveUploadError(fileRef.toBox(), app.getString(R.string.file_error_canceled))
    }

    override fun cancelUploadProgress(files: List<FileRef>): Completable = Completable.fromCallable {
        files.forEach { fileRef ->
            saveUploadError(fileRef.toBox(), app.getString(R.string.file_error_canceled))
        }
    }

    override fun cancelDownloadProgress(fileRef: FileRef) = Completable.fromCallable {
        val taskId = fileRef.toBox().id
        _downloadReadOnlyDisposableMap.remove(taskId)?.get().closeSilently()
        txHelper.inTx("cancel file progress") {
            val fileRefFresh = getFreshFile(fileRef)
            fileRefFresh.downloaded = false
            fileRefFresh.downloadUrl = null
            fileRefFresh.progress = 0
            fileBoxDao.save(fileRefFresh)
            publishChange("cancelDownloadProgress", fileRefFresh)
        }
    }

    override fun upload(uri: Uri, fileType: FileType): Single<FileRef> = Single.fromCallable {
        val fileRef = fileMapper.mapToFileRef(uri, fileType)
        if (fileRef != null) {
            fileRef.modifyDate = fileRef.modifyDate ?: Date()
            verify(fileRef)
            uploadFile(fileRef, uri)
        }
        fileRef
    }

    override fun upload(fileRef: FileRef): Single<FileRef> = Single.fromCallable {
        val uri = fileRef.uploadUrl?.toUri()
        log("upload :: {}", uri)
        val fileRefBox = fileRef.toBox()
        if (uri != null) {
            fileRefBox.modifyDate = fileRefBox.modifyDate ?: Date()
            fileRefBox.downloadUrl = uri.toString()
            fileRefBox.uploadSessionUrl = null
            fileRefBox.uploaded = false
            verify(fileRefBox)
            uploadFile(fileRefBox, uri)
        }
        fileRefBox
    }

    override fun uploadAll(files: List<FileRef>): Single<List<FileRef>> = Single.fromCallable {
        val uploadedFiles = mutableListOf<FileRef>()
        files.forEach { fileRef ->
            val uri = fileRef.uploadUrl?.toUri()
            log("upload :: {}", uri)
            val fileRefBox = fileRef.toBox()
            if (uri != null) {
                fileRefBox.modifyDate = fileRefBox.modifyDate ?: Date()
                fileRefBox.downloadUrl = uri.toString()
                fileRefBox.uploadSessionUrl = null
                fileRefBox.uploaded = false
                verify(fileRefBox)
                uploadFile(fileRefBox, uri)
                uploadedFiles.add(fileRefBox)
            }
        }
        uploadedFiles
    }

    override fun update(fileRef: FileRef, uri: Uri): Single<FileRef> = Single.fromCallable {
        var newFileRef = fileMapper.mapToFileRef(uri, fileRef.type)
        if (newFileRef != null) {
            val newFileTitle = newFileRef.title
            newFileRef = fileRef.with(newFileRef).toBox()
            newFileRef.modifyDate = newFileRef.modifyDate ?: Date()
            if (newFileRef.title == FormatUtils.UNKNOWN) {
                newFileRef.title = newFileTitle
            }
            newFileRef.downloadUrl = uri.toString()
            newFileRef.uploadSessionUrl = null
            newFileRef.uploaded = false
            verify(newFileRef)
            uploadFile(newFileRef, uri)
        }
        newFileRef
    }

    override fun download(fileRef: FileRef, uri: Uri): Single<FileRef> = Single.fromPublisher { publisher ->
        val fileRefBox = fileRef.toBox()
        downloadFile(
            fileRefBox,
            uri,
            onSuccess = {
                publisher.onNext(it)
                publisher.onComplete()
            },
            onError = {
                publisher.onError(it)
            }
        )
    }

    override fun favAll(files: List<FileRef>, fav: Boolean): Single<List<FileRef>> = Single
        .fromCallable {
            val changed = mutableListOf<FileRefBox>()

            txHelper.inTx("change fav") {
                files.forEach {
                    val newFile = it.toBox()
                    val prevFile = fileBoxDao.getById(newFile.id)
                    if (prevFile.fav != fav) {
                        prevFile.fav = fav
                        changed.add(prevFile)
                    }
                }
                val updated = Date()
                fileBoxDao.saveAll(changed, updated)
            }

            changed
        }

    override fun getChildren(folderId: String?, deep: Boolean, fileTypes: List<FileType>): Single<List<FileRef>> = Single
        .fromCallable {
            when {
                folderId == null -> fileBoxDao.getByFileType(fileTypes)
                deep -> fileBoxDao.getChildrenDeep(folderId, fileTypes)
                else -> fileBoxDao.getChildren(folderId, fileTypes)
            }
        }

    override fun changeFolder(files: List<FileRef>, folderId: String?): Single<List<FileRef>> = Single
        .fromCallable {
            val changed = mutableListOf<FileRefBox>()

            txHelper.inTx("change folder") {
                files.forEach {
                    val newFile = it.toBox()
                    val prevFile = fileBoxDao.getById(newFile.id)
                    if (prevFile.folderId != folderId) {
                        prevFile.folderId = folderId
                        changed.add(prevFile)
                    }
                }
                val updated = Date()
                fileBoxDao.saveAll(changed, updated)
            }

            changed
        }

    override fun deleteAll(files: List<FileRef>, permanently: Boolean): Single<List<FileRef>> = Single.fromCallable {
        if (permanently) {
            val filesToDelete = files.map { it.toBox() }
            txHelper.inTx("delete all files") {
                fileBoxDao.deleteAll(filesToDelete)
                filesToDelete.forEach { file ->
                    val uid = file.getUid()
                    if (uid != null) {
                        filesState.onBackground {
                            try {
                                driveHelper.deleteFileByName("file_$uid")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
        files
    }

    private fun downloadFile(
        fileRef: FileRefBox,
        uri: Uri,
        onSuccess: (fileRef: FileRef) -> Unit = {},
        onError: (error: Exception) -> Unit = {}
    ) {
        val fileRefBox = fileRef.toBox()
        cancelDownloadProgress(fileRefBox)
        val descriptor = getDescriptor(uri)
        val fileId = fileRefBox.id
        fileRefBox.downloadUrl = uri.toString()
        fileRefBox.downloaded = false
        fileRefBox.error = null
        fileBoxDao.save(fileRefBox)
        publishChange("download started", fileRefBox)

        log("downloadfiles :: isReadOnly={}, id={}, descriptor={}", fileRefBox.isReadOnly(), fileRefBox.getUid(), descriptor)

        if (fileRefBox.isReadOnly()) {
            try {
                if (descriptor == null) {
                    throw IllegalArgumentException("unsupported file protocol")
                }
                val uploadUrl = fileRefBox.uploadUrl
                val url = URL(uploadUrl)
                val urlConnection = url.openConnection()
                val totalByteCount = urlConnection.contentLength
                urlConnection.getInputStream().use { input ->
                    _downloadReadOnlyDisposableMap[fileId] = WeakReference(input)
                    FileOutputStream(descriptor.fileDescriptor).use { output ->
                        val buffer = ByteArray(1024 * 4)
                        var bytesRead = input.read(buffer)
                        var bytesTransferred = 0L
                        while (bytesRead >= 0) {
                            output.write(buffer, 0, bytesRead)
                            bytesTransferred += bytesRead
                            safeContext {
                                log("downloadFile: progress {} of {}", bytesTransferred, totalByteCount)
                                val fileRefFresh = getFreshFile(fileRef)
                                fileRefFresh.progress = ((bytesTransferred * 100) / totalByteCount).toInt()
                                publishChange("download progress", fileRefFresh)
                            }
                            bytesRead = input.read(buffer)
                        }
                        safeContext {
                            val fileRefFresh = getFreshFile(fileRef)
                            fileRefFresh.error = null
                            fileRefFresh.progress = 0
                            fileRefFresh.downloaded = true
                            val prevSize = fileRefFresh.size
                            val newSize = maxOf(prevSize, totalByteCount.toLong())
                            fileRefFresh.size = newSize
                            fileBoxDao.save(fileRefFresh)
                            log("downloadFile: success {}", fileRefFresh.downloadUrl)
                            publishChange("download success", fileRefFresh)
                            onSuccess(fileRefFresh)
                        }
                    }
                    _downloadReadOnlyDisposableMap.remove(fileId)
                }
            } catch (e: Exception) {
                saveDownloadError(fileRef, e)
                onError.invoke(e)
            }
        } else if (descriptor != null) {
            safeContext {
                try {
                    val uid = fileRefBox.getUid()!!
                    val remoteFileId = driveHelper.findFileByName("file_$uid")
                        ?: throw java.io.FileNotFoundException("File not found on Google Drive")

                    driveHelper.downloadStream(remoteFileId).use { input ->
                        FileOutputStream(descriptor.fileDescriptor).use { output ->
                            IoUtils.copyAllBytes(input, output)
                        }
                    }

                    val fileRefFresh = getFreshFile(fileRef)
                    fileRefFresh.error = null
                    fileRefFresh.progress = 0
                    fileRefFresh.downloaded = true
                    fileRefFresh.uploaded = true
                    fileBoxDao.save(fileRefFresh)
                    log("downloadFile: success {}", fileRefFresh.downloadUrl)
                    publishChange("download success", fileRefFresh)
                    onSuccess(fileRefFresh)
                } catch (e: Exception) {
                    saveDownloadError(fileRefBox, e)
                    onError.invoke(e)
                }
            }
        }
    }

    private fun uploadFile(fileRef: FileRefBox, uri: Uri) {
        val fileRefBox = fileRef.toBox()
        val uploadUrl = uri.toString()
        txHelper.inTx("upload file") {
            fileRefBox.objectType = ObjectType.INTERNAL
            fileRefBox.downloadUrl = uploadUrl
            fileRefBox.uploadUrl = uploadUrl
            fileRefBox.downloaded = true
            fileRefBox.uploaded = false
            fileRefBox.error = null
            fileBoxDao.save(fileRefBox)
            publishChange("upload started", fileRefBox)
        }

        filesState.onBackground {
            try {
                val uid = fileRefBox.getUid()!!
                val inputStream = app.contentResolver.openInputStream(uri)
                    ?: throw java.io.FileNotFoundException("Unable to open input stream for Uri: $uri")

                val size = fileRefBox.size
                val mediaContent = InputStreamContent(fileRefBox.mediaType ?: "application/octet-stream", inputStream)
                    .setLength(size)

                driveHelper.uploadFileStream("file_$uid", mediaContent)

                safeContext {
                    txHelper.inTx("uploadAttachment :: success") {
                        val fileRefFresh = getFreshFile(fileRef)
                        fileRefFresh.uploaded = true
                        fileRefFresh.error = null
                        fileRefFresh.progress = 0
                        fileBoxDao.save(fileRefFresh)
                        publishChange("upload success", fileRefFresh)
                    }
                }
            } catch (e: Exception) {
                Analytics.onError("error_attachment", e)
                saveUploadError(fileRef, e.stackTraceToString())
            }
        }
    }

    private fun publishChange(who: String, fileRef: FileRef) {
        log(
            "publishChange :: {} :: progress = {}, file = {}, uploaded = {}, id = {}",
            who,
            fileRef.progress,
            fileRef.title,
            fileRef.uploaded,
            fileRef.firestoreId
        )
        filesState.changes.setValue(fileRef, force = true)
    }

    private fun saveUploadError(fileRef: FileRefBox, error: String) {
        safeContext {
            txHelper.inTx("save file upload error") {
                val fileRefFresh = getFreshFile(fileRef)
                if (!fileRefFresh.isUploaded()) {
                    fileRefFresh.setUploadError(error)
                    fileRefFresh.uploadSessionUrl = null
                    fileBoxDao.save(fileRefFresh)
                }
                publishChange("saveUploadError", fileRefFresh)
            }
        }
    }

    private fun saveDownloadError(fileRef: FileRefBox, error: Exception) {
        log("saveDownloadError :: {}", error)
        safeContext {
            txHelper.inTx("save file download error") {
                val fileRefFresh = getFreshFile(fileRef)
                if (!fileRefFresh.isDownloaded()) {
                    fileRefFresh.setDownloadError(error.stackTraceToString())
                    fileBoxDao.save(fileRefFresh)
                }
                publishChange("saveDownloadError", fileRefFresh)
            }
        }
    }

    private fun safeContext(block: () -> Unit) {
        if (Looper.getMainLooper().thread == Thread.currentThread()) {
            filesState.onBackground {
                try {
                    block.invoke()
                } catch (th: Throwable) {
                    Analytics.onError("error_attachment_action", th)
                }
            }
        } else {
            try {
                block.invoke()
            } catch (th: Throwable) {
                Analytics.onError("error_attachment_action", th)
            }
        }
    }

    private fun getDescriptor(uri: Uri): ParcelFileDescriptor? {
        return runCatching { app.contentResolver.openFileDescriptor(uri, "w") }.getOrNull()
    }

    private fun getFreshFile(fileRef: FileRef): FileRefBox {
        return fileBoxDao.getById(fileRef.toBox().id)
    }

    private fun verify(fileRef: FileRef) {
        log("files :: verify :: {}", fileRef)
        if (fileRef.isFolder) {
            if (fileRef.title.isNullOrBlank()) {
                throw ValidationException(app.getString(R.string.folder_error_name_required))
            }
            val same = fileBoxDao.getByFolderAndName(fileRef.folderId, fileRef.title)
            if (same != null && same != fileRef) {
                throw ValidationException(app.getString(R.string.folder_error_name_required))
            }
        } else if (fileRef.title.isNullOrBlank()) {
            throw ValidationException(app.getString(R.string.file_error_name_required))
        } else {
            val length = fileRef.size
            val maxLength = appConfig.attachmentUploadLimitInBytes()
             if (length > maxLength.toLong()) {
                val kilobytes = appConfig.attachmentUploadLimitInKilobytes().toString()
                throw ValidationException(app.getString(R.string.main_action_notes_import_error, kilobytes))
            }
        }
    }

    private fun String?.toNullIfEmpty(): String? {
        return if (this.isNullOrBlank()) null else this
    }

    // Helper for downloading streams from driveHelper
    private fun DriveServiceHelper.downloadStream(fileId: String): InputStream {
        return this.downloadFileStream(fileId)
    }
}