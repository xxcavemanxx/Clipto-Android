package clipto.repository

import android.net.Uri
import clipto.domain.Clip
import clipto.domain.FileRef
import clipto.domain.FileType
import clipto.domain.Filter
import io.reactivex.Completable
import io.reactivex.Single

interface IFileRepository {

    fun init(): Completable
    fun resume(): Completable
    fun terminate(): Completable
    fun getByUid(uid: String?): Single<FileRef>
    fun getFile(fileRef: FileRef): Single<FileRef>
    fun save(fileRef: FileRef): Single<FileRef>
    fun getFiles(fileIds: List<String>): Single<List<FileRef>>
    fun getDownloadUrl(fileRef: FileRef): Single<String>
    fun getPublicLink(fileRef: FileRef): Single<String>
    fun getPublicLinks(fileRefs: List<FileRef>): Single<List<String>>
    fun download(fileRef: FileRef, uri: Uri): Single<FileRef>
    fun upload(uri: Uri, fileType: FileType): Single<FileRef>
    fun upload(fileRef: FileRef): Single<FileRef>
    fun uploadAll(files: List<FileRef>): Single<List<FileRef>>
    fun update(fileRef: FileRef, uri: Uri): Single<FileRef>
    fun cancelUploadProgress(fileRef: FileRef): Completable
    fun cancelUploadProgress(files: List<FileRef>): Completable
    fun cancelDownloadProgress(fileRef: FileRef): Completable
    fun getFilePath(fileRef: FileRef): Single<List<FileRef>>
    fun getFiltered(filter: Filter.Snapshot): Single<List<FileRef>>
    fun getParent(uid: String?): Single<FileRef>
    fun favAll(files: List<FileRef>, fav: Boolean): Single<List<FileRef>>
    fun changeFolder(files: List<FileRef>, folderId: String?): Single<List<FileRef>>
    fun getChildren(folderId: String?, deep: Boolean, fileTypes: List<FileType> = emptyList()): Single<List<FileRef>>
    fun deleteAll(files: List<FileRef>, permanently: Boolean): Single<List<FileRef>>
    fun getRelativePath(folderId: String?, fileRef: FileRef): Single<String>

}