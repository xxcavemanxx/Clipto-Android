package clipto.repository

import clipto.api.IApi
import clipto.dao.TxHelper
import clipto.dao.objectbox.*
import clipto.dao.objectbox.model.toBox
import clipto.domain.User
import clipto.extensions.log
import clipto.store.app.AppState
import clipto.store.clipboard.ClipboardState
import clipto.store.main.MainState
import clipto.store.user.UserState
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val api: Lazy<IApi>,
    private val txHelper: TxHelper,
    private val appState: AppState,
    private val userState: UserState,
    private val mainState: MainState,
    private val userBoxDao: UserBoxDao,
    private val clipboardState: ClipboardState,
    private val settingsBoxDao: SettingsBoxDao,
    private val objectBoxDaoHelper: ObjectBoxDaoHelper,
    private val filterRepository: IFilterRepository,
    private val clipRepository: IClipRepository,
    private val fileRepository: IFileRepository
) : IUserRepository {

    override fun init(): Completable = Completable
        .fromCallable {
            val user = userBoxDao.getUser()
            if (user != null) {
                userState.user.setValue(user)
            } else {
                userState.user.setValue(User.NULL)
            }
        }

    override fun terminate(): Completable = Completable
        .fromCallable {
            objectBoxDaoHelper.removeAll()
            userState.user.setValue(User.NULL)
            mainState.resetFilter()
            mainState.clearSelection()
            clipboardState.clearAll()
        }

    override fun generateAppLink(): Maybe<String> = api.get().getInvitationLink()

    override fun login(user: User): Single<User> = Single
        .fromCallable<User> {
            txHelper.inTx("User login") {
                settingsBoxDao.clear()
                userBoxDao.login(user.toBox())
            }
        }
        .doOnSuccess { userState.forceSync.setValue(true) }
        .doOnSuccess { userState.user.setValue(it) }

    override fun logout(user: User): Single<User> = filterRepository.terminate()
        .andThen(fileRepository.terminate())
        .andThen(clipRepository.terminate())
        .andThen(terminate())
        .toSingle { User.NULL }

    override fun update(user: User): Single<User> = Single
        .fromCallable { userBoxDao.save(user.toBox()) }
        .doOnSuccess { userState.user.setValue(it, force = true) }
        .map { it }

    override fun delete(user: User): Single<User> = api.get()
        .deleteAccount()
        .andThen(logout(user))

    override fun upgrade(): Single<String> = Single.just("No upgrade required")

}