package clipto.dao.objectbox.converter

import clipto.domain.NotificationStyle
import io.objectbox.converter.PropertyConverter

class NotificationStyleConverter : PropertyConverter<NotificationStyle, Int> {
    override fun convertToEntityProperty(from: Int?): NotificationStyle = NotificationStyle.byId(from)
    override fun convertToDatabaseValue(from: NotificationStyle): Int = from.id
}