package clipto.backup

import android.app.Application
import clipto.common.misc.GsonUtils
import clipto.config.IAppConfig
import clipto.dao.TxHelper
import clipto.dao.objectbox.ClipBoxDao
import clipto.dao.objectbox.FilterBoxDao
import clipto.dao.objectbox.SettingsBoxDao
import clipto.dao.objectbox.model.ClipBox
import clipto.domain.getTagIds
import clipto.store.app.AppState
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.util.*
import javax.inject.Inject

abstract class BackupProcessor : IBackupProcessor {

    @Inject
    lateinit var settingsBoxDao: SettingsBoxDao

    @Inject
    lateinit var filterBoxDao: FilterBoxDao

    @Inject
    lateinit var clipBoxDao: ClipBoxDao

    @Inject
    lateinit var appConfig: IAppConfig

    @Inject
    lateinit var appState: AppState

    @Inject
    lateinit var txHelper: TxHelper

    @Inject
    lateinit var app: Application

    fun restore(clips: List<ClipBox>, clip: ClipBox, transactionDate: Date): Boolean {
        return clip.text?.takeIf { it.isNotBlank() }?.let { text ->
            val prevClip = clips.find { (clip.firestoreId != null && clip.firestoreId == it.firestoreId) || it.text == text }
            if (clip.createDate == null) {
                clip.createDate = Date()
            }
            clip.modifyDate = clip.createDate
            if (prevClip != null) {
                val newTagIds = linkedSetOf<String>()
                newTagIds.addAll(prevClip.getTagIds())
                newTagIds.addAll(clip.getTagIds())

                val newSnippetSetIds = linkedSetOf<String>()
                newSnippetSetIds.addAll(prevClip.snippetSetsIds)
                newSnippetSetIds.addAll(clip.snippetSetsIds)

                val newClip = ClipBox().apply(prevClip)
                newClip.snippetSetsIds = newSnippetSetIds.toList()
                newClip.tagIds = newTagIds.toList()
                newClip.modifyDate = transactionDate
                newClip.title = newClip.title ?: clip.title
                newClip.fav = newClip.fav || clip.fav
                clipBoxDao.save(newClip)
                filterBoxDao.update(prevClip, newClip)
            } else {
                clipBoxDao.save(clip)
                filterBoxDao.update(null, clip)
            }
            true
        } ?: false
    }

    class SafeDateAdapter : TypeAdapter<Date?>() {

        private val date = Date()

        override fun read(`in`: JsonReader): Date? {
            if (`in`.peek() === JsonToken.NULL) {
                `in`.nextNull()
                return null
            }
            try {
                return GsonUtils.parseDate(`in`.nextString())
            } catch (th: Throwable) {
                return date
            }
        }

        override fun write(out: JsonWriter, value: Date?) {
            if (value == null) {
                out.nullValue()
                return
            }
            out.value(GsonUtils.formatDate(value))
        }
    }

}