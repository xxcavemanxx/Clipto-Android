package clipto.dao.objectbox.converter

import clipto.domain.Filter
import io.objectbox.converter.PropertyConverter

class WhereTypeConverter : PropertyConverter<Filter.WhereType, Int> {
    override fun convertToEntityProperty(from: Int?): Filter.WhereType = Filter.WhereType.byId(from)
    override fun convertToDatabaseValue(from: Filter.WhereType): Int = from.id
}