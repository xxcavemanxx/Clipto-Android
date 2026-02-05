package clipto.dao.objectbox.converter

import clipto.domain.TimePeriod
import io.objectbox.converter.PropertyConverter

class TimePeriodConverter : PropertyConverter<TimePeriod, Int> {
    override fun convertToEntityProperty(from: Int?): TimePeriod? = TimePeriod.byId(from)
    override fun convertToDatabaseValue(from: TimePeriod): Int = from.id
}