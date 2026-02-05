package clipto.action

import clipto.common.presentation.mvvm.model.AuthorizationState
import clipto.dao.sharedprefs.SharedPrefsDao
import clipto.repository.IClipRepository
import clipto.repository.IFileRepository
import clipto.repository.IFilterRepository
import clipto.repository.IUserRepository
import clipto.store.main.MainState
import clipto.store.user.UserState
import dagger.Lazy
import io.reactivex.Completable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InitAppAction @Inject constructor(
    private val userState: UserState,
    private val mainState: MainState,
    private val sharedPrefsDao: SharedPrefsDao,
    private val userRepository: Lazy<IUserRepository>,
    private val clipRepository: Lazy<IClipRepository>,
    private val fileRepository: Lazy<IFileRepository>,
    private val filterRepository: Lazy<IFilterRepository>,
    private val startUserSessionAction: StartUserSessionAction,
    private val checkUserSessionAction: CheckUserSessionAction
) : CompletableAction<ActionContext>() {

    override val name: String = "init_app_action"

    override fun create(context: ActionContext): Completable = sharedPrefsDao
        .getInstanceId()
        .flatMap { userRepository.get().init().toSingleDefault(true) }
        .flatMapObservable { userState.authorizationState.getLiveChanges().toObservable() }
        .map {
            log("authorized (1) :: {}", it)

            val forceSync = userState.forceSync.requireValue()

            if (forceSync) {
                mainState.resetFilter()
            }

            if (it.value == AuthorizationState.AUTHORIZATION_REQUIRED) {
                userState.requestSignIn()
            }

            startUserSessionAction.execute {
                checkUserSessionAction.reset().execute()
            }

            log("authorized (2) :: {}", it)

            forceSync
        }
        .flatMapCompletable {
            filterRepository.get().init()
                .andThen(fileRepository.get().init())
                .andThen(clipRepository.get().init())
                .andThen(fileRepository.get().resume())
                .andThen(userRepository.get().upgrade().ignoreElement())
                .doFinally { appState.setLoadedState() }
                .doOnComplete { log("init completed") }
        }

}