package clipto.presentation.auth

open class AuthData {

    var firebaseId: String? = null
    var providerId: String? = null
    var displayName: String? = null
    var photoUrl: String? = null
    var email: String? = null

    override fun toString(): String {
        return "AuthData(firebaseId=$firebaseId, providerId=$providerId, displayName=$displayName, photoUrl=$photoUrl, email=$email)"
    }

}