package clipto.presentation.auth
 
import android.app.Application
import androidx.fragment.app.FragmentActivity
import clipto.common.extensions.isContextDestroyed
import clipto.common.extensions.withPermissions
import clipto.common.extensions.withResult
import clipto.common.logging.L
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
 
internal class Auth(
    val app: Application,
    val theme: () -> Int,
    val logo: Int,
    val privacyUrl: String,
    val tosUrl: String
) : IAuth {
 
    private fun getGoogleSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
    }
 
    override fun signIn(token: String, callback: (authData: AuthData?, th: Throwable?) -> Unit) {
        // Direct custom token sign-in is not supported in pure Google Sign-in
        // We fallback to checking if there is already a signed-in account
        val account = GoogleSignIn.getLastSignedInAccount(app)
        if (account != null) {
            callback.invoke(GoogleAuthInfo(account), null)
        } else {
            callback.invoke(null, IllegalStateException("No Google Account signed in"))
        }
    }
 
    override fun signIn(activity: FragmentActivity, callback: (authData: AuthData?, th: Throwable?) -> Unit) {
        activity.withPermissions(android.Manifest.permission.INTERNET, android.Manifest.permission.ACCESS_NETWORK_STATE) {
            if (activity.isContextDestroyed()) {
                return@withPermissions
            }
            val gso = getGoogleSignInOptions()
            val client = GoogleSignIn.getClient(activity, gso)
            val intent = client.signInIntent
            
            if (activity.isContextDestroyed()) {
                return@withPermissions
            }
            activity.withResult(intent) { _, resultData ->
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(resultData)
                    val account = task.getResult(ApiException::class.java)
                    if (account != null) {
                        val authData = GoogleAuthInfo(account)
                        L.log(this, "signed in with Google: {}", authData)
                        callback.invoke(authData, null)
                    } else {
                        callback.invoke(null, IllegalStateException("Google Account is null"))
                    }
                } catch (e: Exception) {
                    val statusCode = (e as? ApiException)?.statusCode
                    L.log(this, "Google Sign-in error: {} (status code: {})", e.toString(), statusCode)
                    callback.invoke(null, e)
                }
            }
        }
    }
 
    override fun signOut(activity: FragmentActivity, callback: (authData: AuthData?, th: Throwable?) -> Unit) {
        val gso = getGoogleSignInOptions()
        val client = GoogleSignIn.getClient(activity, gso)
        client.signOut()
            .addOnSuccessListener {
                callback.invoke(null, null)
            }
            .addOnFailureListener {
                callback.invoke(null, it)
            }
    }
 
    internal class GoogleAuthInfo constructor(account: GoogleSignInAccount) : AuthData() {
        init {
            firebaseId = account.id
            providerId = "google"
            photoUrl = account.photoUrl?.toString()
            email = account.email
            displayName = account.displayName ?: account.email?.substringBefore('@')
        }
    }
}