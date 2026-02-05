package clipto.action

import clipto.api.IApi
import clipto.domain.User
import clipto.repository.IClipRepository
import clipto.repository.ISettingsRepository
import clipto.repository.IUserRepository
import clipto.store.internet.InternetState
import clipto.store.user.UserState
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartUserSessionAction @Inject constructor(
        private val api: Lazy<IApi>,
        private val userState: UserState,
        private val internetState: InternetState,
        private val clipRepository: Lazy<IClipRepository>,
        private val userRepository: Lazy<IUserRepository>,
        private val settingsRepository: Lazy<ISettingsRepository>
) : CompletableAction<ActionContext>() {

    override val name: String = "start_user_session"

    override fun subscribeOn(): Scheduler = SCHEDULER_ACTION_SINGLE

    override fun canExecute(context: ActionContext): Boolean =
            userState.isAuthorized() && internetState.isConnected() && !isInProgress()

    fun execute(callback: () -> Unit = {}) = execute(ActionContext.EMPTY, callback)

    override fun create(context: ActionContext): Completable {
        val user = userState.user.requireValue()
        val settings = settingsRepository.get().get()
        return api.get().startSession()
                .map { session ->
                    var userUpdated = false
                    session.invitedCount?.let { newCount ->
                        if (newCount != user.invitedCount) {
                            user.invitedCount = newCount
                            userUpdated = true
                        }
                    }
                    if (session.userRole != user.role) {
                        user.role = session.userRole
                        userUpdated = true
                    }
                    userUpdated
                }
                .flatMapSingle {
                    if (it) {
                        updateUser(user)
                    } else {
                        Single.just(user)
                    }
                }
                .flatMap {
                    if (settings.referralId != null) {
                        settings.referralId = null
                        settingsRepository.get().update(settings)
                    } else {
                        Single.just(settings)
                    }
                }
                .ignoreElement()
    }

    private fun updateUser(user: User): Single<User> = userRepository.get()
            .update(user).doAfterSuccess { clipRepository.get().syncAll() }

}