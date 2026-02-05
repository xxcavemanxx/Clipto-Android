package clipto.repository

import android.app.Application
import android.net.Uri
import android.os.Looper
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import clipto.analytics.Analytics
import clipto.api.IApi
import clipto.common.extensions.closeSilently
import clipto.common.extensions.getPersistableUri
import clipto.common.extensions.takePersistableUriPermission
import clipto.common.extensions.toNullIfEmpty
import clipto.common.misc.FormatUtils
import clipto.config.IAppConfig
import clipto.dao.TxHelper
import clipto.dao.firebase.FileFirebaseDao
import clipto.dao.firebase.FirebaseDaoHelper
import clipto.dao.firebase.mapper.FileMapper
import clipto.dao.firebase.model.UserCollection
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
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageTask
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
    private val fileFirebaseDao: FileFirebaseDao,
    private val firebaseDaoHelper: FirebaseDaoHelper
) : IFileRepository {

    private val _uploadDisposableMap = mutableMapOf<Long, StorageTask<*>>()
    private val _downloadDisposableMap = mutableMapOf<Long, StorageTask<*>>()
    private val _downloadReadOnlyDisposableMap = mutableMapOf<Long, WeakReference<InputStream>>()

    override fun terminate(): Completable = Completable.fromCallable { fileFirebaseDao.stopSync() }

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

    override fun init(): Completable = terminate()
        .andThen(Completable.fromPublisher<Boolean> { publisher ->
            log("FIREBASE LISTENER :: init files")
            fileFirebaseDao.startSync(
                activeInitialCallback = { changes, _ -> initialFiles(changes) },
                activeSnapshotCallback = { changes, _ -> snapshotFiles(changes) },
                deletedCallback = { changes, _ -> deletedFiles(changes) },
                firstCallback = {
                    publisher.onNext(true)
                    publisher.onComplete()
                }
            )
        })

    override fun resume(): Completable = Completable.fromCallable {
        firebaseDaoHelper.getAuthUserCollection()?.let { collection ->
            fileBoxDao.getNotUploaded().forEach { fileRef ->
                val uri = fileRef.uploadUrl?.let { app.getPersistableUri(it) }
                if (uri != null) {
                    log("resume upload :: {}", uri)
                    app.takePersistableUriPermission(uri)
                    uploadFile(fileRef, uri, collection)
                } else {
                    saveUploadError(fileRef, collection, app.getString(R.string.file_error_not_found))
                }
            }
            fileBoxDao.getNotDownloaded().forEach { fileRef ->
                val uri = fileRef.downloadUrl?.toUri()
                if (uri != null) {
                    downloadFile(fileRef, uri, collection)
                }
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
                firebaseDaoHelper.getAuthUserCollection()?.let { collection ->
                    fileFirebaseDao.save(fileRefBox, collection)
                }
                publishChange("save", fileRefBox)
                fileRefBox
            }
        }

    override fun getDownloadUrl(fileRef: FileRef): Single<String> = Single.fromPublisher { publisher ->
        firebaseDaoHelper.getAuthUserCollection()?.getUserFileRef(fileRef)?.let { fileRef ->
            fileRef.downloadUrl
                .addOnSuccessListener {
                    runCatching {
                        publisher.onNext(it.toString())
                        publisher.onComplete()
                    }
                }
                .addOnFailureListener {
                    runCatching { publisher.onError(it) }
                }
        }
    }

    override fun getPublicLink(fileRef: FileRef): Single<String> = api.get().getFilePublicLink(fileRef).onErrorReturnItem("").toSingle()

    override fun getPublicLinks(fileRefs: List<FileRef>): Single<List<String>> = fileRefs
        .map { getDownloadUrl(it) }
        .let { Single.zip(it) { urls -> urls.map { url -> url.toString() } } }

    override fun cancelUploadProgress(fileRef: FileRef): Completable = Completable.fromCallable {
        val taskId = fileRef.toBox().id
        _uploadDisposableMap[taskId]?.cancel()
        _uploadDisposableMap.remove(taskId)
        firebaseDaoHelper.getAuthUserCollection()?.let { collection ->
            cancelTasks(collection.getUserFileRef(fileRef).activeUploadTasks)
            saveUploadError(fileRef.toBox(), collection, app.getString(R.string.file_error_canceled))
        }
    }

    override fun cancelUploadProgress(files: List<FileRef>): Completable = Completable.fromCallable {
        files.forEach { fileRef ->
            val taskId = fileRef.toBox().id
            _uploadDisposableMap[taskId]?.cancel()
            _uploadDisposableMap.remove(taskId)
            firebaseDaoHelper.getAuthUserCollection()?.let { collection ->
                cancelTasks(collection.getUserFileRef(fileRef).activeUploadTasks)
                saveUploadError(fileRef.toBox(), collection, app.getString(R.string.file_error_canceled))
            }
        }
    }

    override fun cancelDownloadProgress(fileRef: FileRef) = Completable.fromCallable {
        val taskId = fileRef.toBox().id
        _downloadReadOnlyDisposableMap.remove(taskId)?.get().closeSilently()
        _downloadDisposableMap.remove(taskId)?.cancel()
        firebaseDaoHelper.getAuthUserCollection()?.let { collection ->
            cancelTasks(collection.getUserFileRef(fileRef).activeDownloadTasks)
        }
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
        val collection = firebaseDaoHelper.getAuthUserCollection()
        val fileRef = fileMapper.mapToFileRef(uri, fileType)
        if (collection != null && fileRef != null) {
            fileRef.modifyDate = fileRef.modifyDate ?: Date()
            verify(fileRef)
            uploadFile(fileRef, uri, collection)
        }
        fileRef
    }

    override fun upload(fileRef: FileRef): Single<FileRef> = Single.fromCallable {
        val collection = firebaseDaoHelper.getAuthUserCollection()
        val uri = fileRef.uploadUrl?.toUri()
        log("upload :: {}", uri)
        val fileRefBox = fileRef.toBox()
        if (collection != null && uri != null) {
            fileRefBox.modifyDate = fileRefBox.modifyDate ?: Date()
            fileRefBox.downloadUrl = uri.toString()
            fileRefBox.uploadSessionUrl = null
            fileRefBox.uploaded = false
            verify(fileRefBox)
            uploadFile(fileRefBox, uri, collection)
        }
        fileRefBox
    }

    override fun uploadAll(files: List<FileRef>): Single<List<FileRef>> = Single.fromCallable {
        val collection = firebaseDaoHelper.getAuthUserCollection()
        val uploadedFiles = mutableListOf<FileRef>()
        files.forEach { fileRef ->
            val uri = fileRef.uploadUrl?.toUri()
            log("upload :: {}", uri)
            val fileRefBox = fileRef.toBox()
            if (collection != null && uri != null) {
                fileRefBox.modifyDate = fileRefBox.modifyDate ?: Date()
                fileRefBox.downloadUrl = uri.toString()
                fileRefBox.uploadSessionUrl = null
                fileRefBox.uploaded = false
                verify(fileRefBox)
                uploadFile(fileRefBox, uri, collection)
                uploadedFiles.add(fileRefBox)
            }
        }
        uploadedFiles
    }

    override fun update(fileRef: FileRef, uri: Uri): Single<FileRef> = Single.fromCallable {
        val collection = firebaseDaoHelper.getAuthUserCollection()
        var newFileRef = fileMapper.mapToFileRef(uri, fileRef.type)
        if (collection != null && newFileRef != null) {
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
            uploadFile(newFileRef, uri, collection)
        }
        newFileRef
    }

    override fun download(fileRef: FileRef, uri: Uri): Single<FileRef> = Single.fromPublisher { publisher ->
        val fileRefBox = fileRef.toBox()
        downloadFile(
            fileRefBox,
            uri,
            firebaseDaoHelper.getAuthUserCollection(),
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

                firebaseDaoHelper.getAuthUserCollection()?.let { collection ->
                    val changes = mutableMapOf<String, Any?>(
                        FirebaseDaoHelper.ATTR_FILE_FAV to fav,
                        FirebaseDaoHelper.ATTR_FILE_UPDATED to updated
                    )
                    firebaseDaoHelper.splitBatches(changed) { batchClips ->
                        val batch = collection.createBatch()
                        batchClips.forEach { file -> fileFirebaseDao.saveInBatch(file.toBox(), batch, collection, changes) }
                        batch.commit()
                    }
                }

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

                log("onMoveSelectionToFolder :: {} -> {}, {}", folderId, files.size, changed.size)

                firebaseDaoHelper.getAuthUserCollection()?.let { collection ->
                    val changes = mutableMapOf<String, Any?>(
                        FirebaseDaoHelper.ATTR_FILE_FOLDER_ID to folderId,
                        FirebaseDaoHelper.ATTR_FILE_UPDATED to updated,
                    )
                    firebaseDaoHelper.splitBatches(changed) { batchClips ->
                        val batch = collection.createBatch()
                        batchClips.forEach { file -> fileFirebaseDao.saveInBatch(file.toBox(), batch, collection, changes) }
                        batch.commit()
                    }
                }

            }

            changed
        }

    override fun deleteAll(files: List<FileRef>, permanently: Boolean): Single<List<FileRef>> = Single.fromCallable {
        if (permanently) {
            firebaseDaoHelper.getAuthUserCollection()?.let { collection ->
                firebaseDaoHelper.splitBatches(files) { batchFiles ->
                    val filesToDelete = batchFiles.map { it.toBox() }
                    txHelper.inTx("delete all files") {
                        fileBoxDao.deleteAll(filesToDelete)
                        val batch = collection.createBatch()
                        fileFirebaseDao.deleteAllInBatch(filesToDelete, batch, collection)
                        batch.commit()
                    }
                }
            }
        }
        files
    }

    private fun initialFiles(changes: List<DocumentChange>) {
        val added = changes
            .filter { it.type == DocumentChange.Type.ADDED }
            .filter { !it.document.metadata.hasPendingWrites() }
            .mapNotNull { fileMapper.fromDocChange(it) }
            .toList()
        if (added.isNotEmpty()) {
            txHelper.inTx("Add files after sync") {
                log("add files: {}", added.size)
                fileBoxDao.saveAll(added)
            }
        }
    }

    private fun snapshotFiles(changes: List<DocumentChange>) {
        log("snapshotFiles :: {}", changes.size)
        val changed = changes
            .filter { it.type != DocumentChange.Type.REMOVED }
            .filter { !it.document.metadata.hasPendingWrites() }
            .mapNotNull { fileMapper.fromDocChange(it) }
            .toList()
        if (changed.isNotEmpty()) {
            txHelper.inTx("snapshotFiles") {
                val changedFiles = mutableListOf<FileRefBox>()
                changed.forEach { newFile ->
                    val prevFile = fileBoxDao.getByUid(newFile.getUid())
                    if (prevFile == null || !FileRef.areTheSame(prevFile, newFile)) {
                        if (prevFile != null && !prevFile.isDeleted() && newFile.isDeleted()) {
                            filterBoxDao.update(prevFile, null)
                        } else if (prevFile != null && prevFile.isDeleted() && !newFile.isDeleted()) {
                            filterBoxDao.update(null, newFile)
                        } else {
                            filterBoxDao.update(prevFile, newFile)
                        }
                        if (prevFile != null) {
                            newFile.id = prevFile.id
                        }
                        changedFiles.add(newFile)
                    }
                }
                if (changedFiles.isNotEmpty()) {
                    fileBoxDao.saveAll(changedFiles)
                }
            }
        }
    }

    private fun deletedFiles(changes: List<DocumentChange>) {
        log("deletedFiles :: {}", changes.size)
        val removed = changes
            .filter { it.type != DocumentChange.Type.REMOVED }
            .filter { !it.document.metadata.hasPendingWrites() }
            .mapNotNull { fileMapper.fromDocChange(it) }
            .toList()

        if (removed.isNotEmpty()) {
            log("to be finally removed: {}", removed.size)
            txHelper.inTx("sync with server state") {
                fileBoxDao.deleteAll(removed.mapNotNull { fileBoxDao.getByUid(it.getUid()) })
            }
        }
    }

    private fun downloadFile(
        fileRef: FileRefBox,
        uri: Uri,
        collection: UserCollection?,
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
                            if (prevSize != newSize && collection != null) {
                                fileFirebaseDao.save(fileRefFresh, collection)
                            }
                            log("downloadFile: success {}", fileRefFresh.downloadUrl)
                            publishChange("download success", fileRefFresh)
                            onSuccess(fileRefFresh)
                        }
                    }
                    _downloadReadOnlyDisposableMap.remove(fileId)
                }
            } catch (e: Exception) {
                saveDownloadError(fileRef, collection, e)
                onError.invoke(e)
            }
        } else if (descriptor != null && collection != null) {
            val userFileRef = collection.getUserFileRef(fileRefBox)
            _downloadDisposableMap[fileId] = userFileRef
                .getStream { _, input ->
                    FileOutputStream(descriptor.fileDescriptor).use { output ->
                        IoUtils.copyAllBytes(input, output)
                    }
                }
                .addOnProgressListener {
                    if (!it.task.isCanceled) {
                        safeContext {
                            log("downloadFile: progress {} of {}", it.bytesTransferred, it.totalByteCount)
                            val fileRefFresh = getFreshFile(fileRef)
                            fileRefFresh.progress = ((it.bytesTransferred * 100) / it.totalByteCount).toInt()
                            publishChange("download progress", fileRefFresh)
                        }
                    }
                }
                .addOnFailureListener {
                    if (it is StorageException && it.errorCode == StorageException.ERROR_RETRY_LIMIT_EXCEEDED) {
                        download(getFreshFile(fileRefBox), uri)
                    } else {
                        saveDownloadError(fileRefBox, collection, it)
                        onError.invoke(it)
                    }
                }
                .addOnSuccessListener {
                    if (!it.task.isCanceled) {
                        safeContext {
                            val fileRefFresh = getFreshFile(fileRef)
                            fileRefFresh.error = null
                            fileRefFresh.progress = 0
                            fileRefFresh.downloaded = true
                            val prevUploaded = fileRefFresh.uploaded
                            val prevSize = fileRefFresh.size
                            val newSize = it.totalByteCount
                            fileRefFresh.uploaded = true
                            fileRefFresh.size = newSize
                            fileBoxDao.save(fileRefFresh)
                            if (prevSize != newSize || !prevUploaded) {
                                fileFirebaseDao.save(fileRefFresh, collection)
                            }
                            log("downloadFile: success {}", fileRefFresh.downloadUrl)
                            publishChange("download success", fileRefFresh)
                            onSuccess(fileRefFresh)
                        }
                    }
                }
                .addOnCompleteListener {
                    _downloadDisposableMap.remove(fileId)
                }
        } else if (collection != null) {
            val userFileRef = collection.getUserFileRef(fileRefBox)
            _downloadDisposableMap[fileId] = userFileRef
                .getFile(uri)
                .addOnProgressListener {
                    if (!it.task.isCanceled) {
                        safeContext {
                            log("downloadFile: progress {} of {}", it.bytesTransferred, it.totalByteCount)
                            val fileRefFresh = getFreshFile(fileRef)
                            fileRefFresh.progress = ((it.bytesTransferred * 100) / it.totalByteCount).toInt()
                            publishChange("download progress", fileRefFresh)
                        }
                    }
                }
                .addOnFailureListener {
                    if (it is StorageException && it.errorCode == StorageException.ERROR_RETRY_LIMIT_EXCEEDED) {
                        download(fileRefBox, uri)
                    } else {
                        saveDownloadError(fileRefBox, collection, it)
                        onError.invoke(it)
                    }
                }
                .addOnSuccessListener {
                    if (!it.task.isCanceled) {
                        safeContext {
                            val fileRefFresh = getFreshFile(fileRef)
                            fileRefFresh.error = null
                            fileRefFresh.progress = 0
                            fileRefFresh.downloaded = true
                            val prevUploaded = fileRefFresh.uploaded
                            val prevSize = fileRefFresh.size
                            val newSize = it.totalByteCount
                            fileRefFresh.uploaded = true
                            fileRefFresh.size = newSize
                            fileBoxDao.save(fileRefFresh)
                            if (prevSize != newSize || !prevUploaded) {
                                fileFirebaseDao.save(fileRefFresh, collection)
                            }
                            log("downloadFile: success {}", fileRefFresh.downloadUrl)
                            publishChange("download success", fileRefFresh)
                        }
                    }
                }
                .addOnCompleteListener {
                    _downloadDisposableMap.remove(fileId)
                }
        }
    }

    private fun uploadFile(fileRef: FileRefBox, uri: Uri, collection: UserCollection) {
        val fileRefBox = fileRef.toBox()
        val userFileRef = collection.getUserFileRef(fileRefBox)
        val uploadSessionUrl = fileRefBox.uploadSessionUrl?.toUri()
        val uploadUrl = uri.toString()
        log("upload files :: {} - {}", uri, uploadSessionUrl)
        cancelTasks(userFileRef.activeUploadTasks)
        txHelper.inTx("upload file") {
            fileRefBox.objectType = ObjectType.INTERNAL
            fileRefBox.downloadUrl = uploadUrl
            fileRefBox.uploadUrl = uploadUrl
            fileRefBox.downloaded = true
            fileRefBox.uploaded = false
            fileRefBox.error = null
            fileBoxDao.save(fileRefBox)
            fileFirebaseDao.save(fileRefBox, collection)
            publishChange("upload started", fileRefBox)
        }
        _uploadDisposableMap[fileRef.id] = userFileRef
            .putFile(
                uri,
                StorageMetadata.Builder()
                    .setCustomMetadata(FirebaseDaoHelper.ATTR_FILE_NAME, fileRef.title)
                    .setCustomMetadata(FirebaseDaoHelper.ATTR_FILE_TYPE, fileRef.type.typeName)
                    .setCustomMetadata(FirebaseDaoHelper.ATTR_API_VERSION, appConfig.getApiVersion().toString())
                    .build(),
                uploadSessionUrl
            )
            .addOnProgressListener {
                if (!it.task.isCanceled) {
                    safeContext {
                        val fileRefFresh = getFreshFile(fileRef)
                        val sessionUrl = it.uploadSessionUri?.toString()
                        if (fileRefFresh.uploadSessionUrl != sessionUrl) {
                            fileRefFresh.uploadSessionUrl = sessionUrl
                            fileRefFresh.size = it.totalByteCount
                            fileBoxDao.save(fileRefFresh)
                        }
                        fileRefFresh.progress = ((it.bytesTransferred * 100) / it.totalByteCount).toInt()
                        publishChange("upload progress", fileRefFresh)
                    }
                }
            }
            .addOnFailureListener {
                if (it is StorageException && it.errorCode == StorageException.ERROR_RETRY_LIMIT_EXCEEDED) {
                    uploadFile(getFreshFile(fileRef), uri, collection)
                } else {
                    Analytics.onError("error_attachment", it)
                    saveUploadError(fileRef, collection, it.stackTraceToString())
                }
            }
            .addOnSuccessListener {
                if (!it.task.isCanceled) {
                    safeContext {
                        val metadata = it.metadata
                        txHelper.inTx("uploadAttachment :: success") {
                            val fileRefFresh = getFreshFile(fileRef)
                            fileRefFresh.mediaType = metadata?.contentType ?: fileRefFresh.mediaType
                            fileRefFresh.size = it.totalByteCount
                            fileRefFresh.uploadSessionUrl = null
                            fileRefFresh.md5 = metadata?.md5Hash
                            fileRefFresh.uploaded = true
                            fileRefFresh.error = null
                            fileRefFresh.progress = 0

                            fileBoxDao.save(fileRefFresh)
                            fileFirebaseDao.save(fileRefFresh, collection)
                            publishChange("upload success", fileRefFresh)
                        }
                    }
                }
            }
            .addOnCompleteListener {
                _uploadDisposableMap.remove(fileRef.id)
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

    private fun saveUploadError(fileRef: FileRefBox, collection: UserCollection, error: String) {
        safeContext {
            txHelper.inTx("save file upload error") {
                val fileRefFresh = getFreshFile(fileRef)
                if (!fileRefFresh.isUploaded()) {
                    fileRefFresh.setUploadError(error)
                    fileRefFresh.uploadSessionUrl = null
                    fileBoxDao.save(fileRefFresh)
                    fileFirebaseDao.save(fileRefFresh, collection)
                }
                publishChange("saveUploadError", fileRefFresh)
            }
        }
    }

    private fun saveDownloadError(fileRef: FileRefBox, collection: UserCollection?, error: Exception) {
        log("saveDownloadError :: {}", error)
        safeContext {
            txHelper.inTx("save file download error") {
                val fileRefFresh = getFreshFile(fileRef)
                if (error is StorageException && error.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                    fileRefFresh.setUploadError(error.message ?: error.stackTraceToString())
                    fileBoxDao.save(fileRefFresh)
                    if (collection != null) {
                        fileFirebaseDao.save(fileRefFresh, collection)
                    }
                } else if (!fileRefFresh.isDownloaded()) {
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

    private fun cancelTasks(tasks: List<StorageTask<*>>) {
        val activeTasks = tasks.filter { (it.isPaused || it.isInProgress) && !it.isCanceled }
        if (activeTasks.isNotEmpty()) {
            log("cancelTasks :: found active tasks :: {}", activeTasks.size)
            activeTasks.forEach { runCatching { it.takeIf { !it.isCanceled }?.cancel() } }
        }
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
            if (length > maxLength) {
                val kilobytes = appConfig.attachmentUploadLimitInKilobytes().toString()
                throw ValidationException(app.getString(R.string.main_action_notes_import_error, kilobytes))
            }
        }
    }

}