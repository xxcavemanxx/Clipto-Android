package clipto.dao.objectbox.model

import clipto.dao.objectbox.converter.FileTypeConverter
import clipto.dao.objectbox.converter.IdsToStringConverter
import clipto.dao.objectbox.converter.ObjectTypeConverter
import clipto.domain.FileRef
import clipto.domain.FileType
import clipto.domain.ObjectType
import clipto.domain.factory.FileRefFactory
import io.objectbox.annotation.*
import java.util.*

@Entity
class FileRefBox : FileRef() {

    @Id
    var id: Long = 0

    override var md5: String? = null
    @Index
    override var size: Long = 0

    @Index
    override var fav: Boolean = false

    @Index
    override var isFolder: Boolean = false

    @Index
    @Convert(converter = FileTypeConverter::class, dbType = Int::class)
    override var type: FileType = FileType.FILE

    @Index
    @Convert(converter = ObjectTypeConverter::class, dbType = Int::class)
    override var objectType: ObjectType = ObjectType.INTERNAL

    @Index
    override var title: String? = null

    override var path: String? = null
    override var folder: String? = null
    @Index
    override var createDate: Date? = null
    @Index
    override var modifyDate: Date? = null
    @Index
    override var updateDate: Date? = null
    @Index
    override var deleteDate: Date? = null
    override var mediaType: String? = null
    override var color: String? = null

    @Index(type = IndexType.HASH)
    override var folderId: String? = null

    @Index(type = IndexType.HASH)
    override var firestoreId: String? = null

    @Index
    override var downloaded: Boolean = false

    @Index
    override var uploaded: Boolean = false

    @Index(type = IndexType.HASH)
    override var downloadUrl: String? = null

    @Index(type = IndexType.HASH)
    override var uploadSessionUrl: String? = null

    @Index(type = IndexType.HASH)
    override var uploadUrl: String? = null

    override var platform: String? = null

    @Index(type = IndexType.HASH)
    override var error: String? = null

    @Index(type = IndexType.VALUE)
    override var description: String? = null

    @Index(type = IndexType.HASH)
    override var abbreviation: String? = null

    @Index(type = IndexType.VALUE)
    @Convert(converter = IdsToStringConverter::class, dbType = String::class)
    override var tagIds: List<String> = emptyList()

    @Index(type = IndexType.VALUE)
    @Convert(converter = IdsToStringConverter::class, dbType = String::class)
    override var snippetSetsIds: List<String> = emptyList()

    fun isValid(): Boolean = firestoreId != null && folder != null && title != null && uploadUrl != null

    override fun apply(from: FileRef): FileRefBox {
        super.apply(from)
        if (from is FileRefBox) {
            this.id = from.id
        }
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileRefBox

        if (firestoreId != other.firestoreId) return false

        return true
    }

    override fun hashCode(): Int {
        return firestoreId?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "FileRefBox(id=$id, md5=$md5, size=$size, fav=$fav, isFolder=$isFolder, type=$type, objectType=$objectType, title=$title, path=$path, folder=$folder, createDate=$createDate, modifyDate=$modifyDate, updateDate=$updateDate, deleteDate=$deleteDate, mediaType=$mediaType, color=$color, folderId=$folderId, firestoreId=$firestoreId, downloaded=$downloaded, uploaded=$uploaded, downloadUrl=$downloadUrl, uploadSessionUrl=$uploadSessionUrl, uploadUrl=$uploadUrl, platform=$platform, error=$error, description=$description, abbreviation=$abbreviation, tagIds=$tagIds, snippetSetsIds=$snippetSetsIds)"
    }

}

fun FileRef.toBox(new: Boolean = false): FileRefBox =
    if (!new && this is FileRefBox) {
        this
    } else {
        FileRefFactory.newInstance(this)
    }

fun FileRef.isNew(): Boolean = toBox().id == 0L

fun FileRef.copy(): FileRefBox = FileRefBox().apply(this)