package clipto.dao.firebase.mapper

import clipto.analytics.Analytics
import clipto.common.extensions.castToListOfMaps
import clipto.common.extensions.castToListOfStrings
import clipto.common.extensions.castToMap
import clipto.config.IAppConfig
import clipto.dao.firebase.FirebaseDaoHelper
import clipto.dao.objectbox.model.ClipBox
import clipto.domain.Clip
import clipto.domain.ObjectType
import clipto.domain.TextType
import clipto.store.app.AppState
import com.google.firebase.firestore.DocumentChange
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipMapper @Inject constructor(
    private val appState: AppState,
    private val appConfig: IAppConfig,
) {

    fun toMap(clip: Clip): Map<String, Any?> {
        val map = mutableMapOf(
            FirebaseDaoHelper.ATTR_DEVICE_ID to appState.getInstanceId(),
            FirebaseDaoHelper.ATTR_API_VERSION to appConfig.getApiVersion(),
            FirebaseDaoHelper.ATTR_CHANGE_TIMESTAMP to FirebaseDaoHelper.getServerTimestamp(),
            FirebaseDaoHelper.ATTR_CLIP_USAGE_COUNT to clip.usageCount,
            FirebaseDaoHelper.ATTR_CLIP_OBJECT_TYPE to clip.objectType.id,
            FirebaseDaoHelper.ATTR_CLIP_CREATE_DATE to DateMapper.toTimestamp(clip.createDate),
            FirebaseDaoHelper.ATTR_CLIP_MODIFY_DATE to DateMapper.toTimestamp(clip.modifyDate),
            FirebaseDaoHelper.ATTR_CLIP_UPDATE_DATE to DateMapper.toTimestamp(clip.updateDate),
            FirebaseDaoHelper.ATTR_CLIP_DELETE_DATE to DateMapper.toTimestamp(clip.deleteDate, true),
            FirebaseDaoHelper.ATTR_CLIP_TEXT_TYPE to clip.textType.typeId,
            FirebaseDaoHelper.ATTR_CLIP_TAG_IDS to clip.tagIds,
            FirebaseDaoHelper.ATTR_CLIP_TEXT to clip.text,
            FirebaseDaoHelper.ATTR_CLIP_TITLE to clip.title,
            FirebaseDaoHelper.ATTR_CLIP_SNIPPET_ID to clip.snippetId,
            FirebaseDaoHelper.ATTR_CLIP_FOLDER_ID to clip.folderId,
            FirebaseDaoHelper.ATTR_CLIP_TRACKED to clip.tracked,
            FirebaseDaoHelper.ATTR_CLIP_FAV to clip.fav,
            FirebaseDaoHelper.ATTR_CLIP_PUBLIC_LINK to PublicNoteLinkMapper.toMap(clip.publicLink),
            FirebaseDaoHelper.ATTR_CLIP_ABBREVIATION to clip.abbreviation,
            FirebaseDaoHelper.ATTR_CLIP_DESCRIPTION to clip.description,
            FirebaseDaoHelper.ATTR_CLIP_SNIPPET_SETS_IDS to clip.snippetSetsIds,
            FirebaseDaoHelper.ATTR_CLIP_FILE_IDS to clip.fileIds,
        )
        return FirebaseDaoHelper.normalizeMap(map, deleteFromCloud = true)
    }

    fun fromDocChange(change: DocumentChange): ClipBox? = runCatching {
        ClipBox().also { clip ->
            val from = change.document
            clip.firestoreId = from.id
            clip.usageCount = from.getLong(FirebaseDaoHelper.ATTR_CLIP_USAGE_COUNT)?.toInt() ?: 0
            clip.objectType = ObjectType.byId(from.getLong(FirebaseDaoHelper.ATTR_CLIP_OBJECT_TYPE)?.toInt())
            clip.createDate = from.getDate(FirebaseDaoHelper.ATTR_CLIP_CREATE_DATE)
            clip.modifyDate = from.getDate(FirebaseDaoHelper.ATTR_CLIP_MODIFY_DATE) ?: clip.createDate
            clip.updateDate = from.getDate(FirebaseDaoHelper.ATTR_CLIP_UPDATE_DATE)
            clip.deleteDate = from.getDate(FirebaseDaoHelper.ATTR_CLIP_DELETE_DATE)
            clip.textType = TextType.byId(from.getLong(FirebaseDaoHelper.ATTR_CLIP_TEXT_TYPE)?.toInt())
            clip.tagIds = from.get(FirebaseDaoHelper.ATTR_CLIP_TAG_IDS).castToListOfStrings()
            clip.tags = from.get(FirebaseDaoHelper.ATTR_CLIP_TAGS)?.toString()
            clip.fileIds = from.get(FirebaseDaoHelper.ATTR_CLIP_FILE_IDS).castToListOfStrings()
            clip.snippetSetsIds = from.get(FirebaseDaoHelper.ATTR_CLIP_SNIPPET_SETS_IDS).castToListOfStrings()
            clip.files = FileMetaMapper.fromMap(from.get(FirebaseDaoHelper.ATTR_CLIP_FILE_METADATA).castToListOfMaps())
            clip.abbreviation = from.getString(FirebaseDaoHelper.ATTR_CLIP_ABBREVIATION)
            clip.description = from.getString(FirebaseDaoHelper.ATTR_CLIP_DESCRIPTION)
            clip.title = from.getString(FirebaseDaoHelper.ATTR_CLIP_TITLE)
            clip.text = from.getString(FirebaseDaoHelper.ATTR_CLIP_TEXT)
            clip.snippetId = from.getString(FirebaseDaoHelper.ATTR_CLIP_SNIPPET_ID)
            clip.folderId = from.getString(FirebaseDaoHelper.ATTR_CLIP_FOLDER_ID)
            clip.tracked = from.getBoolean(FirebaseDaoHelper.ATTR_CLIP_TRACKED) ?: false
            clip.fav = from.getBoolean(FirebaseDaoHelper.ATTR_CLIP_FAV) ?: false
            clip.publicLink = PublicNoteLinkMapper.fromMap(from.get(FirebaseDaoHelper.ATTR_CLIP_PUBLIC_LINK).castToMap())
        }
    }.onFailure { Analytics.onError("ClipMapper", it) }.getOrNull()

}