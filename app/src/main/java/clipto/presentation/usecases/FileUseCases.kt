package clipto.presentation.usecases

import android.app.Application
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import clipto.AppContext
import clipto.common.extensions.grantUriPermissions
import clipto.common.extensions.toFile
import clipto.common.extensions.withNewFile
import clipto.common.misc.GsonUtils
import clipto.common.misc.IntentUtils
import clipto.common.presentation.mvvm.RxViewModel
import clipto.domain.*
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.preview.link.LinkPreview
import clipto.repository.IClipRepository
import clipto.repository.IFileRepository
import clipto.store.app.AppState
import clipto.store.clip.ClipState
import clipto.store.files.FilesState
import com.wb.clipboard.R
import dagger.hilt.android.scopes.ActivityRetainedScoped
import java.io.File
import javax.inject.Inject

@ActivityRetainedScoped
class FileUseCases @Inject constructor(
    app: Application,
    private val appState: AppState,
    private val clipState: ClipState,
    private val filesState: FilesState,
    private val dialogState: DialogState,
    private val fileRepository: IFileRepository,
    private val clipRepository: IClipRepository
) : RxViewModel(app) {

    fun onView(fileRef: FileRef, fileList: List<FileRef> = emptyList(), title: CharSequence? = null) {
        fileRepository.getFile(fileRef)
            .onErrorReturn { fileRef }
            .subscribeBy("getFile", appState) {
                log("onView file :: {}", it)
                filesState.setFiles(fileList)
                filesState.setViewState(it, title)
                appState.requestNavigateTo(R.id.action_file)
            }
    }

    fun onSaveAsFile(uri: Uri, fileType: FileType, callback: (file: FileRef) -> Unit = {}) {
        fileRepository
            .upload(uri, fileType)
            .doOnError { dialogState.showError(it) }
            .subscribeBy(null, callback)
    }

    fun onSaveAsNote(uri: Uri, fileType: FileType, callback: (clip: Clip) -> Unit = {}) {
        fileRepository
            .upload(uri, fileType)
            .flatMap { fileRef ->
                val clip = clipState.getDefaultNewClip()
                clip.description = fileRef.toString(app)
                clip.fileIds = listOf(fileRef.getUid()!!)
                clipRepository.save(clip, false)
            }
            .doOnError { dialogState.showError(it) }
            .subscribeBy(null, callback)
    }

    fun onPreview(file: FileRef, preview: LinkPreview?) {
        if (preview != null) {
            AppContext.get().onShowPreview(preview)
        } else {
            log("onPreview :: file state={}", file.getState())
            if (file.getState() == FileState.Download) {
                fileRepository.cancelDownloadProgress(file)
                    .subscribeBy("cancelDownloadProgress")
            } else {
                onOpen(file)
            }
        }
    }

    fun onOpen(fileRef: FileRef) {
        runCatching {
            val mediaType = fileRef.mediaType ?: "text/plain"
            val openUrl = getOpenUrl(fileRef.downloadUrl)
            log("onOpen :: {}", openUrl, mediaType)
            if (openUrl != null) {
                IntentUtils.send(app, openUrl, mediaType)
            } else {
                val rootDir = getDownloadsDir()
                val localFolder = File(rootDir, fileRef.folder!!).also { it.mkdirs() }
                val localFile = File(localFolder, fileRef.getUid()!!)
                MediaScannerConnection.scanFile(app, arrayOf(localFile.toString()), null) { path, uri ->
                    log("scanned: {} -> {}", path, uri)
                }
                val uri = localFile.toUri()
                log("grantUriPermissions :: new file :: {}", uri)
                fileRepository.download(fileRef, uri)
                    .doOnError { dialogState.showError(it) }
                    .observeOn(getViewScheduler())
                    .subscribeBy("onDownloadFile") { file ->
                        getOpenUrl(file.downloadUrl)?.let {
                            showToast(string(R.string.toast_downloaded, file.title))
                        }
                    }
            }
        }.onFailure { dialogState.showError(it) }
    }

    fun onSaveAs(fragment: Fragment, fileRef: FileRef) {
        fragment.activity?.let { act ->
            act.withNewFile(fileRef.title!!) { uri ->
                fileRepository.download(fileRef, uri)
                    .doOnError { dialogState.showError(it) }
                    .subscribeBy("onDownloadFile")
            }
        }
    }

    private fun getDownloadsDir() = app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!

    private fun getOpenUrl(url: String?): Uri? {
        return url
            ?.let {
                url.toFile()
                    ?.let { FileProvider.getUriForFile(app, "${app.packageName}.file_provider", it) }
                    ?: Uri.parse(url)
            }
            ?.let { app.grantUriPermissions(it) }
    }

}