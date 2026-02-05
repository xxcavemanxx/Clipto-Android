package clipto.presentation.main.list

import android.app.Application
import android.graphics.Typeface
import android.text.SpannableString
import android.text.util.Linkify
import androidx.core.content.res.ResourcesCompat
import clipto.action.CopyClipsAction
import clipto.action.DeleteClipsAction
import clipto.action.SaveClipAction
import clipto.action.SaveSettingsAction
import clipto.analytics.Analytics
import clipto.backup.BackupManager
import clipto.common.extensions.subListExt
import clipto.common.presentation.mvvm.RxViewModel
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.config.IAppConfig
import clipto.dao.sharedprefs.SharedPrefsDao
import clipto.dao.sharedprefs.SharedPrefsState
import clipto.domain.*
import clipto.domain.factory.FileRefFactory
import clipto.extensions.isNew
import clipto.extensions.toClip
import clipto.presentation.clip.ClipScreenHelper
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.presentation.file.FileScreenHelper
import clipto.presentation.usecases.MainActionUseCases
import clipto.presentation.usecases.NoteUseCases
import clipto.repository.IClipRepository
import clipto.repository.IFileRepository
import clipto.store.app.AppState
import clipto.store.clipboard.ClipboardState
import clipto.store.clipboard.isNew
import clipto.store.filter.FilterDetailsState
import clipto.store.filter.FilterState
import clipto.store.folder.FolderState
import clipto.store.main.MainState
import clipto.store.main.ScreenState
import clipto.store.user.UserState
import clipto.utils.DomainUtils
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject

