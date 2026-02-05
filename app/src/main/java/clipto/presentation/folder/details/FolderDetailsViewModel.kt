package clipto.presentation.folder.details

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import clipto.action.DeleteFolderAction
import clipto.action.SaveFilterAction
import clipto.dao.firebase.mapper.FileMapper
import clipto.dao.objectbox.model.copy
import clipto.dao.objectbox.model.isNew
import clipto.domain.FileRef
import clipto.domain.Filter
import clipto.domain.SortBy
import clipto.domain.ViewMode
import clipto.domain.factory.FileRefFactory
import clipto.domain.factory.FilterFactory
import clipto.extensions.SortByExt
import clipto.presentation.blocks.TitleBlock
import clipto.presentation.blocks.bottomsheet.ObjectNameEditBlock
import clipto.presentation.blocks.bottomsheet.ObjectNameReadOnlyBlock
import clipto.presentation.blocks.bottomsheet.ObjectNameViewBlock
import clipto.presentation.blocks.domain.ColorWheelBlock
import clipto.presentation.blocks.domain.SortByWheelBlock
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.common.fragment.blocks.BlocksWithHintViewModel
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.file.FileScreenHelper
import clipto.repository.IFileRepository
import clipto.store.folder.FolderRequest
import clipto.store.folder.FolderState
import clipto.store.main.MainState
import clipto.store.user.UserState
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FolderDetailsViewModel @Inject constructor(
    app: Application,
    savedStateHandle: SavedStateHandle,
    private val userState: UserState,
    private val folderState: FolderState,
    private val fileRepository: IFileRepository,
    private val fileScreenHelper: FileScreenHelper,
    private val deleteFolderAction: DeleteFolderAction,
    private val saveFilterAction: SaveFilterAction,
    private val fileMapper: FileMapper
) : BlocksWithHintViewModel(app) {

    private val requestId: Int? by lazy { savedStateHandle.get(ATTR_REQUEST_ID) }
    private val request: FolderRequest? by lazy { folderState.requestsQueue[requestId] }
    private val colorsMatrix by lazy { appConfig.getColorsMatrix() }

    private var fileRef: FileRef = FileRefFactory.newInstance()
    private var fileRefBefore: FileRef = fileRef
    private var viewMode: ViewMode = ViewMode.VIEW
    private var sortByBefore: SortBy? = null
    private var hideHint = true

    override fun doCreate() {
        val folderRef = request?.folderRef
        if (folderRef != null && folderRef.isRootFolder()) {
            folderRef.title = string(R.string.folder_root)
            viewMode = ViewMode.READONLY
            init(folderRef)
        } else {
            fileRepository.getByUid(folderRef?.getUid())
                .onErrorReturn { request?.folderRef ?: fileMapper.createNewFolder() }
                .subscribeBy("getByUid") { fileRef ->
                    val viewMode = if (fileRef.isNew()) ViewMode.EDIT else ViewMode.VIEW
                    init(fileRef, viewMode)
                }
        }
        sortByBefore = appState.getFilterByFolders().sortBy
    }

    override fun doClear() {
        fileScreenHelper.unbind()
        requestId?.let { folderState.requestsQueue.remove(it) }
        super.doClear()
        if (!fileRef.isNew() && !FileRef.areContentTheSame(fileRef, fileRefBefore)) {
            log("files :: doClear :: changed :: {}", fileRef)
            fileRepository.save(fileRef)
                .subscribeBy {
                    log("files :: doClear :: update :: {}", it)
                    folderState.requestUpdateFolder(it)
                }
        }
        if (sortByBefore != appState.getFilterByFolders().sortBy) {
            saveFilterAction.execute(appState.getFilterByFolders())
        }
    }

    override fun onHideHint() {
        hideHint = true
        initAttrs(fileRef)
    }

    fun onShowHint() {
        hideHint = false
        initAttrs(fileRef)
    }

    private fun init(fileRef: FileRef, viewMode: ViewMode = this.viewMode) {
        fileRef.asFolder()
        this.fileRef = fileRef
        this.viewMode = viewMode
        this.fileRefBefore = fileRef.copy()
        initAttrs(fileRef)
    }

    private fun initAttrs(fileRef: FileRef) {
        log("initAttrs :: folderId :: {}", fileRef.folderId)

        val isEditMode = viewMode == ViewMode.EDIT
        val blocks = mutableListOf<BlockItem<Fragment>>()
        when (viewMode) {
            ViewMode.EDIT -> {
                withEditName(fileRef, blocks)
                withColors(fileRef, blocks)
                withAttrs(fileRef, blocks)
            }
            ViewMode.VIEW -> {
                withViewName(fileRef, blocks)
                withColors(fileRef, blocks)
                withAttrs(fileRef, blocks)
                blocks.add(SpaceBlock.xs())
                withSortBy(fileRef, blocks)
            }
            ViewMode.READONLY -> {
                withReadOnlyName(fileRef, blocks)
                blocks.add(SpaceBlock.md())
                withSortBy(fileRef, blocks)
            }
            else -> Unit
        }

        blocks.add(SpaceBlock.lg())

        postBlocks(blocks)
        onHideKeyboard()
        setHint(
            HintPresenter(
                id = fileRef.getUid(),
                editMode = isEditMode,
                hideHint = hideHint,
                value = fileRef.description,
                title = fileRef.description ?: string(R.string.hint_folders),
                onChanged = { fileRef.description = it }
            )
        )
    }

    private fun withEditName(fileRef: FileRef, blocks: MutableList<BlockItem<Fragment>>) {
        blocks.add(
            ObjectNameEditBlock(
                text = fileRef.title,
                hint = string(R.string.folder_name),
                maxLength = appConfig.maxLengthTitle(),
                onRename = this::onRename,
                onCancelEdit = this::onCancelEdit
            )
        )
        blocks.add(SeparatorVerticalBlock(marginHoriz = 0))
    }

    private fun withViewName(fileRef: FileRef, blocks: MutableList<BlockItem<Fragment>>) {
        blocks.add(
            ObjectNameViewBlock(
                name = fileRef.title,
                uid = fileRef.getUid(),
                color = fileRef.color,
                hideHint = hideHint,
                iconRes = R.drawable.file_type_folder,
                onEdit = this::onEdit,
                onDelete = this::onDelete,
                onShowHint = this::onShowHint
            )
        )
        blocks.add(SeparatorVerticalBlock(marginHoriz = 0))
    }

    private fun withReadOnlyName(fileRef: FileRef, blocks: MutableList<BlockItem<Fragment>>) {
        blocks.add(
            ObjectNameReadOnlyBlock(
                name = fileRef.title,
                uid = fileRef.getUid(),
                color = fileRef.color,
                hideHint = hideHint,
                iconRes = R.drawable.file_type_folder,
                onShowHint = this::onShowHint,
                actionIconRes = R.drawable.filter_group_add,
                onActionClick = this::onNewFolder
            )
        )
        blocks.add(SeparatorVerticalBlock(marginHoriz = 0))
    }

    private fun withColors(fileRef: FileRef, blocks: MutableList<BlockItem<Fragment>>) {
        if (colorsMatrix.isNotEmpty()) {
            blocks.add(SpaceBlock.lg())
            blocks.add(
                ColorWheelBlock(
                    selectedColor = fileRef.color,
                    colorsMatrix = colorsMatrix,
                    onChangeColor = this::onChangeColor
                )
            )
        }
    }

    private fun withAttrs(fileRef: FileRef, blocks: MutableList<BlockItem<Fragment>>) {
        blocks.add(SpaceBlock.lg())
        val block = fileScreenHelper.createAttrsBlock<Fragment>(
            fileRef = fileRef,
            onChanged = { initAttrs(it) }
        )
        blocks.add(block)
    }

    private fun withSortBy(fileRef: FileRef, blocks: MutableList<BlockItem<Fragment>>) {
        blocks.add(TitleBlock(R.string.list_config_sort))
        val filter = FilterFactory.createViewFolder(fileRef, mainState.getFilters().folders)
        blocks.add(
            SortByWheelBlock(
                uid = filter.uid,
                sortBy = filter.sortBy,
                sortByItems = SortByExt.getItems(filter),
                onChanged = { onApplySortBy(fileRef, filter, it) }
            )
        )
    }

    private fun onApplySortBy(fileRef: FileRef, filter: Filter, sortBy: SortBy) {
        appState.getFilterByFolders().sortBy = sortBy
        filter.sortBy = sortBy
        if (!fileRef.isNew() || fileRef.isRootFolder()) {
            mainState.requestApplyFilter(filter, force = true, closeNavigation = false)
        }
    }

    private fun onNewFolder() {
        folderState.requestNewFolder()
    }

    private fun onCancelEdit() {
        if (fileRef.isNew()) {
            dismiss()
        } else {
            viewMode = ViewMode.VIEW
            init(fileRefBefore)
        }
    }

    private fun onEdit() {
        viewMode = ViewMode.EDIT
        initAttrs(fileRef)
    }

    private fun onRename(name: String) {
        userState.withAuth {
            val file = fileRef.copy()
            val isNew = file.isNew()
            file.title = name.trim()
            fileRepository.save(file)
                .doOnError { dialogState.showError(it) }
                .subscribeBy("onSave") {
                    fileRefBefore = it
                    folderState.requestUpdateFolder(it)
                    val isNewlyCreated = isNew && !it.isNew()
                    if (isNewlyCreated && mainState.getActiveFilterSnapshot().isFolderFlat()) {
                        mainState.requestReloadFilter()
                    }
                    if (isNewlyCreated && request?.onConsumeFolder?.invoke(it) == true) {
                        dismiss()
                    } else {
                        init(it, ViewMode.VIEW)
                    }
                }
        }
    }

    private fun onDelete() {
        deleteFolderAction.execute(fileRef) {
            dismiss()
        }
    }

    private fun onChangeColor(color: String?) {
        fileRef.color = color
        initAttrs(fileRef)
    }

    companion object {
        private const val ATTR_REQUEST_ID = "request_id"

        fun buildArgs(request: FolderRequest): Bundle = Bundle().apply {
            putInt(ATTR_REQUEST_ID, request.id)
        }
    }

}