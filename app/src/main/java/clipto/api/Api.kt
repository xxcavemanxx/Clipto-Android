package clipto.api

import android.app.Application
import android.text.format.Formatter
import clipto.AppUtils
import clipto.analytics.Analytics
import clipto.api.data.*
import clipto.common.extensions.castToListOfMaps
import clipto.common.extensions.castToMap
import clipto.common.extensions.inBrackets
import clipto.common.misc.GsonUtils
import clipto.config.IAppConfig
import clipto.dao.firebase.FirebaseDaoHelper
import clipto.dao.firebase.FunctionsFunctionsHelper
import clipto.dao.firebase.mapper.*
import clipto.dao.objectbox.ClipBoxDao
import clipto.dao.objectbox.SettingsBoxDao
import clipto.dao.objectbox.model.toBox
import clipto.domain.*
import clipto.extensions.log
import clipto.extensions.sku
import clipto.repository.data.SnippetKitData
import clipto.store.app.AppState
import clipto.store.purchase.PurchaseState
import clipto.store.user.UserState
import com.wb.clipboard.BuildConfig
import io.reactivex.Completable
import io.reactivex.Maybe
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Api @Inject constructor(
    private val app: Application,
    private val appConfig: IAppConfig,
    private val appState: AppState,
    private val userState: UserState,
    private val purchaseState: PurchaseState,
    private val functionsHelper: FunctionsFunctionsHelper,
    private val settingsBoxDao: SettingsBoxDao,
    private val clipBoxDao: ClipBoxDao,
    private val fileMapper: FileMapper,
    private val filterMapper: FilterMapper
) : IApi {

    override fun deleteAccount(): Completable = functionsHelper.exec<Map<*, Any?>>("deleteAccount").ignoreElement()

    override fun startSession(): Maybe<StartSessionResponse> = Maybe
        .fromCallable {
            val settings = settingsBoxDao.get()
            mapOf(
                "settings" to GsonUtils.toStringSilent(Settings().apply(settings)),
                "notSyncedNotesCount" to clipBoxDao.getNotSyncedClipsCount(),
                "syncedNotesCount" to clipBoxDao.getSyncedClipsCount(),
                "deviceId" to appState.getInstanceId(),
                "deviceModel" to AppUtils.getPlatform(),
                "appVersion" to BuildConfig.VERSION_CODE,
                "language" to userState.getLanguage(),
                "referral" to settings.referralId,
                "version" to appConfig.getApiVersion()
            )
        }
        .flatMap { params ->
            val user = userState.user.requireValue()
            val id = "startSession_${user.toBox().localId}_${user.firebaseId}"
            functionsHelper.execOnce<Map<*, *>>(id, "startSession", params)
        }
        .map { data ->
            StartSessionResponse(
                invitedCount = data["invitedCount"]?.let { it as Int },
                userRole = UserRole.byId(data["role"]?.toString())
            )
        }

    override fun getInvitationLink(): Maybe<String> = functionsHelper
        .exec<Map<*, *>>("generateAppLink")
        .map { it["shortLink"]?.toString() }

    override fun checkSession(request: CheckSessionRequest): Maybe<CheckSessionResponse> = Maybe
        .fromCallable {
            val subs = purchaseState.subs.requireValue()
            val user = userState.user.requireValue()
            mapOf(
                "subs" to subs.map { it.sku() to it.purchaseToken }.toMap(),
                "syncSubscriptionToken" to user.syncSubscriptionToken,
                "syncSubscriptionId" to user.syncSubscriptionId,
                "syncIsRestricted" to user.syncIsRestricted,
                "syncLimit" to user.syncLimit,
                "deviceId" to appState.getInstanceId(),
                "language" to appState.getLanguage(),
                "appVersion" to BuildConfig.VERSION_CODE,
                "deviceModel" to AppUtils.getPlatform(),
                "attemptNumber" to request.retryAttempt,
                "apiVersion" to appConfig.getApiVersion(),
                "notesCount" to request.notesCount
            )
        }
        .flatMap { params ->
            val user = userState.user.requireValue()
            val subs = purchaseState.subs.requireValue()
            val requestId = "checkSession_${user.toBox().localId}_${user.firebaseId}_${subs.joinToString("_") { it.purchaseToken }}"
            log("check session request id: {}", requestId)
            functionsHelper.execOnce<Map<*, *>>(requestId, "checkUserSession", params)
        }
        .map { data ->
            log("check session response: {}", data)
            CheckSessionResponse(
                syncLimit = data["syncLimit"]?.let { it as Int },
                plan = data["plan"]?.let { it as Int }?.let { LicenseType.byId(it) },
                syncSubscriptionId = data["syncSubscriptionId"]?.let { it as String },
                syncSubscriptionToken = data["syncSubscriptionToken"]?.let { it as String }
            )
        }

    override fun getUrlShortLink(url: String): Maybe<String> = functionsHelper
        .exec<Map<*, Any?>>("getLinkShortenUrl", mapOf("link" to url))
        .map { it["shortenLink"]?.toString() }

    override fun createNotePublicLink(clip: Clip): Maybe<Clip> = Maybe.just(clip.toBox(new = true))
        .flatMap { clipBox ->
            val noteId = clipBox.firestoreId
            if (noteId != null) {
                val publicLink = clipBox.publicLink ?: PublicLink()
                val params = mapOf(
                    FirebaseDaoHelper.ATTR_PUBLIC_NOTE_ONETIME to publicLink.oneTimeOpening,
                    FirebaseDaoHelper.ATTR_PUBLIC_NOTE_POSTPONE_IN_MILLIS to publicLink.postponeInMillis,
                    FirebaseDaoHelper.ATTR_PUBLIC_NOTE_POSTPONE_AT_DATE to GsonUtils.formatDate(publicLink.postponeAtDate),
                    FirebaseDaoHelper.ATTR_PUBLIC_NOTE_EXPIRES_IN_MILLIS to publicLink.expiresInMillis,
                    FirebaseDaoHelper.ATTR_PUBLIC_NOTE_EXPIRES_AT_DATE to GsonUtils.formatDate(publicLink.expiresAtDate),
                    FirebaseDaoHelper.ATTR_PUBLIC_NOTE_PASSWORD_CLUE to publicLink.passwordClue,
                    FirebaseDaoHelper.ATTR_PUBLIC_NOTE_PASSWORD to publicLink.password,
                    FirebaseDaoHelper.ATTR_PUBLIC_NOTE_LOCKED to publicLink.locked,
                    FirebaseDaoHelper.ATTR_PUBLIC_NOTE_TITLE to clipBox.title,
                    FirebaseDaoHelper.ATTR_PUBLIC_NOTE_ID to noteId
                )
                functionsHelper.exec<Map<*, Any?>>("notePublicLinkGenerate", params)
                    .map { PublicNoteLinkMapper.fromMap(it) }
                    .map {
                        clipBox.publicLink = it
                        clipBox
                    }
            } else {
                Maybe.just(clipBox)
            }
        }

    override fun removeNotePublicLink(clip: Clip): Maybe<Clip> = Maybe.just(clip.toBox(new = true))
        .flatMap { clipBox ->
            val noteId = clipBox.firestoreId
            if (noteId != null) {
                clipBox.publicLink = null
                val params = mapOf(FirebaseDaoHelper.ATTR_PUBLIC_NOTE_ID to noteId)
                functionsHelper.exec<Any>("notePublicLinkRemove", params).map { clipBox }
            } else {
                Maybe.just(clipBox)
            }
        }

    override fun getFilePublicLink(fileRef: FileRef): Maybe<String> = Maybe
        .fromCallable {
            val fileFolder = fileRef.folder!!
            val fileName = fileRef.getUid()!!
            mapOf(
                "name" to fileName,
                "folder" to fileFolder,
                "title" to "${fileRef.title} ${Formatter.formatShortFileSize(app, fileRef.size).inBrackets()}"
            )
        }
        .flatMap { params -> functionsHelper.exec<Map<*, Any?>>("filePublicLinkGet", params) }
        .map { it["shortLink"]?.toString() ?: "" }
        .doOnError { Analytics.onError("error_attachment_download_url", it) }
        .onErrorReturnItem("")

    override fun getSnippetKitCategories(): Maybe<List<SnippetKitCategory>> = functionsHelper
        .exec<Any?>("snippet_kit_category_list")
        .map(Response::wrap)
        .map { it.data.castToListOfMaps() }
        .map { it.mapNotNull(SnippetKitCategoryMapper::fromMap) }

    override fun getSnippetKits(category: SnippetKitCategory?): Maybe<List<SnippetKit>> = functionsHelper
        .exec<Any?>("snippet_kit_list", mapOf("categoryId" to category?.id))
        .map(Response::wrap)
        .map { it.data.castToListOfMaps() }
        .map { it.mapNotNull(SnippetKitMapper::fromMap) }

    override fun getSnippetKit(id: String): Maybe<SnippetKitData> = functionsHelper
        .exec<Map<*, *>>("snippet_kit_get", mapOf("id" to id))
        .map(Response::wrap)
        .map {
            val accessRole = it.accessRole
            val kit = SnippetKitMapper.fromMap(it.data)
            if (kit != null) {
                SnippetKitData(
                    kit = kit,
                    accessRole = accessRole
                )
            } else {
                null
            }
        }

    override fun publishSnippetKit(filter: Filter, language: String, country: String): Maybe<SnippetKit> = Maybe
        .fromCallable {
            mapOf(
                "filterId" to filter.uid!!,
                "country" to country,
                "language" to language
            )
        }
        .flatMap { params -> functionsHelper.exec<Map<*, *>>("snippet_kit_publish", params) }
        .map(Response::wrap)
        .map { SnippetKitMapper.fromMap(it.data, silentCatch = false) }

    override fun discardSnippetKit(filter: Filter): Maybe<SnippetKit> = functionsHelper
        .exec<Map<*, *>>("snippet_kit_discard", mapOf("filterId" to filter.uid!!))
        .map(Response::wrap)
        .map { SnippetKitMapper.fromMap(it.data, silentCatch = false) }

    override fun createSnippetKitLink(filter: Filter): Maybe<SnippetKit> = functionsHelper
        .exec<Map<*, *>>("snippet_kit_link_create", mapOf("filterId" to filter.uid!!))
        .map(Response::wrap)
        .map { SnippetKitMapper.fromMap(it.data, silentCatch = false) }

    override fun removeSnippetKitLink(filter: Filter): Maybe<SnippetKit> = functionsHelper
        .exec<Map<*, *>>("snippet_kit_link_remove", mapOf("filterId" to filter.uid!!))
        .map(Response::wrap)
        .map { SnippetKitMapper.fromMap(it.data, silentCatch = false) }

    override fun updateSnippetKit(request: UpdateSnippetKitRequest): Maybe<SnippetKit> = Maybe
        .fromCallable {
            mapOf(
                "userId" to request.kit.userId,
                "filterId" to request.kit.filterId,
                "updateReason" to request.message,
                "publicStatus" to request.status?.id,
                "language" to request.language,
                "country" to request.country,
                "categoryId" to request.categoryId,
            )
        }
        .flatMap { params -> functionsHelper.exec<Any?>("snippet_kit_update", params) }
        .map(Response::wrap)
        .map { SnippetKitMapper.fromMap(it.data) }

    override fun installSnippetKit(kit: SnippetKit): Maybe<Filter> = Maybe
        .fromCallable {
            mapOf("kitId" to kit.id)
        }
        .flatMap { params -> functionsHelper.exec<Any?>("snippet_kit_install", params) }
        .map(Response::wrap)
        .map { filterMapper.fromMap(it.data.castToMap()) }

    override fun upgradeData(): Maybe<String> = Maybe
        .fromCallable { emptyMap<String, Any?>() }
        .flatMap { params -> functionsHelper.exec<Any?>("user_data_upgrade", params) }
        .map(Response::wrap)
        .map { it.data?.toString() }

    override fun getSnippetDetails(snippet: Snippet): Maybe<SnippetDetails> = Maybe
        .fromCallable {
            mapOf(
                "userId" to snippet.userId,
                "filterId" to snippet.filterId,
                "snippetId" to snippet.id
            )
        }
        .flatMap { payload -> functionsHelper.exec<Any?>("snippet_get", payload) }
        .map(Response::wrap)
        .map {
            val data = it.data.castToMap()
            val filesMaps = data?.get("files").castToListOfMaps()
            val files = filesMaps.mapNotNull { map -> fileMapper.fromMap(map) }
            files.forEach { file ->
                file.updateDate = file.updateDate ?: file.createDate
                file.objectType = ObjectType.READONLY
                file.uploadUrl = file.downloadUrl
                file.downloadUrl = null
            }
            SnippetDetails(
                snippetId = snippet.id,
                files = files
            )
        }
}