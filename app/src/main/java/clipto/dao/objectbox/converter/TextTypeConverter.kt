package clipto.dao.objectbox.converter

import clipto.domain.TextType
import io.objectbox.converter.PropertyConverter

class TextTypeConverter : PropertyConverter<TextType, Int> {
    override fun convertToEntityProperty(from: Int?): TextType = TextType.byId(from)
    override fun convertToDatabaseValue(from: TextType): Int = from.typeId
}