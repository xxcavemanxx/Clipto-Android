package clipto.repository

import clipto.domain.Clip
import clipto.domain.FileRef
import clipto.domain.Filter
import io.reactivex.Completable
import io.reactivex.Single

interface IClipRepository {

    fun init(): Completable
    fun terminate(): Completable
    fun getById(id: Long): Single<Clip>
    fun getByFile(fileRef: FileRef): Single<List<Clip>>
    fun getByText(text: String?, id: Long = 0): Single<Clip>

    fun clearClipboard(): Single<List<Clip>>
    fun clearRecycleBin(): Single<List<Clip>>

    fun restoreLastCopiedClip(): Single<Clip>

    fun createLink(clip: Clip): Single<Clip>
    fun removeLink(clip: Clip): Single<Clip>

    fun save(clip: Clip, copied: Boolean): Single<Clip>
    fun undoDeleteAll(clips: List<Clip>): Single<List<Clip>>
    fun favAll(clips: List<Clip>, fav: Boolean): Single<List<Clip>>
    fun tagAll(clips: List<Clip>, assignTagIds: List<String>): Single<List<Clip>>
    fun syncAll(newClips: List<Clip> = emptyList(), callback: (clips: List<Clip>) -> Unit = {})
    fun deleteAll(clips: List<Clip>, permanently: Boolean = false, withUndo: Boolean = false, clearClipboard: Boolean = true): Single<List<Clip>>
    fun deleteAllFromFilters(filters: List<Filter>, clips: List<Clip>? = null): Single<List<Clip>>

    fun changeFolder(clips: List<Clip>, folderId: String?): Single<List<Clip>>
    fun getChildren(folderIds: List<String>): Single<List<Clip>>

    fun getRelativePath(folderId: String?, clip: Clip): Single<String>

    fun unlink(files: List<FileRef>): Single<List<Clip>>

}