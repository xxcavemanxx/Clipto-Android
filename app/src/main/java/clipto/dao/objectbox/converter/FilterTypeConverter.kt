package clipto.dao.objectbox.converter

import clipto.domain.Filter
import io.objectbox.converter.PropertyConverter

class FilterTypeConverter : PropertyConverter<Filter.Type, Int> {
    override fun convertToEntityProperty(from: Int?): Filter.Type = Filter.Type.byId(from)
    override fun convertToDatabaseValue(from: Filter.Type): Int = from.id
}