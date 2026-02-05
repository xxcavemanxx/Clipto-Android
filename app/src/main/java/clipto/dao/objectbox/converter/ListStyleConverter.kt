package clipto.dao.objectbox.converter

import clipto.domain.ListStyle
import io.objectbox.converter.PropertyConverter

class ListStyleConverter : PropertyConverter<ListStyle, Int> {
    override fun convertToEntityProperty(from: Int?): ListStyle = ListStyle.byId(from)
    override fun convertToDatabaseValue(from: ListStyle): Int = from.id
}