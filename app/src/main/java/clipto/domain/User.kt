package clipto.domain

open class User {

    open var role: UserRole = UserRole.USER

    open var email: String? = null
    open var photoUrl: String? = null
    open var firebaseId: String? = null
    open var providerId: String? = null
    open var displayName: String? = null
    open var invitedCount: Int = 0

    open var syncLimit: Int = 0
    open var syncIsRestricted = false
    open var syncSubscriptionId: String? = null
    open var syncSubscriptionToken: String? = null
    open var license: LicenseType = LicenseType.NONE

    fun isAuthorized() = firebaseId != null

    fun canSyncNewNotes() = !syncIsRestricted

    fun getTitle() = displayName ?: email

    fun getDetailedName(): String? {
        val titleRef = getTitle()
        val emailRef = email
        if (emailRef != null) {
            if (titleRef !== emailRef) {
                return "$titleRef (${emailRef})"
            }
        }
        return titleRef
    }

    fun apply(from: User): User {
        role = from.role
        photoUrl = from.photoUrl
        firebaseId = from.firebaseId
        providerId = from.providerId
        displayName = from.displayName
        invitedCount = from.invitedCount
        syncIsRestricted = from.syncIsRestricted
        syncSubscriptionId = from.syncSubscriptionId
        syncSubscriptionToken = from.syncSubscriptionToken
        syncLimit = from.syncLimit
        license = from.license
        email = from.email
        return this
    }

    companion object {
        val NULL: User = User()
    }
}