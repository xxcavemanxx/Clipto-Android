package clipto.dao.objectbox.converter

import clipto.domain.*
import io.objectbox.converter.PropertyConverter

class TextTypeInConverter : PropertyConverter<List<TextType>, String?> {
    override fun convertToEntityProperty(from: String?): List<TextType> = from
            ?.let { it.split(",").mapNotNull { it.toIntOrNull() }.map { TextType.byId(it) } }
            ?: emptyList()

    override fun convertToDatabaseValue(from: List<TextType>?): String? = from
            ?.takeIf { it.isNotEmpty() }
            ?.let { types -> types.joinToString(",") }
}