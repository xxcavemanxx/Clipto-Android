package clipto.action

import clipto.analytics.Analytics
import clipto.api.IApi
import clipto.api.data.CheckSessionRequest
import clipto.domain.User
import clipto.repository.IClipRepository
import clipto.repository.IUserRepository
import clipto.store.internet.InternetState
import clipto.store.purchase.PurchaseState
import clipto.store.user.UserState
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckUserSessionAction @Inject constructor(
    private val api: Lazy<IApi>,
    private val userState: UserState,
    private val internetState: InternetState,
    private val purchaseState: PurchaseState,
    private val userRepository: Lazy<IUserRepository>,
    private val clipRepository: Lazy<IClipRepository>
) : CompletableAction<CheckUserSessionAction.Context>() {

    private val syncRetryCount = 10
    private var syncRetryAttempt = 0
    private var previousNotesNumber = -1L

    override val name: String = "check_user_session"

    init {
        userState.user.getLiveChanges({
            it.value?.takeIf { !it.isAuthorized() }?.let {
                log("reset syncRetryAttempt")
                syncRetryAttempt = 0
            }
        })
    }

    fun reset(): CheckUserSessionAction {
        previousNotesNumber = -1L
        return this
    }

    override fun subscribeOn(): Scheduler = SCHEDULER_ACTION_SINGLE

    override fun canExecute(context: Context): Boolean = context.force || (
            !isInProgress()
                    && userState.isAuthorized()
                    && previousNotesNumber != userState.getAllNotesCount()
            )

    fun execute(force: Boolean = false, callback: () -> Unit = {}) = execute(Context(force = force), callback)

    override fun create(context: Context): Completable {
        val syncedNotesCount: Int by lazy { userState.getSyncedNotesCount() }
        return Single.just(false)
            .flatMap {
                val subs = purchaseState.subs.requireValue()
                val user = userState.user.requireValue()
                log("onCheckPlan (1): subs={}", subs)
                if (user.syncIsRestricted && syncedNotesCount < userState.getSyncFreeLimit()) {
                    log("onCheckPlan (1.1): remove restriction due to forced check")
                    user.syncIsRestricted = false
                    log("update user (1.1)")
                    updateUser(user).map { true }
                } else if (subs.isEmpty()) {
                    if (!user.isAuthorized()) {
                        log("onCheckPlan (2): is not authorized")
                        Single.just(true)
                    } else if (user.syncIsRestricted && syncedNotesCount < userState.getSyncLimit()) {
                        log("onCheckPlan (3): remove restriction")
                        user.syncIsRestricted = false
                        log("update user (1)")
                        updateUser(user).map { true }
                    } else if (!appConfig.syncPlanActivated()) {
                        log("onCheckPlan (4): is not activated")
                        Single.just(true)
                    } else if (syncRetryAttempt >= syncRetryCount || !internetState.isConnected()) {
                        if (!user.syncIsRestricted && syncedNotesCount >= userState.getSyncLimit()) {
                            log("onCheckPlan (5): restrict access")
                            user.syncIsRestricted = true
                            Analytics.onRestrictSync()
                            log("update user (2)")
                            updateUser(user).map { true }
                        } else {
                            Single.just(true)
                        }
                    } else {
                        Single.just(false)
                    }
                } else {
                    Single.just(false)
                }
            }
            .flatMapCompletable { complete ->
                if (complete) {
                    Completable.complete()
                } else {
                    val user = userState.user.requireValue()
                    val subs = purchaseState.subs.requireValue()
                    api.get().checkSession(CheckSessionRequest(syncRetryAttempt, syncedNotesCount))
                        .flatMapCompletable { response ->
                            var userUpdated = false

                            response.syncLimit?.let { newLimit ->
                                if (newLimit != user.syncLimit) {
                                    user.syncLimit = newLimit
                                    val isRestricted = syncedNotesCount >= newLimit && syncedNotesCount >= appConfig.syncPlanNotesFreeLimit()
                                    if (isRestricted != user.syncIsRestricted) {
                                        user.syncIsRestricted = isRestricted
                                    }
                                    userUpdated = true
                                }
                            }

                            response.syncSubscriptionId?.let { syncSubscriptionId ->
                                if (syncSubscriptionId != user.syncSubscriptionId) {
                                    user.syncSubscriptionId = syncSubscriptionId
                                    userUpdated = true
                                }
                            }

                            response.syncSubscriptionToken?.let { syncSubscriptionToken ->
                                if (syncSubscriptionToken != user.syncSubscriptionToken) {
                                    user.syncSubscriptionToken = syncSubscriptionToken
                                    userUpdated = true
                                }
                            }

                            response.plan?.let { newLicense ->
                                if (newLicense != user.license) {
                                    user.license = newLicense
                                    userUpdated = true
                                }
                            }

                            syncRetryAttempt = syncRetryCount

                            if (userUpdated) {
                                log("update user: 3")
                                updateUser(user).ignoreElement()
                            } else if (subs.isEmpty() && (user.syncLimit <= syncedNotesCount || !user.license.isSubscriptionPlan())) {
                                log("fetch plans")
                                purchaseState.fetchActiveSubscriptions().getLiveChanges()
                                    .filter { it.isNotNull() }
                                    .filter { !it.value.isNullOrEmpty() }
                                    .firstElement()
                                    .flatMapCompletable {
                                        log("fetched plans: {}", it.value)
                                        create(context)
                                    }
                            } else {
                                Completable.complete()
                            }
                        }
                        .onErrorResumeNext {
                            syncRetryAttempt++
                            if (syncRetryAttempt < syncRetryCount) {
                                create(context)
                            } else {
                                Analytics.onError("error_check_plan", it)
                                Completable.complete()
                            }
                        }
                }
            }
            .doOnComplete { previousNotesNumber = userState.getAllNotesCount() }
    }

    private fun updateUser(user: User): Single<User> = userRepository.get()
        .update(user).doAfterSuccess { clipRepository.get().syncAll() }

    data class Context(val force: Boolean) : ActionContext()

}