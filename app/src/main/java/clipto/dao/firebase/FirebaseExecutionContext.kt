package clipto.dao.firebase

import clipto.store.app.AppState
import clipto.store.user.UserState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseExecutionContext @Inject constructor(
    val appState: AppState,
    val userState: UserState,
    val firebaseDaoHelper: FirebaseDaoHelper
)