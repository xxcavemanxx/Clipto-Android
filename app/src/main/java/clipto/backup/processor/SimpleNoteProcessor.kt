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
import javax.inject.Inject

@ViewModelScoped
class SimpleNoteProcessor @Inject constructor() : BackupProcessor() {

    override fun restore(contentResolver: ContentResolver, uri: Uri): IBackupProcessor.BackupStats {
        var count = 0
        contentResolver.openFileDescriptor(uri, "r")?.use {
            FileInputStream(it.fileDescriptor).use { stream ->
                val reader = stream.reader()
                val backupJson = GsonUtils.get().fromJson(reader, SimplenoteBackup::class.java)
                if (backupJson.notes.isNullOrEmpty()) {
                    return IBackupProcessor.BackupStats()
                }
                val transactionDate = Date()
                txHelper.inTx {
                    val allClips = clipBoxDao.getAllClips()
                    backupJson.notes.filter { !it.content.isNullOrBlank() }.forEach { note ->
                        var noteTitle: String? = null
                        var noteContent: String? = note.content
                        val firstLineIndex: Int = noteContent?.indexOf('\n') ?: -1
                        if (firstLineIndex != -1) {
                            noteTitle = noteContent!!.substring(0, firstLineIndex)
                            noteContent = noteContent.substring(firstLineIndex)
                            val secondLineIndex: Int = noteContent.indexOf('\n')
                            if (secondLineIndex != 0 && !noteContent.startsWith(noteTitle)) {
                                noteContent = note.content
                                noteTitle = null
                            }
                        }
                        val clip = ClipBox().apply {
                            tagIds = note.tags?.mapNotNull { filterBoxDao.getOrSaveByName(it)?.uid } ?: emptyList()
                            textType = clipBoxDao.defineClipType(noteContent)
                            updateDate = note.lastModified ?: createDate
                            createDate = note.creationDate
                            modifyDate = updateDate
                            text = noteContent?.trimStart()
                            title = noteTitle
                            usageCount = 0
                        }
                        if (restore(allClips, clip, transactionDate)) {
                            count++
                        }
                    }
                }
                if (count > 0) {
                    Analytics.onRestoreFromSimplenote()
                }
            }
        }
        return IBackupProcessor.BackupStats(notes = count)
    }

    private data class SimplenoteBackup(
            @SerializedName("activeNotes") val notes: List<SimplenoteItem>
    )

    private data class SimplenoteItem(
            @SerializedName("content") val content: String?,
            @JsonAdapter(SafeDateAdapter::class)
            @SerializedName("creationDate") val creationDate: Date,
            @JsonAdapter(SafeDateAdapter::class)
            @SerializedName("lastModified") val lastModified: Date? = null,
            @SerializedName("tags") val tags: List<String>? = null
    )
}