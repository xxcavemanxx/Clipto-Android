package clipto.dao.objectbox.converter

import clipto.domain.FastActionMeta
import io.objectbox.converter.PropertyConverter

class FastActionMetaConverter : PropertyConverter<List<FastActionMeta>, String?> {
    companion object {
        private var cachedJsonValue: String? = null
        fun cache(from: List<FastActionMeta>): List<FastActionMeta> {
            cachedJsonValue = FastActionMeta.toJson(from)
            return from
        }
    }

    override fun convertToDatabaseValue(from: List<FastActionMeta>?): String? = cachedJsonValue
    override fun convertToEntityProperty(from: String?): List<FastActionMeta> = FastActionMeta.fromJson(from).also { cachedJsonValue = from }
}