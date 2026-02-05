package clipto.dao.objectbox.converter

import clipto.domain.ClipDetailsTab
import io.objectbox.converter.PropertyConverter

class ClipParamsTabConverter : PropertyConverter<ClipDetailsTab, Int> {
    override fun convertToEntityProperty(from: Int?): ClipDetailsTab = ClipDetailsTab.byId(from)
    override fun convertToDatabaseValue(from: ClipDetailsTab): Int = from.id
}