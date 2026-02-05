package clipto.dao.objectbox.converter

import clipto.domain.ObjectType
import io.objectbox.converter.PropertyConverter

class ObjectTypeConverter : PropertyConverter<ObjectType, Int> {
    override fun convertToEntityProperty(from: Int?): ObjectType? = ObjectType.byId(from)
    override fun convertToDatabaseValue(from: ObjectType): Int = from.id
}