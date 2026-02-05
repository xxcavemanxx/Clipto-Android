package clipto.dao.objectbox.converter

import clipto.domain.SortBy
import io.objectbox.converter.PropertyConverter

class SortByConverter : PropertyConverter<SortBy, Int> {
    override fun convertToEntityProperty(from: Int?): SortBy = SortBy.byId(from)
    override fun convertToDatabaseValue(from: SortBy): Int = from.id
}