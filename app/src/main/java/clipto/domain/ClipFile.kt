package clipto.domain

import clipto.common.extensions.toDate
import clipto.common.extensions.toFormattedString
import clipto.common.misc.GsonUtils
import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.util.*

@Deprecated("need to be removed")
open class ClipFile {

    open var name: String? = null
    open var label: String? = null
    open var folder: String? = null
    open var type: FileType = FileType.FILE
    open var mediaType: String? = null
    open var size: Long = 0
    open var md5: String? = null
    open var uploaded: Boolean = false
    open var localUrl: String? = null
    open var error: String? = null

    open var createDate: Date? = null
    open var updateDate: Date? = null

    /** clip id */
    open var clipId: Long = 0L

    /** local state to check if file is downloaded */
    open var downloaded: Boolean = false

    /** local state to check if file is deleted */
    open var deleted: Boolean = false

    /** local url to continue download */
    open var downloadUrl: String? = null

    /** local url to continue upload */
    open var uploadUrl: String? = null


    open fun apply(from: ClipFile): ClipFile {
        name = from.name
        label = from.label
        folder = from.folder
        type = from.type
        mediaType = from.mediaType
        size = from.size
        md5 = from.md5
        clipId = from.clipId
        downloaded = from.downloaded
        uploaded = from.uploaded
        deleted = from.deleted
        downloadUrl = from.downloadUrl
        uploadUrl = from.uploadUrl
        localUrl = from.localUrl
        error = from.error
        return this
    }

    open fun apply(from: Meta): ClipFile {
        name = from.name
        label = from.label
        folder = from.folder
        type = from.type
        mediaType = from.mediaType
        size = from.size
        md5 = from.md5
        uploaded = from.uploaded
        localUrl = from.localUrl
        createDate = from.created
        updateDate = from.updated
        return this
    }

    @JsonAdapter(Meta.Adapter::class)
    data class Meta(
        var type: FileType = FileType.FILE,
        var size: Long = 0,
        var md5: String? = null,
        var name: String? = null,
        var label: String? = null,
        var created: Date? = null,
        var updated: Date? = null,
        var folder: String? = null,
        var uploaded: Boolean = false,
        var mediaType: String? = null,
        var localUrl: String? = null,
        var platform: String? = null
    ) {

        fun isValid(): Boolean = name != null && folder != null && label != null

        fun apply(from: ClipFile): Meta {
            type = from.type
            size = from.size
            md5 = from.md5
            name = from.name
            label = from.label
            created = from.createDate
            updated = from.updateDate
            folder = from.folder
            uploaded = from.uploaded
            mediaType = from.mediaType
            localUrl = from.localUrl
            return this
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Meta

            if (type != other.type) return false
            if (size != other.size) return false
            if (md5 != other.md5) return false
            if (name != other.name) return false
            if (label != other.label) return false
            if (folder != other.folder) return false
            if (uploaded != other.uploaded) return false
            if (mediaType != other.mediaType) return false
            if (localUrl != other.localUrl) return false
            if (platform != other.platform) return false

            return true
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + size.hashCode()
            result = 31 * result + (md5?.hashCode() ?: 0)
            result = 31 * result + (name?.hashCode() ?: 0)
            result = 31 * result + (label?.hashCode() ?: 0)
            result = 31 * result + (folder?.hashCode() ?: 0)
            result = 31 * result + uploaded.hashCode()
            result = 31 * result + (mediaType?.hashCode() ?: 0)
            result = 31 * result + (localUrl?.hashCode() ?: 0)
            result = 31 * result + (platform?.hashCode() ?: 0)
            return result
        }

        companion object {
            const val NAME = "n"
            const val LABEL = "l"
            const val FOLDER = "f"
            const val TYPE = "t"
            const val MEDIA_TYPE = "m"
            const val SIZE = "s"
            const val MD5 = "h"
            const val CREATED = "c"
            const val UPDATED = "u"
            const val UPLOADED = "r"
            const val LOCAL_PATH = "p"
            const val PLATFORM = "pl"

            private val TYPE_LIST = object : TypeToken<List<Meta>>() {}

            fun fromJson(json: String?): List<Meta> {
                if (json == null) {
                    return emptyList()
                }
                try {
                    return GsonUtils.get().fromJson(json, TYPE_LIST.type)
                } catch (th: Throwable) {
                    return emptyList()
                }
            }

            fun toJson(list: List<Meta>?): String? {
                if (list.isNullOrEmpty()) {
                    return null
                }
                try {
                    return GsonUtils.get().toJson(list)
                } catch (th: Throwable) {
                    return null
                }
            }
        }

        class Adapter : TypeAdapter<Meta>() {

            override fun write(to: JsonWriter, from: Meta) {
                to.beginObject()
                to.name(NAME).value(from.name)
                to.name(LABEL).value(from.label)
                to.name(FOLDER).value(from.folder)
                to.name(TYPE).value(from.type.typeId)
                to.name(MEDIA_TYPE).value(from.mediaType)
                to.name(SIZE).value(from.size)
                to.name(MD5).value(from.md5)
                to.name(UPLOADED).value(from.uploaded)
                to.name(LOCAL_PATH).value(from.localUrl)
                to.name(CREATED).value(from.created.toFormattedString())
                to.name(UPDATED).value(from.updated.toFormattedString())
                to.name(PLATFORM).value(from.platform)
                to.endObject()
            }

            override fun read(from: JsonReader): Meta {
                val to = Meta()
                from.beginObject()
                while (from.hasNext()) {
                    when (from.nextName()) {
                        NAME -> to.name = stringOrNull(from)
                        LABEL -> to.label = stringOrNull(from)
                        FOLDER -> to.folder = stringOrNull(from)
                        TYPE -> to.type = FileType.byId(from.nextInt())
                        MEDIA_TYPE -> to.mediaType = stringOrNull(from)
                        UPLOADED -> to.uploaded = from.nextBoolean()
                        SIZE -> to.size = from.nextLong()
                        MD5 -> to.md5 = stringOrNull(from)
                        LOCAL_PATH -> to.localUrl = stringOrNull(from)
                        CREATED -> to.created = stringOrNull(from).toDate()
                        UPDATED -> to.updated = stringOrNull(from).toDate()
                        PLATFORM -> to.platform = stringOrNull(from)
                    }
                }
                from.endObject()
                return to
            }

            private fun stringOrNull(from: JsonReader) = if (from.peek() == JsonToken.STRING) from.nextString() else from.nextNull().let { null }

        }

    }
}