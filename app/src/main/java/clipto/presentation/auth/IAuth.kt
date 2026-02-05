package clipto.presentation.auth

import androidx.fragment.app.FragmentActivity

interface IAuth {

    fun signIn(token:String, callback: (authData: AuthData?, th: Throwable?) -> Unit)

    fun signIn(activity: FragmentActivity, callback: (authData: AuthData?, th: Throwable?) -> Unit)

    fun signOut(activity: FragmentActivity, callback: (authData: AuthData?, th: Throwable?) -> Unit)

}