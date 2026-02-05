package clipto.dao.objectbox

import clipto.analytics.Analytics
import clipto.common.extensions.getNotNull
import clipto.common.extensions.threadLocal
import clipto.common.extensions.toNullIfEmpty
import clipto.common.misc.FormatUtils
import clipto.dao.firebase.mapper.FileMapper
import clipto.dao.objectbox.model.FileRefBox
import clipto.dao.objectbox.model.FileRefBox_
import clipto.domain.*
import clipto.extensions.log
import com.wb.clipboard.R
import dagger.Lazy
import io.objectbox.kotlin.inValues
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileBoxDao @Inject constructor(
    private val filterBoxDao: Lazy<FilterBoxDao>,
    private val fileMapper: Lazy<FileMapper>
) : AbstractBoxDao<FileRefBox>() {

    private val foldersCache = mutableMapOf<String, FileRef?>()

    private val queryFirestoreIdEq = threadLocal {
        box.query()
            .equal(FileRefBox_.firestoreId, "")
            .build()
    }

    private val queryFolderIdEq = threadLocal {
        box.query()
            .equal(FileRefBox_.folderId, "")
            .build()
    }

    private val queryFolderIdEqAndFileTypeIn = threadLocal {
        box.query()
            .equal(FileRefBox_.folderId, "")
            .`in`(FileRefBox_.type, intArrayOf())
            .build()
    }

    private val queryFileTypeIn = threadLocal {
        box.query()
            .`in`(FileRefBox_.type, intArrayOf())
            .build()
    }

    private val queryFileIdsIn = threadLocal {
        box.query()
            .`in`(FileRefBox_.firestoreId, arrayOf(""))
            .build()
    }

    private val queryFolderEqAndNameEq = threadLocal {
        box.query()
            .equal(FileRefBox_.folderId, "")
            .equal(FileRefBox_.title, "")
            .build()
    }

    private val queryFavEqAndIsFolderEq = threadLocal {
        box.query()
            .equal(FileRefBox_.fav, true)
            .equal(FileRefBox_.isFolder, true)
            .build()
    }

    private val queryFolderNullAndNameEq = threadLocal {
        box.query()
            .isNull(FileRefBox_.folderId)
            .equal(FileRefBox_.title, "")
            .build()
    }

    override fun getType(): Class<FileRefBox> = FileRefBox::class.java

    fun getByFileType(fileTypes: List<FileType> = emptyList()): List<FileRef> {
        return queryFileTypeIn.getNotNull()
            .setParameters(FileRefBox_.type, fileTypes.map { it.typeId }.toIntArray())
            .find()
    }

    fun getChildren(folderId: String, fileTypes: List<FileType> = emptyList()): List<FileRef> {
        return if (fileTypes.isEmpty()) {
            queryFolderIdEq.getNotNull()
                .setParameter(FileRefBox_.folderId, folderId)
                .find()
        } else {
            queryFolderIdEqAndFileTypeIn.getNotNull()
                .setParameter(FileRefBox_.folderId, folderId)
                .setParameters(FileRefBox_.type, fileTypes.map { it.typeId }.toIntArray())
                .find()
        }
    }

    fun getChildrenDeep(folderId: String, fileTypes: List<FileType> = emptyList()): List<FileRef> {
        val all = mutableListOf<FileRef>()
        val children = getChildren(folderId, fileTypes)
        all.addAll(children)
        children.forEach { child ->
            if (child.isFolder) {
                all.addAll(getChildrenDeep(child.getUid()!!, fileTypes))
            }
        }
        return all
    }

    fun saveAll(files: List<FileRefBox>, updateDate: Date? = null) {
        log("files :: saveAll", files.size)
        files.forEach { file ->
            log("files :: save :: {}", file)
            file.normalize()
            if (file.isFolder) {
                file.getUid()?.let {
                    foldersCache[it]?.apply(file)
                }
            }
            if (updateDate != null) {
                file.updateDate = updateDate
            }
        }
        box.put(files)
    }

    fun delete(file: FileRefBox) {
        filterBoxDao.get().update(file, null)
        box.remove(file)
    }

    fun deleteAll(files: List<FileRefBox>) {
        files.forEach {
            filterBoxDao.get().update(it, null)
        }
        box.remove(files)
    }

    fun save(file: FileRefBox) {
        file.updateDate = Date()
        file.normalize()
        if (file.isFolder) {
            file.getUid()?.let {
                foldersCache.getOrPut(it) { file }?.apply(file)
            }
        }
        log("files :: save :: {}", file)
        box.put(file)
    }

    fun getById(id: Long): FileRefBox {
        return box.get(id)
    }

    fun getPath(fileRef: FileRef): List<FileRef> {
        return getPath(fileRef.folderId)
    }

    fun getPath(folderId: String?): List<FileRef> {
        var parentId = folderId
        val path = mutableListOf<FileRef>()
        while (parentId != null && !path.any { it.firestoreId == parentId }) {
            val parent = foldersCache.getOrPut(parentId) { getByUid(parentId) }
            if (parent != null) {
                path.add(parent)
                parentId = parent.folderId
            } else {
                path.clear()
                break
            }
        }
        path.reverse()
        return path
    }

    fun getByUid(uid: String?): FileRefBox? {
        if (uid == null) return null
        log("files :: getByUid :: {}", uid)
        return queryFirestoreIdEq.getNotNull()
            .setParameter(FileRefBox_.firestoreId, uid)
            .findFirst()
    }

    fun getByFolderAndName(folderId: String?, name: String?): FileRefBox? {
        if (name.isNullOrBlank()) return null
        return if (folderId.isNullOrBlank()) {
            queryFolderNullAndNameEq.getNotNull()
                .setParameter(FileRefBox_.title, name)
                .findFirst()
        } else {
            queryFolderEqAndNameEq.getNotNull()
                .setParameter(FileRefBox_.folderId, folderId)
                .setParameter(FileRefBox_.title, name)
                .findFirst()
        }
    }

    fun getByFavEqAndFolderEq(fav: Boolean = true, isFolder: Boolean = true): List<FileRefBox> {
        return queryFavEqAndIsFolderEq.getNotNull()
            .setParameter(FileRefBox_.fav, fav)
            .setParameter(FileRefBox_.isFolder, isFolder)
            .find()
    }

    fun getFiles(clip: Clip): List<FileRefBox> {
        return getFiles(clip.fileIds)
    }

    fun getFiles(ids: List<String>): List<FileRefBox> {
        if (ids.isEmpty()) return emptyList()
        val files = queryFileIdsIn.getNotNull()
            .setParameters(FileRefBox_.firestoreId, ids.toTypedArray())
            .find()
        return ids.map { uid ->
            var file = files.find { it.getUid() == uid }
            if (file == null) {
                file = fileMapper.get().createNewFile()
                    .apply {
                        error = app.getString(R.string.file_error_not_found)
                        objectType = ObjectType.READONLY
                        title = FormatUtils.UNKNOWN
                        firestoreId = uid
                        path = null
                    }
                box.put(file)
            }
            file
        }
    }

    fun getNotUploaded(): List<FileRefBox> {
        return box.query()
            .notEqual(FileRefBox_.uploaded, true)
            .notNull(FileRefBox_.uploadUrl)
            .isNull(FileRefBox_.error)
            .build()
            .find()
    }

    fun getNotDownloaded(): List<FileRefBox> {
        return box.query()
            .equal(FileRefBox_.uploaded, true)
            .notEqual(FileRefBox_.downloaded, true)
            .notNull(FileRefBox_.downloadUrl)
            .isNull(FileRefBox_.error)
            .build()
            .find()
    }

    fun getFiltered(filter: Filter.Snapshot): Query<FileRefBox> {
        val query = box.query()

        // ===== SPECIFIC =====

//        if (!filter.cleanupRequest && !filter.recycled) {
//            query.isNull(FileRefBox_.deleteDate)
//        }
        query.notEqual(FileRefBox_.objectType, ObjectType.READONLY.id.toLong())

        // ===== TEXT LIKE =====

        filter.textLike?.let {
            val textLikes = it.split(",").mapNotNull { tokens -> tokens.toNullIfEmpty() }
            textLikes.forEachIndexed { index, textLike ->
                if (textLike.matches("\\w+".toRegex()) && Character.UnicodeBlock.of(textLike.first()) == Character.UnicodeBlock.CYRILLIC) {
                    Analytics.onSearchByCyrillic()
                    val combinations = mutableSetOf<String>()
                    combinations.add(textLike.uppercase())
                    combinations.add(textLike.lowercase())
                    combinations.add(textLike.capitalize())
                    combinations.add(textLike)
                    combinations.forEachIndexed { i, s ->
                        query.contains(FileRefBox_.title, s, QueryBuilder.StringOrder.CASE_INSENSITIVE)
                            .or().contains(FileRefBox_.description, s, QueryBuilder.StringOrder.CASE_INSENSITIVE)
                            .or().equal(FileRefBox_.abbreviation, s, QueryBuilder.StringOrder.CASE_INSENSITIVE)
                        if (i < combinations.size - 1) {
                            query.or()
                        }
                    }
                } else {
                    query.contains(FileRefBox_.title, textLike, QueryBuilder.StringOrder.CASE_INSENSITIVE)
                        .or().contains(FileRefBox_.description, textLike, QueryBuilder.StringOrder.CASE_INSENSITIVE)
                        .or().equal(FileRefBox_.abbreviation, textLike, QueryBuilder.StringOrder.CASE_INSENSITIVE)
                }
                if (index < textLikes.size - 1) {
                    query.or()
                }
            }
        }

        // ===== WHERE FILE IDS =====

        if (filter.fileIds.isNotEmpty()) {
            when (filter.fileIdsWhereType) {
                Filter.WhereType.NONE_OF -> {
                    val ids = box.query().inValues(FileRefBox_.firestoreId, filter.fileIds.toTypedArray()).build().findIds()
                    query.notIn(FileRefBox_.id, ids)
                }
                else -> {

                }
            }
        }

        // ===== WHERE FILE TYPES IN =====
        if (filter.fileTypes.isNotEmpty()) {
            val types = filter.fileTypes.map { it.typeId }.toIntArray()

            when (filter.fileTypesWhereType) {
                Filter.WhereType.NONE_OF -> {
                    query.notIn(FileRefBox_.type, types)
                }
                Filter.WhereType.ANY_OF -> {
                    query.`in`(FileRefBox_.type, types)
                }
                else -> {

                }
            }
        }

        // ===== WHERE FOLDER EQ =====
        if (filter.folderIds.isNotEmpty()) {
            var orLogic = false
            val folderIds = filter.folderIds.mapNotNull { it.toNullIfEmpty() }.toTypedArray()
            if (filter.folderIds.size != folderIds.size) {
                query.isNull(FileRefBox_.folderId)
                orLogic = true
            }
            if (folderIds.isNotEmpty()) {
                if (orLogic) {
                    query.or()
                }
                if (folderIds.size == 1) {
                    query.equal(FileRefBox_.folderId, folderIds.first())
                } else {
                    query.`in`(FileRefBox_.folderId, folderIds)
                }
            }
        }

        query
            .orderDesc(FileRefBox_.isFolder)
            .orderDesc(FileRefBox_.fav)

        when (filter.sortBy) {
            SortBy.MODIFY_DATE_ASC -> query
                .order(FileRefBox_.updateDate)
            SortBy.MODIFY_DATE_DESC -> query
                .orderDesc(FileRefBox_.updateDate)

            SortBy.CREATE_DATE_ASC -> query
                .order(FileRefBox_.createDate)
            SortBy.CREATE_DATE_DESC -> query
                .orderDesc(FileRefBox_.createDate)

            SortBy.NAME_ASC -> query.order(FileRefBox_.title, QueryBuilder.NULLS_LAST or QueryBuilder.CASE_SENSITIVE)
                .orderDesc(FileRefBox_.createDate)
            SortBy.NAME_DESC -> query
                .order(FileRefBox_.title, QueryBuilder.DESCENDING or QueryBuilder.NULLS_LAST or QueryBuilder.CASE_SENSITIVE)
                .orderDesc(FileRefBox_.createDate)

            SortBy.SIZE_ASC -> query
                .order(FileRefBox_.size)
                .orderDesc(FileRefBox_.createDate)
            SortBy.SIZE_DESC -> query
                .orderDesc(FileRefBox_.size)
                .orderDesc(FileRefBox_.createDate)

            else -> {
                query.orderDesc(FileRefBox_.createDate)
            }
        }

        return query.build()
    }

}