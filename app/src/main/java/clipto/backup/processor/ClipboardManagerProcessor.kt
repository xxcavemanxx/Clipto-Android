package clipto.backup.processor

import android.content.ContentResolver
import android.net.Uri
import clipto.analytics.Analytics
import clipto.backup.BackupProcessor
import clipto.backup.IBackupProcessor
import clipto.common.misc.GsonUtils
import clipto.dao.objectbox.model.ClipBox
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.scopes.ViewModelScoped
import java.io.FileInputStream
import java.util.*
import java.util.zip.GZIPInputStream
import javax.inject.Inject

@ViewModelScoped
class ClipboardManagerProcessor @Inject constructor() : BackupProcessor() {

    override fun restore(contentResolver: ContentResolver, uri: Uri): IBackupProcessor.BackupStats {
        var count = 0
        contentResolver.openFileDescriptor(uri, "r")?.use {
            GZIPInputStream(FileInputStream(it.fileDescriptor)).use { stream ->
                val reader = stream.reader()
                val json = GsonUtils.get().fromJson(reader, ClipboardManagerBackup::class.java)
                if (json.notes.isNullOrEmpty()) {
                    return IBackupProcessor.BackupStats()
                }
                val transactionDate = Date()
                txHelper.inTx {
                    val allClips = clipBoxDao.getAllClips()
                    json.notes.forEach { note ->
                        val tagName = json.categories.find { it.id == note.categoryId }?.name
                        val tagFilter = filterBoxDao.getOrSaveByName(tagName)
                        val noteText = note.text ?: note.title
                        if (noteText != null) {
                            val clip = ClipBox().apply {
                                tagIds = tagFilter?.uid?.let { listOf(it) } ?: emptyList()
                                textType = clipBoxDao.defineClipType(noteText)
                                createDate = note.creationDate
                                modifyDate = note.creationDate
                                updateDate = createDate
                                fav = note.isFavorite == 1
                                text = noteText
                                title = note.title
                                        ?.let {
                                            if (it.length > appConfig.maxLengthTitle()) {
                                                it.substring(0, appConfig.maxLengthTitle())
                                            } else {
                                                it
                                            }
                                        }
                                usageCount = 0
                            }
                            if (restore(allClips, clip, transactionDate)) {
                                count++
                            }
                        }
                    }
                }
                if (count > 0) {
                    Analytics.onRestoreFromClipboardManager()
                }
            }
        }
        return IBackupProcessor.BackupStats(notes = count)
    }

    private data class ClipboardManagerBackup(
            @SerializedName("category") val categories: List<ClipboardManagerCategory>,
            @SerializedName("note") val notes: List<ClipboardManagerNote>
    )

    private data class ClipboardManagerCategory(
            @SerializedName("_id") val id: Long,
            @SerializedName("name") val name: String
    )

    private data class ClipboardManagerNote(
            @SerializedName("category_id") val categoryId: Long = 0L,
            @SerializedName("title") val title: String? = null,
            @SerializedName("body") val text: String? = null,
            @SerializedName("is_favorite") val isFavorite: Int = 0,
            @JsonAdapter(SafeDateAdapter::class)
            @SerializedName("created_datetime") val creationDate: Date? = null
    )
}