@HiltViewModel
class MainListViewModel @Inject constructor(
    app: Application,
    filterState: FilterState,
    val userState: UserState,
    val appConfig: IAppConfig,
    val backupManager: BackupManager,
    val noteUseCases: NoteUseCases,
    val clipboardState: ClipboardState,
    val mainState: MainState,
    private val appState: AppState,
    private val folderState: FolderState,
    private val dialogState: DialogState,
    private val filterDetailsState: FilterDetailsState,
    private val clipRepository: IClipRepository,
    private val fileRepository: IFileRepository,
    private val deleteClipsAction: DeleteClipsAction,
    private val saveClipAction: SaveClipAction,
    private val copyClipsAction: CopyClipsAction,
    private val saveSettingsAction: SaveSettingsAction,
    private val clipScreenHelper: ClipScreenHelper,
    private val sharedPrefsState: SharedPrefsState,
    private val sharedPrefsDao: SharedPrefsDao,
    val mainActionUseCases: MainActionUseCases,
    private val listProvider: MainListProvider,
    private val fileScreenHelper: FileScreenHelper
) : RxViewModel(app) {

    val screenLive = mainState.screen.getLiveData()
    val filtersLive = appState.filters.getLiveData()
    val lastActionLive = appState.lastAction.getLiveData()
    val selectedClipsLive = mainState.selectedObjects.getLiveData()
    val undoDeleteClipsLive = mainState.undoDeleteClips.getLiveData()
    val requestUpdateFilterLive = filterState.requestUpdateFilter.getLiveData()
    val requestCloseLeftNavigationLive = mainState.requestCloseLeftNavigation.getLiveData()

    fun onNavigateHierarchyBack(): Boolean = listProvider.onNavigateHierarchyBack()
    fun getMainListDataLive() = sharedPrefsState.mainListData.getLiveData()
    fun getHeaderBlocksLive() = listProvider.getHeaderBlocksLive()
    fun getEmptyBlocksLive() = listProvider.getEmptyBlocksLive()
    fun getForceLayoutLive() = listProvider.getForceLayoutLive()
    fun requestLayout() = listProvider.requestLayout()
    fun getClipsLive() = listProvider.getClipsLive()
    fun getFilesLive() = listProvider.getFilesLive()
    fun refresh() = listProvider.refresh()

    override fun doCreate() {
        mainState.requestApplyListConfig.getLiveChanges()
            .filter { it.isNotNull() }
            .map { it.value!! }
            .subscribeBy("requestApplyListConfig") {
                onChangeListConfig(it.specific, it.callback)
            }

        sharedPrefsDao.getMainListData()
            .ignoreElement()
            .andThen(initFilters())
            .andThen(initClipboard())
            .andThen(initTextFonts())
            .subscribeBy(
                tag = "init",
                onComplete = { mainState.requestReloadFilter() },
                onError = { mainState.requestReloadFilter() }
            )

        listProvider.bind(this)
    }

    override fun doClear() {
        super.doClear()
        mainState.onClear()
        listProvider.unbind()
        clipScreenHelper.unbind()
        fileScreenHelper.unbind()
    }

    fun getFilters() = appState.getFilters()
    fun getSettings() = appState.getSettings()
    fun getLastFilter() = appState.getFilterByLast()
    fun getSelectedClips(): List<Clip> = mainState.getSelectedClips()
    fun getSelectedFiles(): List<FileRef> = mainState.getSelectedFiles()

    fun onClearSelection() {
        mainState.selectedObjects.clearValue()
    }

    fun onNavigate(screen: ScreenState) = mainState.screen.setValue(screen)
    fun hasSelection(): Boolean = !mainState.selectedObjects.getValue().isNullOrEmpty()

    fun onOpenLastFilter() {
        val lastFilter = getLastFilter()
        if (lastFilter.isFolder()) {
            folderState.requestOpenFolder(FileRefFactory.newFolderWithId(lastFilter.folderId))
        } else {
            filterDetailsState.requestOpenFilter(lastFilter)
        }
    }

    fun onSyncSelected() {
        val clips = getSelectedClips()
        val title = string(if (clips.size > 1) R.string.sync_off_multiple_title else R.string.sync_off_title)
        val description = SimpleSpanBuilder()
            .append(string(R.string.sync_off_caption))
            .append("\n\n")
            .append(string(R.string.account_sync_plan_warning_limit_reached_title))
            .append("\n\n")
            .append(string(R.string.sync_off_reason_universal_clipboard))
            .append("\n\n")
            .append(string(R.string.sync_off_reason_manual_off))
            .build()
            .toString()
        val data = ConfirmDialogData(
            iconRes = R.drawable.ic_clip_not_synced_action,
            title = title,
            description = description,
            confirmActionTextRes = R.string.button_sync,
            onConfirmed = {
                clipRepository.syncAll(clips.toList()) {
                    onClearSelection()
                }
            })
        dialogState.showConfirm(data)
    }

    fun onSearch(text: String?) {
        mainState.requestSearch(text)
    }

    fun onCopy(clip: Clip) {
        if (clip.isActive) {
            clipboardState.clearClipboard()
            clip.isActive = false
        } else {
            if (clip.isNew()) clip.tracked = true
            copyClipsAction.execute(listOf(clip), clearSelection = false)
        }
    }

    fun onSave(clip: Clip, callback: (clip: Clip) -> Unit = {}) {
        saveClipAction.execute(
            clip,
            callback = callback,
            withLoadingState = false
        )
    }

    fun onEditTags(clip: Clip) {
        clipScreenHelper.onEditTags(
            clip = clip,
            titleRes = R.string.clip_info_label_tags_edit,
            onChanged = {
                onSave(it)
            }
        )
    }

    fun onUndoDelete(clips: List<Clip>, callback: (clips: List<Clip>) -> Unit = {}) {
        mainState.undoDeleteClips.clearValue()
        clipRepository.undoDeleteAll(clips)
            .observeOn(getViewScheduler())
            .doFinally { mainState.clearSelection() }
            .doOnSuccess { appState.refreshFilters() }
            .subscribeBy("onUndoDelete") { callback.invoke(it) }
    }

    fun onSelect(obj: AttributedObject) {
        val selected = !mainState.isSelected(obj)
        if (selected) {
            mainState.selectedObjects.updateValue { it!!.plus(obj) }
        } else {
            mainState.selectedObjects.updateValue { it!!.minus(obj) }
        }
    }

    fun onSelectRange(obj: AttributedObject) {
        Single
            .fromCallable {
                val isClip = obj is Clip
                val selectedObjects = mainState.selectedObjects.requireValue()
                val clips = mainState.clipsQuery.getValue()?.findLazyCached() ?: emptyList()
                val files = mainState.filesQuery.getValue()?.findLazyCached() ?: emptyList()
                val firstSelectedFile = selectedObjects.firstOrNull { it is FileRef }
                val lastSelectedFile = selectedObjects.lastOrNull { it is FileRef }
                val firstSelectedClip = selectedObjects.firstOrNull { it is Clip }
                val lastSelectedClip = selectedObjects.lastOrNull { it is Clip }
                val newFiles: List<FileRef>
                val newClips: List<Clip>
                if (isClip) {
                    // CLIPS
                    val lastClipIndex = clips.indexOf(obj)
                    val firstSelectedClipIndex = firstSelectedClip?.let { clips.indexOf(it) } ?: -1
                    newClips = clips.subListExt(firstSelectedClipIndex, lastClipIndex, 1)

                    // FILES
                    newFiles =
                        when {
                            firstSelectedClipIndex == -1 && firstSelectedFile == null -> files
                            firstSelectedFile == null -> emptyList()
                            else -> {
                                val firstSelectedFileIndex = files.indexOf(firstSelectedFile)
                                files.subList(firstSelectedFileIndex, files.size)
                            }
                        }

                    // SELECTION
                    mainState.selectedObjects.setValue(newFiles.plus(newClips).toSet())
                } else {
                    val lastFileIndex = files.indexOf(obj)
                    val firstSelectedFileIndex = firstSelectedFile?.let { files.indexOf(it) } ?: -1
                    val lastSelectedFileIndex = lastSelectedFile?.let { files.indexOf(it) } ?: -1

                    when {
                        lastSelectedFileIndex > lastFileIndex -> {
                            newFiles = files.subListExt(lastSelectedFileIndex, lastFileIndex, 1)
                            newClips = emptyList()
                        }
                        firstSelectedFileIndex != -1 -> {
                            newFiles = files.subListExt(firstSelectedFileIndex, lastFileIndex, 1)
                            newClips = emptyList()
                        }
                        firstSelectedFileIndex == -1 && firstSelectedClip != null -> {
                            newFiles = files.subListExt(lastFileIndex, files.size)
                            newClips = clips.subListExt(0, clips.indexOf(firstSelectedClip), 1)
                        }
                        lastSelectedClip != null -> {
                            newFiles = files.subListExt(lastFileIndex, files.size)
                            newClips = getSelectedClips()
                        }
                        else -> {
                            newFiles = emptyList()
                            newClips = emptyList()
                        }
                    }

                    // SELECTION
                    mainState.selectedObjects.setValue(newFiles.plus(newClips).toSet())
                }
            }
            .subscribeBy("onSelectRange", appState) {
                listProvider.requestLayout()
            }
    }

    fun onSelectAll(callback: () -> Unit) {
        Single
            .fromCallable {
                val clips = mainState.clipsQuery.getValue()?.find() ?: emptyList()
                val files = mainState.filesQuery.getValue()?.find() ?: emptyList()
                mainState.selectedObjects.updateValue { it!!.plus(files).plus(clips) }
            }
            .observeOn(getViewScheduler())
            .subscribeBy("onSelectAll", appState) {
                callback.invoke()
            }
    }

    fun onDelete(clips: List<Clip>, cancel: () -> Unit = {}) {
        if (clips.size > 1 || mainState.hasAnyThatRequiresConfirm(clips)) {
            dialogState.showConfirm(ConfirmDialogData(
                iconRes = R.drawable.ic_attention,
                title = string(R.string.main_delete_selected_title),
                description = string(R.string.main_delete_selected_description),
                confirmActionTextRes = R.string.menu_delete,
                onConfirmed = {
                    deleteClipsAction.execute(clips, false)
                },
                onClosed = {
                    cancel.invoke()
                }
            ))
        } else {
            deleteClipsAction.execute(clips, true)
        }
    }

    fun onNewNote() {
        noteUseCases.onNewNote()
    }

    fun onMergeSelectedNotes() {
        noteUseCases.onMergeNotes(mainState.getSelectedClips())
    }

    fun onFav(clips: List<Clip>, fav: Boolean) {
        clipRepository.favAll(clips, fav).subscribeBy("onFav")
    }

    fun onFavSelection(fav: Boolean) {
        val selection = mainState.getSelectedObjects()
        if (selection.isNotEmpty()) {
            val clips = getSelectedClips()
            val files = getSelectedFiles()
            log("onFavSelection :: {}, files={}, clips={}", fav, files.size, clips.size)
            clipRepository.favAll(clips, fav)
                .flatMap { fileRepository.favAll(files, fav) }
                .doOnError { dialogState.showError(it) }
                .doOnSuccess { onClearSelection() }
                .subscribeBy("onFavSelection", appState)
        }
    }

    fun onMoveSelectionToFolder() {
        val selection = mainState.getSelectedObjects()
        if (selection.isNotEmpty()) {
            val excluded = mainState.getSelectedFolders().mapNotNull { it.getUid() }
            val fromFolderId = DomainUtils.getCommonFolder(selection)
            fileScreenHelper.onSelectFolder(
                fromFolderId = fromFolderId,
                excludedIds = excluded,
                withNewFolder = true,
                onSelected = { folderId ->
                    if (fromFolderId != folderId) {
                        val clips = getSelectedClips()
                        val files = getSelectedFiles()
                        clipRepository.changeFolder(clips, folderId)
                            .flatMap { fileRepository.changeFolder(files, folderId) }
                            .doOnError { dialogState.showError(it) }
                            .doOnSuccess { onClearSelection() }
                            .subscribeBy("onMoveSelectionToFolder", appState)
                    }
                }
            )
        }
    }

    private fun initFilters() = Completable
        .fromCallable {
            val filters = getFilters()
            if (!getSettings().restoreFilterOnStart) {
                filters.last.withFilter(filters.all)
                mainState.activeFilter.setValue(filters.all)
            }
        }

    private fun initClipboard() = Single
        .fromCallable { clipboardState.clipData }
        .flatMapCompletable {
            clipboardState.refreshClipboard()
            val value = it.getValue()
            if (value.isNew()) {
                val clip = value.toClip(app)
                if (clip != null && clipboardState.canTakeNoteFromClipboard()) {
                    clipRepository.save(clip, false).ignoreElement()
                } else {
                    Completable.complete()
                }
            } else {
                Completable.complete()
            }
        }

    private fun initTextFonts() = Completable.fromCallable {
        try {
            Linkify.addLinks(SpannableString(""), Linkify.ALL)
        } catch (e: Exception) {
            Analytics.onError("error_step_init_linkify", e)
        }
        try {
            Font.valueOf(getSettings()).initTypeface(app)
        } catch (e: Exception) {
            Analytics.onError("error_step_init_base_font", e)
        }
    }

    private fun onChangeListConfig(specific: Filter? = null, callback: (state: ListConfig) -> ListConfig) {
        if (specific != null && specific != getFilters().findActive()) {
            log("skip config due to different active filter: {}", specific)
            val prevConfig = mainState.getListConfig().copy(specific)
            val newConfig = callback.invoke(prevConfig)
            specific.listStyle = newConfig.listStyle
            specific.sortBy = newConfig.sortBy
            return
        }

        val prevConfig = mainState.getListConfig()
        val newConfig = callback.invoke(prevConfig)

        if (newConfig != prevConfig) {

            val onApplyConfigInvoker: () -> Unit = {
                val filterChanged = prevConfig.sortBy != newConfig.sortBy || prevConfig.listStyle != newConfig.listStyle
                val filter = getFilters().findActive(specific ?: getLastFilter())
                filter.listStyle = newConfig.listStyle
                filter.sortBy = newConfig.sortBy

                mainState.listConfig.setValue(newConfig, notifyChanged = !filterChanged)

                if (filterChanged) {
                    log("applyFilter :: onApplyConfig: list updated: {}", newConfig)
                    mainState.requestApplyFilter(template = filter, closeNavigation = false)
                } else {
                    log("onApplyConfig: config changed: {}", newConfig)
                    saveSettingsAction.execute()
                }
            }

            if (prevConfig.textFont != newConfig.textFont) {
                val font = Font.valueOf(newConfig.textFont)
                if (font != null && font.typeface == null && font != Font.DEFAULT) {
                    try {
                        appState.setLoadingState()
                        ResourcesCompat.getFont(app, font.fontRes, object : ResourcesCompat.FontCallback() {
                            override fun onFontRetrieved(typeface: Typeface) {
                                try {
                                    font.typeface = typeface
                                } catch (e: Exception) {
                                    Analytics.onError("error_font_retrieved", e)
                                } finally {
                                    appState.setLoadedState()
                                    onApplyConfigInvoker.invoke()
                                }
                            }

                            override fun onFontRetrievalFailed(reason: Int) {
                                try {
                                    appState.showToast(string(R.string.font_request_failed, reason))
                                } finally {
                                    appState.setLoadedState()
                                    onApplyConfigInvoker.invoke()
                                }
                            }
                        }, null)
                    } catch (e: Exception) {
                        try {
                            Analytics.onError("error_apply_font", e)
                        } finally {
                            appState.setLoadedState()
                            onApplyConfigInvoker.invoke()
                        }
                    }
                } else {
                    onApplyConfigInvoker.invoke()
                }
            } else {
                onApplyConfigInvoker.invoke()
            }

        }
    }

}