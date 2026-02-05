package clipto.dao.objectbox.model

import clipto.dao.objectbox.converter.LicenseTypeConverter
import clipto.dao.objectbox.converter.UserRoleConverter
import clipto.domain.LicenseType
import clipto.domain.User
import clipto.domain.UserRole
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
class UserBox : User() {

    @Id
    var localId: Long = 0

    override var email: String? = null
    override var photoUrl: String? = null
    override var firebaseId: String? = null
    override var providerId: String? = null
    override var displayName: String? = null

    @Convert(converter = UserRoleConverter::class, dbType = String::class)
    override var role: UserRole = UserRole.USER

    @Convert(converter = LicenseTypeConverter::class, dbType = Int::class)
    override var license: LicenseType = LicenseType.NONE
    override var syncSubscriptionToken: String? = null
    override var syncSubscriptionId: String? = null
    override var syncIsRestricted: Boolean = false
    override var syncLimit: Int = 0

    override var invitedCount: Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserBox

        if (localId != other.localId) return false

        return true
    }

    override fun hashCode(): Int {
        return localId.hashCode()
    }

}

fun User.toBox(): UserBox =
        if (this is UserBox) {
            this
        } else {
            UserBox().apply(this) as UserBox
        }