package clipto.presentation.usecases

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import clipto.common.extensions.withFile
import clipto.common.extensions.withPhoto
import clipto.common.extensions.withSafeFragmentManager
import clipto.common.extensions.withVideoRecord
import clipto.common.presentation.mvvm.RxViewModel
import clipto.dao.sharedprefs.SharedPrefsDao
import clipto.dao.sharedprefs.SharedPrefsState
import clipto.domain.FileType
import clipto.domain.MainAction
import clipto.extensions.getId
import clipto.extensions.toClip
import clipto.presentation.clip.add.AddClipFragment
import clipto.presentation.clip.add.data.AddClipRequest
import clipto.presentation.clip.add.data.AddClipType
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.file.add.AddFileFragment
import clipto.presentation.filter.advanced.AdvancedFilterFragment
import clipto.repository.IClipRepository
import clipto.store.app.AppState
import clipto.store.clip.ClipState
import clipto.store.clipboard.ClipboardState
import clipto.store.filter.FilterDetailsState
import clipto.store.folder.FolderRequest
import clipto.store.folder.FolderState
import clipto.store.main.MainState
import clipto.store.user.UserState
import com.wb.clipboard.R
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
class MainActionUseCases @Inject constructor(
    app: Application,
    val appState: AppState,
    val userState: UserState,
    val clipState: ClipState,
    val folderState: FolderState,
    val dialogState: DialogState,
    val mainState: MainState,
    val clipboardState: ClipboardState,
    val filterDetailsState: FilterDetailsState,
    private val noteUseCases: NoteUseCases,
    private val fileUseCases: FileUseCases,
    private val clipRepository: IClipRepository,
    private val sharedPrefsState: SharedPrefsState,
    private val sharedPrefsDao: SharedPrefsDao
) : RxViewModel(app) {

    fun onAction(
        action: MainAction,
        activity: FragmentActivity? = null,
        onReady: (callback: (activity: FragmentActivity) -> Unit) -> Unit = { activity?.let(it) }
    ) {
        log("onAction :: {}", action)
        when (action) {
            MainAction.NOTE_NEW -> onNewNote()
            MainAction.NOTE_BARCODE -> onScanBarcode()
            MainAction.NOTE_CLIPBOARD -> onReadClipboard(onReady)
            MainAction.NOTE_FILE -> onNewNoteFromFile(activity)
            MainAction.FILE_SELECT -> onSelectFile(onReady)
            MainAction.FILE_VIDEO -> onRecordVideo(onReady)
            MainAction.FILE_PHOTO -> onTakePhoto(onReady)
            MainAction.SNIPPET_KIT_NEW -> onNewSnippetKit()
            MainAction.SNIPPET_NEW -> onNewSnippet()
            MainAction.FOLDER_NEW -> onNewFolder()
            MainAction.FILTER_NEW -> onNewFilter()
            MainAction.TAG_NEW -> onNewTag()
            else -> Unit
        }
        sharedPrefsState.mainListData.getValue()
            ?.takeIf { it.lastAction != action && action.rememberLastAction && it.rememberLastAction }
            ?.let {
                log("onAction :: save :: {}", action)
                val newData = it.copy(lastAction = action)
                sharedPrefsDao.saveMainListData(newData)
                    .subscribeBy("saveMainListData")
            }
    }

    private fun withAuth(callback: () -> Unit = {}) = userState.withAuth(callback = callback)

    private fun onNewNote() {
        noteUseCases.onNewNote()
    }

    private fun onScanBarcode() {
        dialogState.requestScanBarcode {
            appState.requestShowClipInfoText(it, scanBarcode = true)
        }
    }

    private fun onNewNoteFromFile(activity: FragmentActivity?) {
        activity?.let { noteUseCases.onNewNoteFromFile(it) }
    }

    private fun onReadClipboard(onReady: (callback: (activity: FragmentActivity) -> Unit) -> Unit) {
        val clip = clipboardState.refreshClipboard(true)?.toClip(app, withMetadata = true)
        val text = clip?.text
        when {
            text.isNullOrBlank() -> {
                showToast(string(R.string.notification_title_empty))
                dismiss()
            }
            else -> {
                clipRepository.getByText(clip.text, clip.getId())
                    .map {
                        AddClipRequest(
                            id = it.getId(),
                            text = it.text,
                            screenType = AddClipType.EDIT
                        )
                    }
                    .onErrorReturn {
                        AddClipRequest(
                            text = text,
                            tracked = true,
                            screenType = AddClipType.EDIT,
                            folderId = appState.getActiveFolderId()
                        )
                    }
                    .observeOn(getViewScheduler())
                    .subscribeBy("getByText") {
                        onReady { act ->
                            AddClipFragment.show(act, it)
                        }
                    }
            }
        }
    }

    private fun onSelectFile(onReady: (callback: (activity: FragmentActivity) -> Unit) -> Unit) {
        withAuth {
            onReady { act ->
                act.withFile {
                    onAddFile(act, it, FileType.FILE)
                }
            }
        }
    }

    private fun onRecordVideo(onReady: (callback: (activity: FragmentActivity) -> Unit) -> Unit) {
        withAuth {
            onReady { act ->
                act.withVideoRecord {
                    onAddFile(act, it, FileType.RECORD)
                }
            }
        }
    }

    private fun onTakePhoto(onReady: (callback: (activity: FragmentActivity) -> Unit) -> Unit) {
        withAuth {
            onReady { act ->
                act.withPhoto {
                    onAddFile(act, it, FileType.PHOTO)
                }
            }
        }
    }

    private fun onAddFile(activity: FragmentActivity, uri: Uri, fileType: FileType) {
        if (activity.withSafeFragmentManager() != null) {
            AddFileFragment.show(activity, uri, fileType)
        } else {
            fileUseCases.onSaveAsFile(uri, fileType)
        }
    }

    private fun onNewSnippet() {
        noteUseCases.onNewSnippet()
    }

    private fun onNewFolder() {
        withAuth {
            val request = FolderRequest(
                onConsumeFolder = { folder ->
                    mainState.requestApplyFilter(folder, force = true)
                    true
                }
            )
            folderState.requestOpenFolder(request)
        }
    }

    private fun onNewSnippetKit() {
        filterDetailsState.requestNewSnippetKit(requestApply = true)
    }

    private fun onNewFilter() {
        val bundle = Bundle().apply { putInt(AdvancedFilterFragment.ATTR_TITLE, R.string.main_action_organize_filter_title) }
        appState.requestNavigateTo(R.id.action_advanced_filter, bundle)
    }

    private fun onNewTag() {
        filterDetailsState.requestNewTag(requestApply = true)
    }

}