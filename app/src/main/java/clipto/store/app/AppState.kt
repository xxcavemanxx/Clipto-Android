package clipto.store.app

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Bundle
import android.text.Spannable
import clipto.action.intent.IntentAction
import clipto.cache.AppColorCache
import clipto.cache.AppTextCache
import clipto.common.extensions.showToast
import clipto.common.extensions.toNullIfEmpty
import clipto.common.misc.IdUtils
import clipto.common.misc.ThemeUtils
import clipto.common.presentation.mvvm.LoadingStateProvider
import clipto.common.presentation.mvvm.model.DataLoadingState
import clipto.config.IAppConfig
import clipto.dao.objectbox.FilterBoxDao
import clipto.dao.objectbox.SettingsBoxDao
import clipto.domain.FastAction
import clipto.domain.Filters
import clipto.domain.FocusMode
import clipto.domain.Theme
import clipto.presentation.contextactions.ContextActionsActivity
import clipto.store.StoreObject
import clipto.store.StoreState
import clipto.store.analytics.AnalyticsState
import com.google.common.io.BaseEncoding
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import io.reactivex.Single
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AppState @Inject constructor(
    appConfig: IAppConfig,
    val app: Application,
    private val filterBoxDao: FilterBoxDao,
    private val settingsBoxDao: SettingsBoxDao,
    private val analyticsState: AnalyticsState
) : StoreState(appConfig), LoadingStateProvider {

    private val languageIdentification by lazy {
        val options = LanguageIdentificationOptions.Builder()
            .setConfidenceThreshold(appConfig.firebaseLanguageIdentificationThreshold())
            .build()
        LanguageIdentification.getClient(options)
    }

    val lastActivity = StoreObject<Class<Activity>>(
        id = "lastActivity",
        onChanged = { prev, next ->
            log("lastActivity :: {} -> {}", prev, next)
        }
    )

    val restart = StoreObject<Boolean>(id = "restart", liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER)

    val instanceId = StoreObject<String>(id = "instance_id")

    val language by lazy {
        val settings = settingsBoxDao.get()
        val lang = settings.selectedLanguage
        val country = settings.selectedCountry
        val initialValue =
            if (lang != null && country != null) {
                Locale(lang, country)
            } else {
                getLocale()
            }
        StoreObject(
            id = "language",
            initialValue = initialValue,
            onChanged = { _, next ->
                settings.selectedLanguage = next?.language
                settings.selectedCountry = next?.country
                requestRestart()
            }
        )
    }

    val universalClipboard by lazy {
        StoreObject(
            id = "universal_clipboard",
            initialValue = settingsBoxDao.get().universalClipboard
        )
    }

    val clipboard by lazy {
        StoreObject(
            id = "clipboard",
            initialValue = !settingsBoxDao.get().doNotTrackClipboardChanges,
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    val dataLoadingState by lazy {
        StoreObject<DataLoadingState>(
            id = "data_loading_state",
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    val navigateTo by lazy {
        StoreObject<NavigateToRequest>(
            id = "navigate_to",
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    val lastAction by lazy {
        StoreObject<IntentActionRequest>(
            id = "last_action",
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    val lastIntent by lazy {
        StoreObject<Intent>(
            id = "last_intent",
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    val clipInfoTextRequest = StoreObject<ClipInfoTextRequest>(
        id = "clip_info",
        liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
    )

    val filters by lazy {
        StoreObject<Filters>(
            id = "filters",
            onChanged = { _, next ->
                next!!
                next.groupNotes.notesCount = next.all.notesCount + next.deleted.notesCount
                next.groupFilters.notesCount = next.getNamedFilters().size.toLong()
                next.groupSnippets.notesCount = next.getSnippetKits().size.toLong()
                next.groupTags.notesCount = next.getTags().size.toLong()
                analyticsState.onUpdateAppState(this)
            }
        )
    }

    val settings by lazy {
        StoreObject(
            id = "settings",
            initialValue = settingsBoxDao.get(),
            onChanged = { _, _ ->
                theme.setValue(Theme.valueOf(settingsBoxDao.get()))
                universalClipboard.setValue(settingsBoxDao.get().universalClipboard)
                clipboard.setValue(!settingsBoxDao.get().doNotTrackClipboardChanges)
            }
        )
    }

    val theme by lazy {
        val storeObject = StoreObject<Theme>(
            id = "theme",
            onChanged = { _, value ->
                val newTheme = value!!
                settingsBoxDao.get().theme = newTheme.id
                ThemeUtils.clearCache()
                AppColorCache.clearCache()
                AppTextCache.clearCache()
                app.setTheme(newTheme.themeId)
            }
        )
        storeObject.setValue(Theme.valueOf(settingsBoxDao.get()))
        storeObject
    }

    val requestFastActionsUpdate by lazy {
        val settings = getSettings()
        if (settings.fastActionsMeta.isNotEmpty()) {
            FastAction.getAllActions().forEach { it.visible = false }
            settings.fastActionsMeta.forEach { meta ->
                FastAction.valueOf(meta.id)?.let {
                    it.visible = meta.visible
                    it.order = meta.order
                }
            }
        }
        StoreObject(id = "request_fast_actions_update", initialValue = FastActionsUpdateRequest())
    }

    override fun setLoadingState() {
        setLoadingState(DataLoadingState.LOADING)
    }

    override fun setLoadedState() {
        setLoadingState(DataLoadingState.LOADED)
    }

    fun isLastActivityContextActions(): Boolean = lastActivity.getValue() == ContextActionsActivity::class.java
    fun isLastActivityNull(): Boolean = lastActivity.getValue() == null

    fun getTheme() = theme.requireValue()
    fun getSettings() = settings.requireValue()
    fun getSelectedLocale() = language.requireValue()
    fun getInstanceId() = instanceId.getValue() ?: IdUtils.autoId()
    fun getFilters() = filters.getValue() ?: filterBoxDao.getFilters()

    fun getActiveFolderId() = getFilters().findActive().folderId.toNullIfEmpty()

    fun getVisibleClipActions(spannable: Spannable) = requestFastActionsUpdate.let { FastAction.getClipActions(app, spannable) }
    fun getFocusMode(): FocusMode = if (getSettings().focusOnTitle) FocusMode.TITLE else FocusMode.TEXT
    fun getFilterByClipboard() = getFilters().clipboard
    fun getFilterBySnippets() = getFilters().snippets
    fun getFilterByStarred() = getFilters().starred
    fun getFilterByDeleted() = getFilters().deleted
    fun getFilterByFolders() = getFilters().folders
    fun getFilterByLast() = getFilters().last
    fun getFilterByAll() = getFilters().all

    fun getGroupNotes() = getFilters().groupNotes

    fun refreshSettings() = settings.setValue(settingsBoxDao.get(), force = true)
    fun refreshFilters() = filters.setValue(filterBoxDao.getFilters(), force = true)

    fun requestRestart() = restart.setValue(true, force = true)
    fun requestIntentAction(action: IntentAction) = lastAction.setValue(IntentActionRequest(action = action))
    fun requestFastActionsUpdate() = requestFastActionsUpdate.setValue(FastActionsUpdateRequest())
    fun requestFastActionsRefresh() = requestFastActionsUpdate.setValue(FastActionsUpdateRequest(refresh = true))
    fun requestNavigateTo(destinationId: Int, args: Bundle? = null) = navigateTo.setValue(NavigateToRequest(destinationId = destinationId, args = args))
    fun requestShowClipInfoText(text: String, scanBarcode: Boolean = false) = clipInfoTextRequest.setValue(ClipInfoTextRequest(text = text, scanBarcode = scanBarcode))

    fun setLoadingState(state: DataLoadingState) = dataLoadingState.setValue(state)
    fun showToast(message: CharSequence) = onMain { app.showToast(message) }
    fun showToast(messageRes: Int) = onMain { app.showToast(messageRes) }

    fun getPackageName(): String = app.packageName

    fun getSignature(): String? {
        return try {
            val pm = app.packageManager
            val packageName = app.packageName
            val packageInfo = pm.getPackageInfo(packageName!!, PackageManager.GET_SIGNATURES)
            if (packageInfo?.signatures == null || packageInfo.signatures.isEmpty() || packageInfo.signatures[0] == null) {
                null
            } else {
                signatureDigest(packageInfo.signatures[0])
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getTextLanguage(text: String): Single<String> = Single
        .fromPublisher { publisher ->
            languageIdentification.identifyLanguage(text)
                .addOnSuccessListener {
                    val lang = if (it != "und") it else getSelectedLocale().language
                    publisher.onNext(lang)
                }
                .addOnFailureListener {
                    val lang = getSelectedLocale().language
                    publisher.onNext(lang)
                }
                .addOnCompleteListener {
                    publisher.onComplete()
                }
        }

    fun getTextPossibleLanguages(text: String): Single<List<TextLanguage>> = Single
        .fromPublisher { publisher ->
            languageIdentification.identifyPossibleLanguages(text)
                .addOnSuccessListener {
                    publisher.onNext(it.mapNotNull { TextLanguage(it.languageTag, it.confidence) })
                }
                .addOnFailureListener {
                    publisher.onError(it)
                }
                .addOnCompleteListener {
                    publisher.onComplete()
                }
        }

    private fun signatureDigest(sig: Signature): String? {
        val signature: ByteArray = sig.toByteArray()
        return try {
            val md: MessageDigest = MessageDigest.getInstance("SHA1")
            val digest: ByteArray = md.digest(signature)
            BaseEncoding.base16().lowerCase().encode(digest)
        } catch (e: NoSuchAlgorithmException) {
            null
        }
    }

    data class IntentActionRequest(
        val id: Long = System.currentTimeMillis(),
        val action: IntentAction
    )

    data class NavigateToRequest(
        val id: Long = System.currentTimeMillis(),
        val destinationId: Int,
        val args: Bundle? = null
    )

    data class FastActionsUpdateRequest(
        val id: Long = System.currentTimeMillis(),
        val refresh: Boolean = false
    )

    data class ClipInfoTextRequest(
        val id: Long = System.currentTimeMillis(),
        val scanBarcode: Boolean,
        val text: String
    )

}