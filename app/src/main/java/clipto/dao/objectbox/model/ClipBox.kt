package clipto.dao.objectbox.model

import clipto.dao.objectbox.converter.*
import clipto.domain.*
import io.objectbox.annotation.*
import java.util.*

@Entity
class ClipBox : Clip() {

    @Id
    var localId: Long = 0

    @Convert(converter = ObjectTypeConverter::class, dbType = Int::class)
    override var objectType: ObjectType = ObjectType.INTERNAL

    @Index(type = IndexType.HASH)
    override var firestoreId: String? = null

    @Index(type = IndexType.HASH)
    override var snippetId: String? = null

    @Index
    override var fav: Boolean = false

    @Index(type = IndexType.VALUE)
    override var text: String? = null

    @Index(type = IndexType.VALUE)
    @Convert(converter = IdsToStringConverter::class, dbType = String::class)
    override var tagIds: List<String> = emptyList()

    @Index(type = IndexType.VALUE)
    @Convert(converter = IdsToStringConverter::class, dbType = String::class)
    override var fileIds: List<String> = emptyList()

    @Index(type = IndexType.VALUE)
    @Convert(converter = IdsToStringConverter::class, dbType = String::class)
    override var snippetSetsIds: List<String> = emptyList()

    @Index(type = IndexType.HASH)
    override var abbreviation: String? = null

    @Index(type = IndexType.HASH)
    override var folderId: String? = null

    @Index(type = IndexType.VALUE)
    override var description: String? = null

    @Index
    var tags: String? = null

    @Index
    override var title: String? = null

    @Index
    override var changeTimestamp: Long = 0

    @Index
    override var platform: String? = null

    @Index
    override var createDate: Date? = null

    @Index
    override var updateDate: Date? = null

    @Index
    override var modifyDate: Date? = null

    @Index
    override var deleteDate: Date? = null

    @Index
    @Convert(converter = TextTypeConverter::class, dbType = Int::class)
    override var textType: TextType = TextType.TEXT_PLAIN

    @Index
    override var tracked: Boolean = false

    @Index
    override var usageCount: Int = 0

    @Index
    @Convert(converter = ClipFilesConverter::class, dbType = String::class)
    override var files: List<ClipFile.Meta> = emptyList()

    @Index
    override var filesCount: Int = 0

    @Index
    override var size: Long = 0

    @Index
    @Convert(converter = PublicLinkConverter::class, dbType = String::class)
    override var publicLink: PublicLink? = null

    @Index
    override var characters: Int = 0

    @Index
    override var snippet: Boolean = false

    @Index
    override var dynamic: Boolean = false

    override fun apply(from: Clip): ClipBox {
        super.apply(from)
        if (from is ClipBox) {
            this.localId = from.localId
        }
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClipBox

        if (localId != other.localId) return false

        return true
    }

    override fun hashCode(): Int = localId.hashCode()

}

fun Clip.toBox(new: Boolean = false): ClipBox =
    if (!new && this is ClipBox) {
        this
    } else {
        ClipBox().apply(this)
    }