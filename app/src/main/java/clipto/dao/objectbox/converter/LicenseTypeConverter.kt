package clipto.dao.objectbox.converter

import clipto.domain.LicenseType
import io.objectbox.converter.PropertyConverter

class LicenseTypeConverter : PropertyConverter<LicenseType, Int> {
    override fun convertToEntityProperty(from: Int?): LicenseType = LicenseType.byId(from)
    override fun convertToDatabaseValue(from: LicenseType): Int = from.id
}