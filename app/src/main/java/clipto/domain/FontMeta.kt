package clipto.domain

import clipto.common.misc.GsonUtils
import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

@JsonAdapter(FontMeta.Adapter::class)
data class FontMeta(
    var id: Int = 0,
    var order: Int = 0,
    var visible: Boolean = false
) {
    companion object {
        const val ID = "i"
        const val ORDER = "o"
        const val VISIBLE = "v"

        private val TYPE_LIST = object : TypeToken<List<FontMeta>>() {}

        fun fromJson(json: String?): List<FontMeta> {
            if (json == null) {
                return emptyList()
            }
            return try {
                GsonUtils.get().fromJson(json, TYPE_LIST.type)
            } catch (th: Throwable) {
                emptyList()
            }
        }

        fun toJson(list: List<FontMeta>?): String? {
            if (list.isNullOrEmpty()) {
                return null
            }
            return try {
                GsonUtils.get().toJson(list)
            } catch (th: Throwable) {
                null
            }
        }
    }

    class Adapter : TypeAdapter<FontMeta>() {

        override fun write(to: JsonWriter, from: FontMeta) {
            to.beginObject()
            to.name(ID).value(from.id)
            to.name(ORDER).value(from.order)
            to.name(VISIBLE).value(from.visible)
            to.endObject()
        }

        override fun read(from: JsonReader): FontMeta {
            val to = FontMeta()
            from.beginObject()
            while (from.hasNext()) {
                when (from.nextName()) {
                    ID -> to.id = from.nextInt()
                    ORDER -> to.order = from.nextInt()
                    VISIBLE -> to.visible = from.nextBoolean()
                }
            }
            from.endObject()
            return to
        }

    }
}