package clipto

import android.app.Application
import androidx.annotation.StringRes
import clipto.action.*
import clipto.api.IApi
import clipto.config.IAppConfig
import clipto.dao.firebase.FirebaseDaoHelper
import clipto.dao.objectbox.FilterBoxDao
import clipto.dao.objectbox.SettingsBoxDao
import clipto.domain.Clip
import clipto.domain.Filter
import clipto.domain.Filters
import clipto.domain.Settings
import clipto.dynamic.IDynamicValuesRepository
import clipto.extensions.from
import clipto.presentation.preview.link.LinkPreview
import clipto.presentation.preview.link.LinkPreviewFactory
import clipto.presentation.preview.link.LinkPreviewState
import clipto.repository.IClipRepository
import clipto.repository.IFilterRepository
import clipto.store.app.AppState
import clipto.store.clipboard.ClipboardState
import clipto.store.clipboard.IClipboardStateManager
import clipto.store.internet.InternetState
import clipto.store.main.MainState
import clipto.store.user.UserState
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppContext @Inject constructor(
    val app: Application,
    val appState: AppState,
    val mainState: MainState,
    val userState: UserState,
    val appConfig: IAppConfig,
    val filterBoxDao: FilterBoxDao,
    val settingsBoxDao: SettingsBoxDao,
    val internetState: InternetState,
    val clipboardState: ClipboardState,
    val linkPreviewState: LinkPreviewState,
    val linkPreviewFactory: LinkPreviewFactory,
    val clipRepository: Lazy<IClipRepository>,
    val filterRepository: Lazy<IFilterRepository>,
    val dynamicValuesRepository: Lazy<IDynamicValuesRepository>,
    private val api: Lazy<IApi>,
    private val firebaseDaoHelper: FirebaseDaoHelper,
    private val clipboardStateManager: Lazy<IClipboardStateManager>,
    private val checkUserSessionAction: CheckUserSessionAction,
    private val saveClipAction: SaveClipAction,
    private val shareClipsAction: ShareClipsAction,
    private val getClipTextAction: GetClipTextAction,
    private val getClipByTextAction: GetClipByTextAction
) {

    companion object {
        lateinit var INSTANCE: AppContext
        fun get() = INSTANCE
    }

    init {
        INSTANCE = this
    }

    fun getSettings(): Settings = settingsBoxDao.get()
    fun getFilters(): Filters = filterBoxDao.getFilters()
    fun getAllTags(): List<Filter> = getFilters().getSortedTags()

    fun withInternet(success: () -> Unit, failed: () -> Unit = {}) = internetState.withInternet(success, failed)
    fun onUniversalCopy(clip: Clip) = clipboardStateManager.get().onUniversalCopy(clip)
    fun getAuthUserCollection() = firebaseDaoHelper.getAuthUserCollection()
    fun onCheckSession() = checkUserSessionAction.execute()

    fun showToast(message: CharSequence) = appState.showToast(message)
    fun string(@StringRes id: Int, vararg args: Any?): CharSequence = app.getString(id, *args)
    fun string(@StringRes id: Int): CharSequence = app.getString(id)
    fun onBackground(func: () -> Unit) = appState.onBackground(func)
    fun onMain(func: () -> Unit) = appState.onMain(func)
    fun setLoadingState() = appState.setLoadingState()
    fun setLoadedState() = appState.setLoadedState()

    fun onGetUrlShortLink(
        url: String,
        onSuccess: (shortUrl: String) -> Unit,
        onFailure: (th: Throwable) -> Unit = {}
    ) = api.get()
        .getUrlShortLink(url)
        .subscribeOn(appState.getBackgroundScheduler())
        .observeOn(appState.getViewScheduler())
        .subscribe(onSuccess, onFailure)

    fun onGetClipByText(
        id: Long,
        text: String,
        fail: (text: String) -> Unit = {},
        success: (clip: Clip) -> Unit
    ) = getClipByTextAction.execute(
        id = id,
        text = text,
        fail = fail,
        success = success
    )

    fun onGetClipText(
        clip: Clip,
        callback: (text: String) -> Unit
    ) = getClipTextAction.execute(
        clip,
        callback
    )

    fun onSave(
        clip: Clip,
        withLoadingState: Boolean = true,
        withSilentValidation: Boolean = false,
        callback: (clip: Clip) -> Unit = {}
    ) = saveClipAction.execute(
        clip = clip,
        copied = false,
        withLoadingState = withLoadingState,
        withSilentValidation = withSilentValidation,
        callback = callback
    )

    fun onShare(
        clips: Collection<Clip>,
        clearSelection: Boolean = true
    ) = shareClipsAction.execute(
        clips,
        clearSelection
    )

    fun onCopy(
        vararg clips: Clip,
        clearSelection: Boolean = true,
        saveCopied: Boolean = true,
        callback: () -> Unit = {}
    ) = clipboardStateManager.get().onCopy(
        clips = clips.toList(),
        clearSelection = clearSelection,
        saveCopied = saveCopied,
        callback = callback
    )

    fun onCopy(
        clip: Clip,
        clearSelection: Boolean = true,
        saveCopied: Boolean = true,
        callback: () -> Unit = {}
    ) = clipboardStateManager.get().onCopy(
        clip = clip,
        clearSelection = clearSelection,
        saveCopied = saveCopied,
        callback = callback
    )

    fun onCopy(
        text: String,
        clearSelection: Boolean = false,
        saveCopied: Boolean = true,
        callback: () -> Unit = {}
    ) = clipboardStateManager.get().onCopy(
        clip = Clip.from(text, tracked = true),
        clearSelection = clearSelection,
        saveCopied = saveCopied,
        callback = callback
    )

    fun onShowPreview(preview: LinkPreview) {
        linkPreviewState.requestPreview.setValue(preview, force = true)
    }

}