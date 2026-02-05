package clipto.dao.firebase

import android.app.Application
import clipto.analytics.Analytics
import clipto.common.presentation.mvvm.model.DataLoadingState
import clipto.config.IAppConfig
import clipto.dao.firebase.model.UserCollection
import clipto.extensions.log
import clipto.store.app.AppState
import clipto.store.internet.InternetState
import clipto.store.user.UserState
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseDaoHelper @Inject constructor(
    val app: Application,
    private val appState: AppState,
    private val userState: UserState,
    private val appConfig: IAppConfig,
    private val internetState: InternetState
) {

    private val userCollections = mutableMapOf<String, UserCollection>()
    private val firebaseStorage by lazy { FirebaseStorage.getInstance() }
    private var firebaseFirestore: FirebaseFirestore? = null

    fun getFirestore() = firebaseFirestore

    fun <T> splitBatches(array: List<T>, callback: (items: List<T>) -> Unit) {
        val batchSize = appConfig.firestoreBatchSize()
        if (array.size <= batchSize) {
            callback.invoke(array)
        } else {
            var end = 0
            val delay = appConfig.firestoreBatchDelay()
            while (end < array.size) {
                val start = end
                if (start > 0 && delay > 0) {
                    runCatching { TimeUnit.MILLISECONDS.sleep(delay) }
                }
                end = minOf(start + batchSize, array.size)
                callback.invoke(array.subList(start, end))
            }
        }
    }

    fun getAuthUserCollection(): UserCollection? {
        return userState.getUserId()?.let { getUserCollection(it) }
    }

    fun getUserCollection(firebaseId: String): UserCollection {
        return userCollections.getOrPut(firebaseId) {
            val firebaseFirestoreRef = firebaseFirestore ?: FirebaseFirestore.getInstance()
            firebaseFirestore = firebaseFirestoreRef
            UserCollection(
                firebaseId = firebaseId,
                firebaseFirestore = firebaseFirestoreRef,
                firebaseStorage = firebaseStorage
            )
        }
    }

    fun waitForPendingWrites(callback: () -> Unit) {
        val fireStore = getFirestore()
        if (fireStore == null) {
            callback.invoke()
        } else {
            try {
                fireStore.waitForPendingWrites()
                    .addOnCompleteListener { callback.invoke() }
            } catch (e: Exception) {
                callback.invoke()
            }
        }
    }

    fun terminate() {
        val firebaseFirestoreRef = firebaseFirestore
        if (firebaseFirestoreRef != null) {
            runCatching { Tasks.await(firebaseFirestoreRef.terminate()) }
            runCatching { Tasks.await(firebaseFirestoreRef.clearPersistence()) }
            firebaseFirestore = null
        }
        userCollections.clear()
    }

    fun getNewDateFromChanges(changes: List<DocumentChange>): Date? {
        return runCatching { changes.mapNotNull { change -> change.document.getDate(ATTR_CHANGE_TIMESTAMP) }.maxOrNull() }.getOrNull()
    }

    fun getNewDateFromSnapshots(changes: List<DocumentSnapshot>): Date? {
        return runCatching { changes.mapNotNull { change -> change.getDate(ATTR_CHANGE_TIMESTAMP) }.maxOrNull() }.getOrNull()
    }

    fun createListener(
        id: String,
        query: Query,
        forceSync: Boolean,
        initialCallback: (snapshot: QuerySnapshot) -> Unit,
        snapshotCallback: (snapshot: QuerySnapshot) -> Unit
    ): ListenerRegistration {
        val initialSnapshot = AtomicBoolean(true)
        val metadataChanges = MetadataChanges.INCLUDE
        log("FIREBASE LISTENER :: createListener: ref: {}, {}", id, metadataChanges)
        return query.addSnapshotListener(metadataChanges) { querySnapshot, exception ->
            exception?.also {
                if (forceSync) {
                    appState.setLoadingState(FirebaseException("create_listener_${id}", it))
                } else if (internetState.isConnected()) {
                    appState.setLoadingState(
                        DataLoadingState.Error(
                            code = it.code.name,
                            message = it.message,
                            throwable = it
                        )
                    )
                    Analytics.onError("error_listen_${id}", it)
                }
            }
            querySnapshot?.also {
                try {
                    val metadata = it.metadata
                    val changes = it.documentChanges
                    val initial = initialSnapshot.compareAndSet(true, false)
                    if (forceSync && initial) {
                        appState.onBackground {
                            log(
                                "FIREBASE LISTENER :: createListener :: initialCallback :: {}, changes={}, size={}",
                                id,
                                changes.size,
                                it.size()
                            )
                            try {
                                initialCallback.invoke(querySnapshot)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } else if (!initial) {
                        appState.onBackground {
                            log(
                                "FIREBASE LISTENER :: createListener :: snapshotCallback :: fromCache={}, id={}, changes={}, size={}",
                                metadata.isFromCache,
                                id,
                                changes.size,
                                it.size()
                            )
                            snapshotCallback.invoke(querySnapshot)
                        }
                    } else if (!metadata.isFromCache) {
                        appState.onBackground {
                            log(
                                "FIREBASE LISTENER :: createListener :: snapshot :: fromCache={}, id={}, changes={}, size={}",
                                metadata.isFromCache,
                                id,
                                changes.size,
                                it.size()
                            )
                            snapshotCallback.invoke(querySnapshot)
                        }
                    }
                } catch (e: Exception) {
                    Analytics.onError("error_createListener", e)
                }
            }
        }
    }

    companion object {
        fun getFieldValueArrayRemove(elements: List<String>) = FieldValue.arrayRemove(*elements.toTypedArray())

        fun getServerTimestamp() = FieldValue.serverTimestamp()

        fun getFieldValueDelete() = FieldValue.delete()

        fun normalizeMap(map: MutableMap<String, Any?>, deleteFromCloud: Boolean): MutableMap<String, Any?> {
            val iterator = map.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val value = entry.value
                val isNull =
                    when {
                        value == null -> true
                        value is Boolean && !value -> true
                        value is List<*> && value.isEmpty() -> true
                        else -> false
                    }
                if (isNull) {
                    if (deleteFromCloud) {
                        map[entry.key] = getFieldValueDelete()
                    } else {
                        iterator.remove()
                    }
                }
            }
            return map
        }

        const val ATTR_DEVICE_ID = "iid"
        const val ATTR_API_VERSION = "av"
        const val ATTR_CHANGE_TIMESTAMP = "cts"

        const val COLLECTION_USERS = "u"
        const val COLLECTION_USER_CLIPS = "c"
        const val COLLECTION_USER_CLIPS_DELETED = "cd"
        const val COLLECTION_USER_FILTERS = "f"
        const val COLLECTION_USER_FILES = "fs"
        const val COLLECTION_USER_FILES_DELETED = "fsd"
        const val COLLECTION_PUBLIC_NOTES = "n"

        const val ATTR_CLIP_USAGE_COUNT = "n"
        const val ATTR_CLIP_CREATE_DATE = "c"
        const val ATTR_CLIP_UPDATE_DATE = "u"
        const val ATTR_CLIP_MODIFY_DATE = "m"
        const val ATTR_CLIP_DELETE_DATE = "d"
        const val ATTR_CLIP_OBJECT_TYPE = "ot"
        const val ATTR_CLIP_TEXT_TYPE = "v"
        const val ATTR_CLIP_TITLE = "h"
        const val ATTR_CLIP_TEXT = "t"
        const val ATTR_CLIP_TAG_IDS = "li"
        const val ATTR_CLIP_FILE_IDS = "fi"
        const val ATTR_CLIP_TRACKED = "a"
        const val ATTR_CLIP_FAV = "f"
        const val ATTR_CLIP_SNIPPET_ID = "s"
        const val ATTR_CLIP_FOLDER_ID = "fid"
        const val ATTR_CLIP_PUBLIC_LINK = "pl"
        const val ATTR_CLIP_ABBREVIATION = "abr"
        const val ATTR_CLIP_DESCRIPTION = "dsc"
        const val ATTR_CLIP_SNIPPET_SETS_IDS = "si"

        @Deprecated("to be removed")
        const val ATTR_CLIP_FILE_METADATA = "fm"

        @Deprecated("to be removed")
        const val ATTR_CLIP_TAGS = "l"

        const val ATTR_FILTER_NAME = "name"
        const val ATTR_FILTER_DESCRIPTION = "description"
        const val ATTR_FILTER_OBJECT_TYPE = "obj_type"
        const val ATTR_FILTER_TYPE = "type"
        const val ATTR_FILTER_LIMIT = "limit"
        const val ATTR_FILTER_COLOR = "color"
        const val ATTR_FILTER_SORT_BY = "sortBy"
        const val ATTR_FILTER_LIST_STYLE = "listStyle"
        const val ATTR_FILTER_AUTO_RULE_ENABLED = "autoRuleEnabled"
        const val ATTR_FILTER_AUTO_RULE_TEXT = "autoRuleText"
        const val ATTR_FILTER_EXCLUDE_WITH_CUSTOM_ATTRS = "excludeCustomAttrs"
        const val ATTR_FILTER_TAG_IDS = "tagIds"
        const val ATTR_FILTER_TAG_IDS_WHERE_TYPE = "tagIdsWhereType"
        const val ATTR_FILTER_SNIPPET_SETS_IDS = "snippetSetsIds"
        const val ATTR_FILTER_SNIPPET_SETS_WHERE_TYPE = "snippetSetsWhereType"
        const val ATTR_FILTER_LOCATED_IN_WHERE_TYPE = "locatedInWhereType"
        const val ATTR_FILTER_SHOW_ONLY_WITH_PUBLIC_LINKS = "showOnlyWithPublicLink"
        const val ATTR_FILTER_SHOW_ONLY_WITH_ATTACHMENTS = "showOnlyWithAttachments"
        const val ATTR_FILTER_SHOW_ONLY_NOT_SYNCED = "showOnlyNotSynced"
        const val ATTR_FILTER_TEXT_TYPE_IN = "textTypeIn"
        const val ATTR_FILTER_CREATE_DATE_FROM = "createDateFrom"
        const val ATTR_FILTER_CREATE_DATE_TO = "createDateTo"
        const val ATTR_FILTER_CREATE_DATE_PERIOD = "createDatePeriod"
        const val ATTR_FILTER_UPDATE_DATE_FROM = "updateDateFrom"
        const val ATTR_FILTER_UPDATE_DATE_TO = "updateDateTo"
        const val ATTR_FILTER_UPDATE_DATE_PERIOD = "updateDatePeriod"
        const val ATTR_FILTER_TEXT_LIKE = "textLike"
        const val ATTR_FILTER_STARRED = "starred"
        const val ATTR_FILTER_UNTAGGED = "untagged"
        const val ATTR_FILTER_CLIPBOARD = "clipboard"
        const val ATTR_FILTER_RECYCLED = "recycled"
        const val ATTR_FILTER_UPDATED = "updated"
        const val ATTR_FILTER_DELETED = "deleted"
        const val ATTR_FILTER_SNIPPETS = "snippets"
        const val ATTR_FILTER_PIN_STARRED_ENABLED = "pinStarred"
        const val ATTR_FILTER_HIDE_HINT = "hideHint"
        const val ATTR_FILTER_SNIPPET_KIT = "snippetKit"

        const val ATTR_SNIPPET_KIT_ID = "id"
        const val ATTR_SNIPPET_KIT_NAME = "name"
        const val ATTR_SNIPPET_KIT_HASH = "hash"
        const val ATTR_SNIPPET_KIT_CREATED = "created"
        const val ATTR_SNIPPET_KIT_UPDATED = "updated"
        const val ATTR_SNIPPET_KIT_INSTALLS = "installs"
        const val ATTR_SNIPPET_KIT_USER_ID = "userId"
        const val ATTR_SNIPPET_KIT_USER_NAME = "userName"
        const val ATTR_SNIPPET_KIT_COLOR = "color"
        const val ATTR_SNIPPET_KIT_COUNTRY = "country"
        const val ATTR_SNIPPET_KIT_LANGUAGE = "language"
        const val ATTR_SNIPPET_KIT_FILTER_ID = "filterId"
        const val ATTR_SNIPPET_KIT_SNIPPETS_COUNT = "snippetsCount"
        const val ATTR_SNIPPET_KIT_SNIPPETS = "snippets"
        const val ATTR_SNIPPET_KIT_PUBLIC_LINK = "publicLink"
        const val ATTR_SNIPPET_KIT_DESCRIPTION = "description"
        const val ATTR_SNIPPET_KIT_PUBLIC_STATUS = "publicStatus"
        const val ATTR_SNIPPET_KIT_UPDATE_REASON = "updateReason"
        const val ATTR_SNIPPET_KIT_CATEGORY_ID = "categoryId"
        const val ATTR_SNIPPET_KIT_SHARABLE = "sharable"
        const val ATTR_SNIPPET_KIT_SORT_BY = "sortBy"
        const val ATTR_SNIPPET_KIT_LIST_STYLE = "listStyle"

        const val ATTR_SNIPPET_ID = "id"
        const val ATTR_SNIPPET_TEXT = "text"
        const val ATTR_SNIPPET_TITLE = "title"
        const val ATTR_SNIPPET_CREATED = "created"
        const val ATTR_SNIPPET_UPDATED = "updated"
        const val ATTR_SNIPPET_FILE_IDS = "fileIds"
        const val ATTR_SNIPPET_ABBREVIATION = "abbreviation"
        const val ATTR_SNIPPET_DESCRIPTION = "description"
        const val ATTR_SNIPPET_TEXT_TYPE = "textType"

        const val ATTR_PUBLIC_NOTE_POSTPONE_IN_MILLIS = "postponeInMillis"
        const val ATTR_PUBLIC_NOTE_POSTPONE_AT_DATE = "postponeAtDate"
        const val ATTR_PUBLIC_NOTE_EXPIRES_IN_MILLIS = "expiresInMillis"
        const val ATTR_PUBLIC_NOTE_EXPIRES_AT_DATE = "expiresAtDate"
        const val ATTR_PUBLIC_NOTE_LINK = "link"
        const val ATTR_PUBLIC_NOTE_LOCKED = "locked"
        const val ATTR_PUBLIC_NOTE_UNAVAILABLE = "unavailable"
        const val ATTR_PUBLIC_NOTE_PASSWORD_CLUE = "passwordClue"
        const val ATTR_PUBLIC_NOTE_OPENED_TIMES = "openedTimes"
        const val ATTR_PUBLIC_NOTE_ONETIME = "oneTimeOpening"

        const val ATTR_PUBLIC_NOTE_PASSWORD = "password"
        const val ATTR_PUBLIC_NOTE_TITLE = "title"
        const val ATTR_PUBLIC_NOTE_ID = "noteId"

        const val ATTR_FILE_MD5 = "md5"
        const val ATTR_FILE_SIZE = "size"
        const val ATTR_FILE_TYPE = "type"
        const val ATTR_FILE_NAME = "name"
        const val ATTR_FILE_PATH = "path"
        const val ATTR_FILE_FOLDER = "folder"
        const val ATTR_FILE_DESCRIPTION = "description"
        const val ATTR_FILE_ABBREVIATION = "abbreviation"
        const val ATTR_FILE_TAG_IDS = "tagIds"
        const val ATTR_FILE_KIT_IDS = "kitIds"
        const val ATTR_FILE_CREATED = "created"
        const val ATTR_FILE_UPDATED = "updated"
        const val ATTR_FILE_DELETED = "deleted"
        const val ATTR_FILE_MODIFIED = "modified"
        const val ATTR_FILE_MEDIA_TYPE = "mediaType"
        const val ATTR_FILE_UPLOADED = "uploaded"
        const val ATTR_FILE_UPLOAD_URL = "uploadUrl"
        const val ATTR_FILE_DOWNLOAD_URL = "downloadUrl"
        const val ATTR_FILE_PLATFORM = "platform"
        const val ATTR_FILE_ERROR = "error"
        const val ATTR_FILE_ID = "firestoreId"
        const val ATTR_FILE_FOLDER_ID = "folderId"
        const val ATTR_FILE_COLOR = "color"
        const val ATTR_FILE_FAV = "fav"

        const val ATTR_FILE_CREATE_DATE = "createDate"
        const val ATTR_FILE_UPDATE_DATE = "updateDate"
        const val ATTR_FILE_MODIFY_DATE = "modifyDate"
        const val ATTR_FILE_DELETE_DATE = "deleteDate"
    }

}