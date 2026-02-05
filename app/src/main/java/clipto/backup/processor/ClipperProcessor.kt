package clipto.backup.processor

import android.content.ContentResolver
import android.net.Uri
import clipto.analytics.Analytics
import clipto.backup.BackupProcessor
import clipto.backup.IBackupProcessor
import clipto.common.misc.GsonUtils
import clipto.dao.objectbox.model.ClipBox
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.scopes.ViewModelScoped
import java.io.FileInputStream
import java.util.*
import javax.inject.Inject

@ViewModelScoped
class ClipperProcessor @Inject constructor() : BackupProcessor() {

    override fun restore(contentResolver: ContentResolver, uri: Uri): IBackupProcessor.BackupStats {
        var count = 0
        contentResolver.openFileDescriptor(uri, "r")?.use {
            FileInputStream(it.fileDescriptor).use { stream ->
                val reader = stream.reader()
                val clipperJson = GsonUtils.get().fromJson(reader, ClipperBackup::class.java)
                if (clipperJson.list.isNullOrEmpty()) {
                    return IBackupProcessor.BackupStats()
                }
                val transactionDate = Date()
                txHelper.inTx {
                    val allClips = clipBoxDao.getAllClips()
                    clipperJson.list.forEach { clips ->
                        val isFav = clips.name == "Favorites"
                        val clipsTag = clips.name.takeIf { it.length <= appConfig.maxLengthTag() }?.let { listOf(it) }
                            ?: emptyList()
                        clips.clips.forEach { clipperClip ->
                            val clip = ClipBox().apply {
                                textType = clipBoxDao.defineClipType(clipperClip.contents)
                                createDate = Date(clipperClip.timestamp * 1000)
                                modifyDate = createDate
                                updateDate = createDate
                                if (isFav) {
                                    fav = isFav
                                } else {
                                    tagIds = clipsTag.mapNotNull { filterBoxDao.getOrSaveByName(it)?.uid }
                                }
                                if (clipperClip.pinned) {
                                    fav = true
                                }
                                text = clipperClip.contents
                                title = clipperClip.title
                                    .takeIf { it != clipperClip.contents }
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
                    Analytics.onRestoreFromClipper()
                }
            }
        }
        return IBackupProcessor.BackupStats(notes = count)
    }

    private data class ClipperBackup(
        @SerializedName("lists") val list: List<ClipperList>
    )

    private data class ClipperList(
        @SerializedName("name") val name: String,
        @SerializedName("clippings") val clips: List<ClipperItem>
    )

    private data class ClipperItem(
        @SerializedName("title") val title: String,
        @SerializedName("timestamp") val timestamp: Long,
        @SerializedName("contents") val contents: String,
        @SerializedName("pinned") val pinned: Boolean = false
    )

}