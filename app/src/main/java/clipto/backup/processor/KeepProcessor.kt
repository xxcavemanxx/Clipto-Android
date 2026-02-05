package clipto.backup.processor

import android.content.ContentResolver
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import clipto.action.SaveClipAction
import clipto.backup.BackupProcessor
import clipto.backup.IBackupProcessor
import clipto.common.extensions.toNullIfEmpty
import clipto.common.logging.L
import clipto.common.misc.FileExtraUtils
import clipto.common.misc.GsonUtils
import clipto.dao.objectbox.model.ClipBox
import clipto.store.user.UserState
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.scopes.ViewModelScoped
import org.greenrobot.essentials.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import java.util.zip.ZipInputStream
import javax.inject.Inject

@ViewModelScoped
class KeepProcessor @Inject constructor(
    private val userState: UserState,
    private val saveClipAction: SaveClipAction,
) : BackupProcessor() {

    override fun restore(contentResolver: ContentResolver, uri: Uri): IBackupProcessor.BackupStats {
        var count = 0
        val dirDownloads = app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val outputDirectory = File(dirDownloads, "takeout_extracted")
        FileExtraUtils.forceMkdir(outputDirectory)
        val keepFiles = mutableListOf<String>()
        contentResolver.openFileDescriptor(uri, "r")?.use {
            ZipInputStream(FileInputStream(it.fileDescriptor)).use { zipStream ->
                var entry = zipStream.nextEntry
                while (entry != null) {
                    val entryFile = File(entry.name)
                    L.log(this, "keep extract: {} -> {}", entry.name, entryFile.parent)
                    extractFileFromArchive(outputDirectory, zipStream, entryFile.name)
                    keepFiles.add(entryFile.name)
                    entry = zipStream.nextEntry
                }
            }
        }
        keepFiles.filter { it.endsWith(".json") }.let { notes ->
            notes.forEach {
                runCatching {
                    val file = File(outputDirectory, it)
                    val fileContent = FileUtils.readUtf8(file)
                    val note = GsonUtils.toObjectSilent(fileContent, KeepItem::class.java)
                    if (note != null) {
                        val attachments = note.attachments ?: emptyList()
                        if (attachments.isEmpty() || userState.isAuthorized()) {
                            val date = Date(note.userEditedTimestampUsec / 1000)
                            val clip = ClipBox().apply {
                                tagIds = note.labels?.mapNotNull { filterBoxDao.getOrSaveByName(it.name)?.uid } ?: emptyList()
                                text = note.textContent.toNullIfEmpty()
                                title = note.title.toNullIfEmpty()
                                fav = note.isPinned
                                createDate = date
                                modifyDate = date
                                updateDate = date
                                if (note.isTrashed) {
                                    deleteDate = date
                                }
                            }
                            val files = attachments
                                .mapNotNull {
                                    val localFile = File(outputDirectory, it.filePath)
                                    if (localFile.exists()) {
                                        localFile
                                    } else {
                                        val indexOfDot = it.filePath.lastIndexOf(".")
                                        if (indexOfDot != -1) {
                                            val nameWithoutExt = it.filePath.substring(0, indexOfDot)
                                            keepFiles.find { file -> file.startsWith(nameWithoutExt) }
                                                ?.let { file -> File(outputDirectory, file) }
                                                ?.takeIf { file -> file.exists() }
                                        } else {
                                            null
                                        }
                                    }
                                }
                                .map { it.toUri() }
                            val clipText = clip.text
                            if (!clipText.isNullOrBlank() && clipBoxDao.getClipByText(clipText) != null) {
                                L.log(this, "ignore clip due to found same text: {}", clipText)
                            } else if (files.isNotEmpty() || !clip.text.isNullOrBlank()) {
                                appState.setLoadingState()
                                saveClipAction.execute(
                                    clip = clip,
                                    files = files,
                                    withLoadingState = false,
                                    withDisposeRunning = false,
                                    withSilentValidation = true,
                                )
                                count++
                            }
                        }
                    }
                }
            }
            appState.setLoadedState()
        }
        return IBackupProcessor.BackupStats(notes = count)
    }

    private fun extractFileFromArchive(outputDirectory: File, stream: ZipInputStream, outputName: String) {
        val path = File(outputDirectory, outputName).absolutePath
        try {
            FileOutputStream(path).use { output ->
                val buffer = ByteArray(2048)
                var len: Int
                while (stream.read(buffer).also { len = it } > 0) {
                    output.write(buffer, 0, len)
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    private data class KeepItem(
        @SerializedName("title") val title: String?,
        @SerializedName("isPinned") val isPinned: Boolean,
        @SerializedName("isTrashed") val isTrashed: Boolean,
        @SerializedName("textContent") val textContent: String?,
        @SerializedName("userEditedTimestampUsec") val userEditedTimestampUsec: Long,
        @SerializedName("attachments") val attachments: List<KeepAttachment>?,
        @SerializedName("labels") val labels: List<KeepLabel>?
    )

    private data class KeepLabel(
        @SerializedName("name") val name: String
    )

    private data class KeepAttachment(
        @SerializedName("filePath") val filePath: String,
        @SerializedName("mimetype") val mimetype: String?
    )

}