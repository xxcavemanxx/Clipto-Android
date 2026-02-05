package clipto.presentation.auth

import android.app.Application
import androidx.fragment.app.FragmentActivity
import clipto.common.extensions.isContextDestroyed
import clipto.common.extensions.withPermissions
import clipto.common.extensions.withResult
import clipto.common.logging.L
import com.facebook.FacebookSdk
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

internal class Auth(
    val app: Application,
    val theme: () -> Int,
    val logo: Int,
    val privacyUrl: String,
    val tosUrl: String
) : IAuth {

    override fun signIn(token: String, callback: (authData: AuthData?, th: Throwable?) -> Unit) {
        FirebaseAuth.getInstance().signInWithCustomToken(token)
            .addOnSuccessListener {
                val authData = getAuthData()
                L.log(this, "check authData: {}", authData)
                if (authData != null) {
                    L.log(this, "signed in: {}", authData)
                    callback.invoke(authData, null)
                }
            }
            .addOnFailureListener { callback.invoke(null, it) }
    }

    private fun initializeDeps(activity: FragmentActivity) {
        runCatching {
            FacebookSdk.sdkInitialize(activity)
            FacebookSdk.fullyInitialize()
        }
    }

    override fun signIn(activity: FragmentActivity, callback: (authData: AuthData?, th: Throwable?) -> Unit) {
        initializeDeps(activity)
        activity.withPermissions(android.Manifest.permission.INTERNET, android.Manifest.permission.ACCESS_NETWORK_STATE) {
            if (activity.isContextDestroyed()) {
                return@withPermissions
            }
            val intent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAlwaysShowSignInMethodScreen(true)
                .setIsSmartLockEnabled(false)
                .apply {
                    val themeId = theme.invoke()
                    if (themeId != 0) {
                        setTheme(themeId)
                    }
                    if (logo != 0) {
                        setLogo(logo)
                    }
                    val providers = mutableListOf<AuthUI.IdpConfig>()
                    if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(app) == ConnectionResult.SUCCESS) {
                        providers.add(AuthUI.IdpConfig.GoogleBuilder().build())
                    }
                    if (FacebookSdk.isInitialized()) {
                        providers.add(AuthUI.IdpConfig.FacebookBuilder().build())
                    }
                    providers.add(AuthUI.IdpConfig.EmailBuilder().build())
                    providers.add(AuthUI.IdpConfig.PhoneBuilder().build())

                    setAvailableProviders(providers)
                }
                .setTosAndPrivacyPolicyUrls(tosUrl, privacyUrl)
                .build()
            if (activity.isContextDestroyed()) {
                return@withPermissions
            }
            activity.withResult(intent) { _, _ ->
                val authData = getAuthData()
                L.log(this, "check authData: {}", authData)
                if (authData != null) {
                    L.log(this, "signed in: {}", authData)
                    callback.invoke(authData, null)
                }
            }
        }
    }

    override fun signOut(activity: FragmentActivity, callback: (authData: AuthData?, th: Throwable?) -> Unit) {
        initializeDeps(activity)
        AuthUI.getInstance()
            .signOut(activity)
            .addOnSuccessListener {
                val authData = getAuthData()
                callback.invoke(authData, null)
            }
            .addOnFailureListener { callback.invoke(null, it) }
    }

    private fun getAuthData(): AuthData? {
        val auth = FirebaseAuth.getInstance()
        return auth.currentUser?.let { FirabaseAuthInfo(it) }
    }

    internal class FirabaseAuthInfo constructor(user: FirebaseUser) : AuthData() {

        init {
            firebaseId = user.uid
            providerId = user.providerId
            photoUrl = user.photoUrl?.toString()
            email = user.email
            displayName =
                if (user.displayName == null) {
                    if (user.email == null) {
                        null
                    } else {
                        val lastIndex = user.email!!.lastIndexOf('@')
                        user.email!!.substring(0, lastIndex)
                    }
                } else {
                    user.displayName
                }
        }

    }

}