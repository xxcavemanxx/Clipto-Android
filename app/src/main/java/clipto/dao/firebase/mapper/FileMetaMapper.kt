package clipto.dao.firebase.mapper

import clipto.domain.ClipFile
import clipto.domain.FileType

object FileMetaMapper {

    fun fromMap(list: List<Map<String, Any?>>?): List<ClipFile.Meta> {
        if (list.isNullOrEmpty()) {
            return emptyList()
        }
        return list.map { it ->
            val meta = ClipFile.Meta()
            it[ClipFile.Meta.NAME]?.let { meta.name = it.toString() }
            it[ClipFile.Meta.LABEL]?.let { meta.label = it.toString() }
            it[ClipFile.Meta.FOLDER]?.let { meta.folder = it.toString() }
            it[ClipFile.Meta.TYPE]?.let { meta.type = FileType.byId((it.toString().toIntOrNull())) }
            it[ClipFile.Meta.MEDIA_TYPE]?.let { meta.mediaType = it.toString() }
            it[ClipFile.Meta.SIZE]?.let { meta.size = it as Long }
            it[ClipFile.Meta.MD5]?.let { meta.md5 = it.toString() }
            it[ClipFile.Meta.CREATED]?.let { meta.created = DateMapper.toDate(it) }
            it[ClipFile.Meta.UPDATED]?.let { meta.updated = DateMapper.toDate(it) }
            it[ClipFile.Meta.UPLOADED]?.let { meta.uploaded = it as Boolean }
            meta
        }
    }

    fun toMap(list: List<ClipFile.Meta>?): List<Map<String, Any?>>? {
        if (list.isNullOrEmpty()) {
            return null
        }
        return list.map {
            mapOf(
                ClipFile.Meta.NAME to it.name,
                ClipFile.Meta.LABEL to it.label,
                ClipFile.Meta.FOLDER to it.folder,
                ClipFile.Meta.TYPE to it.type.typeId,
                ClipFile.Meta.MEDIA_TYPE to it.mediaType,
                ClipFile.Meta.SIZE to it.size,
                ClipFile.Meta.MD5 to it.md5,
                ClipFile.Meta.CREATED to it.created,
                ClipFile.Meta.UPDATED to it.updated,
                ClipFile.Meta.UPLOADED to it.uploaded
            )
        }
    }

}