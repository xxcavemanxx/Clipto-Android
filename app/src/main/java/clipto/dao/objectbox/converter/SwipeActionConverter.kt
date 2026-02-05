package clipto.dao.objectbox.converter

import clipto.domain.SwipeAction
import io.objectbox.converter.PropertyConverter

class SwipeActionConverter : PropertyConverter<SwipeAction, Int> {
    override fun convertToEntityProperty(from: Int?): SwipeAction = SwipeAction.byId(from)
    override fun convertToDatabaseValue(from: SwipeAction): Int = from.id
}