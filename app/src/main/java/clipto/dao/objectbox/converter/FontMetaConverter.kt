package clipto.dao.objectbox.converter

import clipto.domain.FontMeta
import io.objectbox.converter.PropertyConverter

class FontMetaConverter : PropertyConverter<List<FontMeta>, String?> {
    companion object {
        private var cachedJsonValue: String? = null
        fun cache(from: List<FontMeta>): List<FontMeta> {
            cachedJsonValue = FontMeta.toJson(from)
            return from
        }
    }

    override fun convertToDatabaseValue(from: List<FontMeta>?): String? = cachedJsonValue
    override fun convertToEntityProperty(from: String?): List<FontMeta> = FontMeta.fromJson(from).also { cachedJsonValue = from }
}