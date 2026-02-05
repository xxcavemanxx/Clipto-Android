package clipto.backup.processor

import android.content.ContentResolver
import android.net.Uri
import clipto.analytics.Analytics
import clipto.backup.BackupProcessor
import clipto.backup.IBackupProcessor
import clipto.dao.objectbox.model.ClipBox
import dagger.hilt.android.scopes.ViewModelScoped
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@ViewModelScoped
class ClipStackProcessor @Inject constructor() : BackupProcessor() {

    override fun restore(contentResolver: ContentResolver, uri: Uri): IBackupProcessor.BackupStats {
        var count = 0
        val dateFormatter = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH)
        contentResolver.openFileDescriptor(uri, "r")?.use {
            (FileInputStream(it.fileDescriptor)).use { stream ->
                val transactionDate = Date()
                txHelper.inTx {
                    val allClips = clipBoxDao.getAllClips()
                    val clipLines = mutableListOf<String>()
                    var clipCreateDate: Date? = null
                    var isFav = false
                    stream.bufferedReader().forEachLine { line ->
                        val clipCreateDateString: String
                        if (line.endsWith("☆★☆")) {
                            clipCreateDateString = line.substring(0, line.length - 3)
                        } else {
                            clipCreateDateString = line
                        }
                        try {
                            val nextClipCreateDate = dateFormatter.parse(clipCreateDateString.replace("WAT", "GMT"))
                            if (clipLines.isNotEmpty()) {
                                val clip = ClipBox().apply {
                                    this.fav = isFav
                                    this.text = clipLines.joinToString("\n")
                                    this.createDate = clipCreateDate
                                    this.textType = clipBoxDao.defineClipType(this.text)
                                }
                                if (restore(allClips, clip, transactionDate)) {
                                    count++
                                }
                                clipLines.clear()
                            }
                            clipCreateDate = nextClipCreateDate
                            isFav = line.endsWith("☆★☆")
                        } catch (e: Exception) {
                            if (clipCreateDate != null) {
                                clipLines.add(line)
                            }
                        }
                    }
                    if (clipCreateDate != null && clipLines.isNotEmpty()) {
                        val clip = ClipBox().apply {
                            this.fav = isFav
                            this.text = clipLines.joinToString("\n")
                            this.createDate = clipCreateDate
                            this.textType = clipBoxDao.defineClipType(this.text)
                        }
                        if (restore(allClips, clip, transactionDate)) {
                            count++
                        }
                    }
                }
            }
            if (count > 0) {
                Analytics.onRestoreFromClipStack()
            }
        }
        return IBackupProcessor.BackupStats(notes = count)
    }

}