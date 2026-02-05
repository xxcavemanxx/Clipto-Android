package clipto.dao.objectbox.converter

import io.objectbox.converter.PropertyConverter

class IdsToStringConverter : PropertyConverter<List<String>, String?> {
    companion object {
        private val cache = hashMapOf<String, List<String>>()
    }

    override fun convertToEntityProperty(from: String?): List<String> = from
            ?.let {
                cache.getOrPut(it) {
                    it.split("|").filter { id -> id.isNotEmpty() }
                }
            }
            ?: emptyList()

    override fun convertToDatabaseValue(from: List<String>?): String? = from
            ?.takeIf { it.isNotEmpty() }
            ?.let { tags -> tags.joinToString(separator = "|", prefix = "|", postfix = "|") }
}