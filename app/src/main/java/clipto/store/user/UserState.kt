package clipto.store.user

import android.app.Application
import clipto.action.intent.IntentActionFactory
import clipto.action.intent.provider.AppAuthProvider
import clipto.common.analytics.A
import clipto.common.extensions.disposeSilently
import clipto.common.misc.AndroidUtils
import clipto.common.presentation.mvvm.model.AuthorizationState
import clipto.config.IAppConfig
import clipto.dao.objectbox.ClipBoxDao
import clipto.domain.*
import clipto.extensions.getTitleRes
import clipto.extensions.isNew
import clipto.store.StoreObject
import clipto.store.StoreState
import clipto.store.analytics.AnalyticsState
import clipto.store.app.AppState
import com.wb.clipboard.R
import io.reactivex.Maybe
import io.reactivex.disposables.Disposable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserState @Inject constructor(
    appConfig: IAppConfig,
    private val app: Application,
    private val appState: AppState,
    private val clipBoxDao: ClipBoxDao,
    private val analyticsState: AnalyticsState,
    private val intentActionFactory: IntentActionFactory
) : StoreState(appConfig) {

    val authorizationState by lazy { StoreObject<AuthorizationState>("authorization_state") }
    val signOutInProgress by lazy { StoreObject("sign_out_in_progress", false) }
    val deletedTags by lazy { StoreObject<List<Filter>>("deleted_tags") }
    val syncLimit by lazy { StoreObject<CharSequence>("sync_limit") }
    val license by lazy { StoreObject<CharSequence>("license") }
    val invitations by lazy { StoreObject<Int>("invitations") }
    val canSync by lazy { StoreObject<Boolean>("canSync", liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER) }

    val requestSignIn = StoreObject<SignInRequest>("request_sign_in", liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER)
    val requestSignOut = StoreObject<SignOutRequest>("request_sign_out", liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER)
    val requestShareApp = StoreObject<ShareAppRequest>("request_share_app", liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER)

    private var authDisposable: Disposable? = null

    val forceSync by lazy {
        StoreObject(
            id = "forceSync",
            initialValue = false,
            onChanged = { _, value ->
                appState.getFilters().initial = value ?: false
            })
    }

    val user by lazy {
        StoreObject(
            id = "user",
            initialValue = User.NULL,
            onChanged = { _, value ->
                if (value != null && value != User.NULL) {
                    // analytics
                    analyticsState.onUpdateUserState(this)

                    // invitations
                    invitations.setValue(value.invitedCount)

                    // license
                    if (value.isAuthorized()) {
                        val plan = value.license
                        val syncLimit = getSyncLimit()
                        if (syncLimit > 0) {
                            license.setValue(app.getString(plan.getTitleRes(), syncLimit))
                        } else if (plan == LicenseType.CONTRIBUTOR || plan == LicenseType.PERSONAL) {
                            license.setValue(app.getString(plan.getTitleRes(), app.getString(R.string.filter_limit_unlimited)))
                        }
                    } else {
                        license.clearValue()
                    }

                    // sync limit
                    val syncLimitString = app.getString(R.string.account_sync_plan_title, getSyncLimit())
                    syncLimit.setValue(syncLimitString)

                    // authorization state
                    authorizationState.setValue(AuthorizationState.AUTHORIZED)

                    signOutInProgress.clearValue()
                } else {
                    authorizationState.setValue(AuthorizationState.NOT_AUTHORIZED)
                    forceSync.setValue(false)
                    invitations.clearValue()
                    syncLimit.clearValue()
                    canSync.setValue(true)
                    license.clearValue()
                    A.init(null)
                }
            })
    }

    fun isSignOutInProgress(): Boolean = signOutInProgress.requireValue()
    fun isAdmin(): Boolean = user.requireValue().role == UserRole.ADMIN
    fun isAuthorized(): Boolean = user.requireValue().isAuthorized()
    fun getUserName(): String? = user.getValue()?.displayName
    fun getUserId(): String? = user.getValue()?.firebaseId

    fun canSyncNote(clip: Clip): Boolean = canSyncNotes() && (!clip.tracked || appState.getSettings().universalClipboard || clip.forceSync)
    fun getAllNotesCount(): Long = appState.getFilterByAll().notesCount + appState.getFilterByDeleted().notesCount
    fun getSyncLimit(): Int = user.requireValue().syncLimit + getSyncFreeLimit() + getSyncBonusForPublicKits()
    fun isNotSynced(clip: Clip?): Boolean = isAuthorized() && !clip.isNew() && clip?.firestoreId == null
    fun canSyncNewNotes(): Boolean = user.requireValue().canSyncNewNotes()
    fun canSyncNotes(): Boolean = isSyncEnabled() && canSyncNewNotes()
    fun getSyncedNotesCount(): Int = clipBoxDao.getSyncedClipsCount()
    fun isSyncEnabled(): Boolean = !appState.getSettings().disableSync
    fun getSyncFreeLimit(): Int = appConfig.syncPlanNotesFreeLimit()

    fun getSyncBonusForPublicKits(): Int = appState.getFilters().getSnippetKits()
        .filter { it.snippetKit?.isPublished() == true && it.objectType != ObjectType.EXTERNAL_SNIPPET_KIT }
        .sumOf { it.snippetKit?.snippetsCount ?: 0 }
        .times(appConfig.syncPlanNotesBonusForSnippet())

    fun requestSignIn(request: SignInRequest) {
        requestSignIn.setValue(request)
    }

    fun requestSignIn(webAuth: Boolean = false, withWarning: Boolean = false) {
        requestSignIn(SignInRequest(withWarning = withWarning, webAuth = webAuth))
    }

    fun requestSignOut(withConfirm: Boolean = true) {
        requestSignOut.setValue(SignOutRequest(withConfirm = withConfirm))
    }

    fun requestShareApp() {
        requestShareApp.setValue(ShareAppRequest())
    }

    fun signIn(request: SignInRequest): Maybe<User> {
        return if (isAuthorized()) {
            Maybe.just(user.requireValue())
        } else {
            user.getLiveChanges()
                .filter { requestSignIn.getValue() == request }
                .filter { it.isNotNull() }
                .map { it.value!! }
                .filter { it.isAuthorized() }
                .firstElement()
                .doOnSubscribe {
                    if (appState.isLastActivityContextActions()) {
                        val intent = intentActionFactory.getIntent(AppAuthProvider.Action())
                        app.startActivity(intent)
                    } else {
                        requestSignIn.setValue(request)
                    }
                }
        }
    }

    fun signOut(withConfirm: Boolean = true): Maybe<User> {
        return if (!isAuthorized()) {
            Maybe.just(user.requireValue())
        } else {
            val request = SignOutRequest(withConfirm = withConfirm)
            user.getLiveChanges()
                .filter { requestSignOut.getValue() == request }
                .filter { it.isNotNull() }
                .map { it.value!! }
                .filter { !it.isAuthorized() }
                .firstElement()
                .doOnSubscribe { requestSignOut.setValue(request) }
        }
    }

    fun withAuth(withWarning: Boolean = true, callback: () -> Unit = {}) {
        authDisposable.disposeSilently()
        authDisposable = signIn(SignInRequest.newRequireAuthRequest(withWarning = withWarning))
            .subscribeOn(getBackgroundScheduler())
            .observeOn(getViewScheduler())
            .subscribe({ callback.invoke() }, {})
    }

    data class SignInRequest(
        val id: Int = AndroidUtils.nextId(),
        val withWarning: Boolean,
        val webAuth: Boolean,
        val webAuthToken: String? = null,
        val iconRes: Int = R.drawable.ic_sync_enabled,
        val titleRes: Int = R.string.sync_warning_title,
        val actionTextRes: Int = R.string.button_continue,
        val descriptionRes: Int = R.string.sync_warning_description
    ) {
        companion object {
            fun newRequireAuthRequest(withWarning: Boolean = true) = SignInRequest(
                webAuth = false,
                withWarning = withWarning,
                iconRes = R.drawable.account_icon,
                titleRes = R.string.account_title_required_authorization,
                descriptionRes = R.string.account_description_required_authorization,
                actionTextRes = R.string.account_button_sign_in
            )

            fun newWebAuthTokenRequest(token: String) = SignInRequest(
                webAuthToken = token,
                withWarning = false,
                webAuth = true
            )
        }
    }

    data class SignOutRequest(
        val id: Int = AndroidUtils.nextId(),
        val withConfirm: Boolean
    )

    data class ShareAppRequest(
        val id: Int = AndroidUtils.nextId()
    )

}