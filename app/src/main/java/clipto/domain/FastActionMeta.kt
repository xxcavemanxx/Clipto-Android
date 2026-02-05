package clipto.domain

import clipto.common.misc.GsonUtils
import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

@JsonAdapter(FastActionMeta.Adapter::class)
data class FastActionMeta(
    var id: Int = 0,
    var order: Int = 0,
    var visible: Boolean = false
) {
    companion object {
        const val ID = "i"
        const val ORDER = "o"
        const val VISIBLE = "v"

        private val TYPE_LIST = object : TypeToken<List<FastActionMeta>>() {}

        fun fromJson(json: String?): List<FastActionMeta> {
            if (json == null) {
                return emptyList()
            }
            try {
                return GsonUtils.get().fromJson(json, TYPE_LIST.type)
            } catch (th: Throwable) {
                return emptyList()
            }
        }

        fun toJson(list: List<FastActionMeta>?): String? {
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

    class Adapter : TypeAdapter<FastActionMeta>() {

        override fun write(to: JsonWriter, from: FastActionMeta) {
            to.beginObject()
            to.name(ID).value(from.id)
            to.name(ORDER).value(from.order)
            to.name(VISIBLE).value(from.visible)
            to.endObject()
        }

        override fun read(from: JsonReader): FastActionMeta {
            val to = FastActionMeta()
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