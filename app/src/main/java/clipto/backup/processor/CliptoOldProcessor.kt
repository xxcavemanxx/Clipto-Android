package clipto.backup.processor

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64InputStream
import clipto.analytics.Analytics
import clipto.backup.BackupProcessor
import clipto.backup.IBackupProcessor
import clipto.dao.objectbox.model.ClipBox
import clipto.domain.TextType
import dagger.hilt.android.scopes.ViewModelScoped
import java.io.DataInputStream
import java.io.FileInputStream
import java.util.*
import javax.inject.Inject

@ViewModelScoped
class CliptoOldProcessor @Inject constructor() : BackupProcessor() {

    override fun restore(contentResolver: ContentResolver, uri: Uri): IBackupProcessor.BackupStats {
        var count = 0
        contentResolver.openFileDescriptor(uri, "r")?.use {
            DataInputStream(Base64InputStream(FileInputStream(it.fileDescriptor), 0)).use { stream ->
                txHelper.inTx {
                    val date = Date()
                    val allClips = clipBoxDao.getAllClips()
                    count = stream.readInt()
                    val iterations = count
                    (0 until iterations).forEach { _ ->
                        val clip = read(ClipBox(), stream)
                        if (!restore(allClips, clip, date)) {
                            count--
                        }
                    }
                }
            }
            if (count > 0) {
                Analytics.onRestoreFromClipto()
            }
        }
        return IBackupProcessor.BackupStats(notes = count)
    }

    private fun read(data: ClipBox, from: DataInputStream): ClipBox {
        val createDate = from.readLong()
        if (createDate != 0L) {
            data.createDate = Date(createDate)
        }
        val updateDate = from.readLong()
        if (updateDate != 0L) {
            data.updateDate = Date(updateDate)
        }
        val modifyDate = from.readLong()
        if (modifyDate != 0L) {
            data.modifyDate = Date(modifyDate)
        }
        data.textType = TextType.byId(from.readInt())
        data.usageCount = from.readInt()
        val hasTitleAndTags = from.read()
        if (hasTitleAndTags == 1) {
            val size = from.readInt()
            val bytes = ByteArray(size)
            from.readFully(bytes)
            val titleAndTags = String(bytes, Charsets.UTF_8).split('\n')
            if (titleAndTags.size > 1) {
                data.title = titleAndTags[0].takeIf { it.isNotBlank() }
                data.tagIds = titleAndTags[1].takeIf { it.isNotBlank() }?.let { filterBoxDao.getOrSaveByName(it) }?.let { listOf(it.uid!!) } ?: emptyList()
            } else {
                data.tagIds = titleAndTags[0].takeIf { it.isNotBlank() }?.let { filterBoxDao.getOrSaveByName(it) }?.let { listOf(it.uid!!) } ?: emptyList()
            }
        }
        val hasText = from.read()
        if (hasText == 1) {
            val size = from.readInt()
            val bytes = ByteArray(size)
            from.readFully(bytes)
            data.text = String(bytes, Charsets.UTF_8)
        }
        return data
    }

}