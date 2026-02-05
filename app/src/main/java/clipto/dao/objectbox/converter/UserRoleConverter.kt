package clipto.dao.objectbox.converter

import clipto.domain.UserRole
import io.objectbox.converter.PropertyConverter

class UserRoleConverter : PropertyConverter<UserRole, String> {
    override fun convertToEntityProperty(from: String?): UserRole = UserRole.byId(from)
    override fun convertToDatabaseValue(from: UserRole): String = from.id
}