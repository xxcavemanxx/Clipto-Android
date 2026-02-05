package clipto.presentation.file.add

import android.app.Application
import android.content.res.Resources
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagedList
import clipto.common.extensions.notNull
import clipto.common.misc.Units
import clipto.dao.firebase.mapper.FileMapper
import clipto.dao.objectbox.model.isNew
import clipto.dao.sharedprefs.SharedPrefsDao
import clipto.dao.sharedprefs.data.AddFileScreenData
import clipto.domain.*
import clipto.dynamic.DynamicTextHelper
import clipto.extensions.*
import clipto.presentation.blocks.*
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.clip.ClipScreenHelper
import clipto.presentation.clip.view.blocks.AttachmentsBlock
import clipto.presentation.clip.view.blocks.TextBlock
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.presentation.common.fragment.attributed.blocks.AbbreviationBlock
import clipto.presentation.common.fragment.attributed.blocks.DescriptionBlock
import clipto.presentation.common.fragment.attributed.blocks.TagsBlock
import clipto.presentation.common.fragment.attributed.blocks.TitleBlock
import clipto.presentation.common.fragment.blocks.BlocksViewModel
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.file.FileScreenHelper
import clipto.presentation.file.add.data.AddFileType
import clipto.presentation.file.add.data.AddFilesRequest
import clipto.presentation.file.view.blocks.PreviewBlock
import clipto.presentation.main.list.blocks.FileItemBlock
import clipto.presentation.main.list.blocks.FileItemDefaultBlock
import clipto.presentation.usecases.FileUseCases
import clipto.repository.IClipRepository
import clipto.repository.IFileRepository
import clipto.store.clip.ClipScreenState
import clipto.store.clip.ClipState
import clipto.store.files.FileScreenState
import clipto.store.files.FilesState
import clipto.store.filter.FilterDetailsState
import clipto.store.user.UserState
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddFileViewModel @Inject constructor(
    app: Application,
    private val clipState: ClipState,
    private val userState: UserState,
    private val filesState: FilesState,
    private val fileMapper: FileMapper,
    private val filterDetailsState: FilterDetailsState,
    private val fileScreenHelper: FileScreenHelper,
    private val clipRepository: IClipRepository,
    private val fileRepository: IFileRepository,
    private val dynamicTextHelper: DynamicTextHelper,
    private val clipScreenHelper: ClipScreenHelper,
    private val sharedPrefsDao: SharedPrefsDao,
    private val savedStateHandle: SavedStateHandle,
    private val fileUseCases: FileUseCases
) : BlocksViewModel(app) {

    companion object {
        const val PEEK_HEIGHT = 0.75f
    }

    private val request: AddFilesRequest? by lazy { savedStateHandle.get(AddFileFragment.ATTR_FILES) }

    private val fileChangeListeners = mutableListOf<(file: FileRef) -> Unit>()
    private var screenData: AddFileScreenData = AddFileScreenData()
    private var changesCallback: (fileRef: FileRef) -> Unit = {}
    private var files: MutableList<FileRef> = ArrayList()
    private var fileState: FileScreenState? = null
    private var clip: Clip = Clip.NULL
    private var file: FileRef? = null
    private var multipleFiles = false

    fun getScreenState(): FileScreenState? = fileState
    private fun getContentMinHeight(): Int = (Resources.getSystem().displayMetrics.heightPixels * PEEK_HEIGHT - Units.DP.toPx(12f)).toInt()

    fun getClipsSearchByText() = clipScreenHelper.getSearchByText()
    fun getBackConfirmRequired() = fileState?.isPreviewMode() != true
    fun getClipsLive(): LiveData<PagedList<Clip>> = clipScreenHelper.getClipsLive()
    fun getClipsListConfig() = mainState.getListConfig().copy(appState.getFilterByAll()).copy(listStyle = ListStyle.PREVIEW)
    private fun getClipsFilter() = Filter.Snapshot().copy(appState.getFilterByAll())

    override fun doClear() {
        clipScreenHelper.unbind()
        fileScreenHelper.unbind()
        super.doClear()
    }

    override fun doCreate() {
        val addedFiles = request?.files?.mapNotNull { fileMapper.mapToFileRef(it.uri, it.fileType) }
        if (addedFiles.isNullOrEmpty()) {
            dismiss()
        } else {
            sharedPrefsDao.getAddFileData()
                .doOnSuccess { screenData = it }
                .subscribeBy("init") {
                    fileState = null
                    files = addedFiles.toMutableList()
                    val fileRef = addedFiles.first()
                    file = fileRef
                    multipleFiles = files.size > 1
                    clip = clipState.getDefaultNewClip(addedFiles)
                    val folderId = clip.folderId ?: screenData.folderId
                    files.forEach { it.folderId = folderId }
                    clip.folderId = folderId
                    updateBlocks(FileScreenState(fileRef, ViewMode.VIEW, FocusMode.NONE))
                }
            filesState.changes.getLiveChanges()
                .filter { it.isNotNull() }
                .map { it.requireValue() }
                .filter { files.contains(it) }
                .observeOn(getViewScheduler())
                .subscribeBy("getLiveChanges") { fileRef ->
                    val indexOf = files.indexOf(fileRef)
                    if (indexOf != -1) {
                        files[indexOf] = fileRef
                    }
                    if (!files.any { !it.isUploaded() }) {
                        dismiss()
                    } else {
                        if (fileRef == file) {
                            changesCallback.invoke(fileRef)
                        }
                        fileState?.let { state ->
                            updateBlocks(FileScreenState(fileRef, state.viewMode, FocusMode.NONE))
                        }
                        fileChangeListeners.forEach { it.invoke(fileRef) }
                    }
                }
        }
    }

    fun onSaveAsAttachment(clip: Clip) {
        log("onSaveAs :: attachment")
        fileState?.let { state ->
            withAuth {
                fileRepository.uploadAll(files)
                    .doOnSuccess {
                        val ids = it.mapNotNull { it.getUid() }
                        clip.fileIds = clip.fileIds.plus(ids).distinct()
                        updateBlocks(state.copy(value = it.first(), viewMode = ViewMode.PREVIEW))
                    }
                    .flatMap { clipRepository.save(clip, copied = false) }
                    .doOnError { dialogState.showError(it) }
                    .subscribeBy("onSaveAs")
            }
        }
    }

    override fun onShowHideKeyboard(visible: Boolean) {
        super.onShowHideKeyboard(visible)
        if (!visible) {
            log("update view state because of the keyboard hidden")
            fileState?.takeIf { it.isEditMode() }?.let {
                updateBlocks(it.copy(viewMode = ViewMode.VIEW))
            }
        }
    }

    private fun saveData() {
        sharedPrefsDao.saveAddFileData(screenData).subscribeBy("saveScreenData")
    }

    private fun onHideHint() {
        screenData = screenData.screenType.hideHint(screenData)
        refreshState()
        saveData()
    }

    private fun withAuth(callback: () -> Unit = {}) {
        if (userState.isAuthorized()) {
            callback.invoke()
        } else {
            val request = UserState.SignInRequest.newRequireAuthRequest()
            dialogState.showConfirm(ConfirmDialogData(
                iconRes = request.iconRes,
                title = string(request.titleRes),
                description = string(request.descriptionRes),
                confirmActionTextRes = request.actionTextRes,
                onConfirmed = {
                    userState.signIn(request)
                        .observeOn(getViewScheduler())
                        .subscribeBy("withAuth") { callback.invoke() }
                }
            ))
        }
    }

    private fun updateBlocks(state: FileScreenState) {
        if (state.value == file) {
            file = state.value
        }
        fileState = state

        val screenType = screenData.screenType
        val showAdditionalAttrs = getSettings().noteShowAdditionalAttributes
        val blocks = mutableListOf<BlockItem<Fragment>>()

        // SWITCH
        blocks.add(SpaceBlock(heightInDp = 8))
        blocks.add(
            TitleBlock(
                titleRes = R.string.menu_save_as,
                gravity = Gravity.CENTER_HORIZONTAL,
                width = ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        blocks.add(
            ThreeButtonsToggleBlock(
                firstButtonTextRes = AddFileType.FILE.getTitleRes(files),
                secondButtonTextRes = AddFileType.NOTE.getTitleRes(files),
                thirdButtonTextRes = AddFileType.ATTACHMENT.getTitleRes(files),
                onFirstButtonClick = { onChangeTab(AddFileType.FILE) },
                onSecondButtonClick = { onChangeTab(AddFileType.NOTE) },
                onThirdButtonClick = { onChangeTab(AddFileType.ATTACHMENT) },
                selectedButtonIndex = screenType.index
            )
        )

        if (screenType.canShowHint(screenData)) {
            blocks.add(SpaceBlock(heightInDp = 12))
            blocks.add(
                DescriptionSecondaryBlock(
                    description = string(screenType.getDescriptionRes(files)),
                    onCancel = this::onHideHint
                )
            )
        }

        if (state.isPreviewMode()) {
            clipScreenHelper.onClearSearch(postEmptyState = true)
            blocks.add(SpaceBlock(heightInDp = 8))
            if (multipleFiles) {
                files.forEach { fileRef ->
                    blocks.add(createFileBlock(fileRef))
                }
            } else {
                blocks.add(createFileUploadBlock(state, this::onCancelUpload))
                blocks.add(createFilePreviewBlock(state))
            }
            postBlocks(blocks)
            return
        }

        when (screenType) {
            AddFileType.FILE -> {
                clipScreenHelper.onClearSearch(postEmptyState = true)
                blocks.add(SpaceBlock(heightInDp = 8))
                blocks.add(createFileUploadBlock(state, this::onSaveAsFile))
            }
            AddFileType.NOTE -> {
                clipScreenHelper.onClearSearch(postEmptyState = true)
                blocks.add(SpaceBlock(heightInDp = 8))
                blocks.add(createFileUploadBlock(state, this::onSaveAsNote))
            }
            AddFileType.ATTACHMENT -> {
                clipScreenHelper.onSearch(getClipsFilter())
                blocks.add(SpaceBlock(heightInDp = 16))
                blocks.add(createClipSearchBlock(state))
                blocks.add(SpaceBlock(heightInDp = 12))
            }
        }

        when (screenType) {
            AddFileType.FILE -> {
                if (multipleFiles) {
                    blocks.add(createFilesAttrsBlock(state))

                    files.forEach { fileRef ->
                        blocks.add(createFileBlock(fileRef))
                    }
                } else {
                    // TITLE
                    blocks.add(createFileTitleBlock(state, showAdditionalAttrs))

                    if (showAdditionalAttrs) {
                        // ABBREVIATION
                        blocks.add(createFileAbbreviationBlock(state))
                        // DESCRIPTION
                        blocks.add(createFileDescriptionBlock(state))
                    }

                    // ATTRS
                    blocks.add(createFileAttrsBlock(state))

                    // PREVIEW
                    blocks.add(createFilePreviewBlock(state))
                }
            }
            AddFileType.NOTE -> {
                // TITLE
                blocks.add(createClipTitleBlock(state, showAdditionalAttrs))

                if (showAdditionalAttrs) {
                    // ABBREVIATION
                    blocks.add(createClipAbbreviationBlock(state))
                    // DESCRIPTION
                    blocks.add(createClipDescriptionBlock(state))
                }

                // ATTRS
                blocks.add(createAttrsBlock())
                blocks.add(SpaceBlock(heightInDp = 12))

                val hasTags = clip.getTags(noExcluded = true).isNotEmpty()
                if (hasTags) {
                    blocks.add(createTagsBlock(state))
                    blocks.add(SpaceBlock(heightInDp = 8))
                }

                // ATTACHMENTS
                blocks.add(createClipAttachmentsBlock())

                // TEXT
                blocks.add(createClipTextBlock(state))
            }
            AddFileType.ATTACHMENT -> {
                //
            }
        }

        postBlocks(blocks)

        if (state.viewMode != ViewMode.EDIT) {
            onHideKeyboard()
        }
    }

    private fun onChangeTab(type: AddFileType) {
        screenData = screenData.copy(screenType = type)
        refreshState()
        saveData()
    }

    private fun createFileBlock(file: FileRef): FileItemBlock<Fragment> {
        return FileItemDefaultBlock(
            file = file,
            fileScreenHelper = fileScreenHelper,
            isSelectedGetter = { false },
            onFileClicked = this::onIconClick,
            onFileIconClicked = this::onIconClick,
            onFileChanged = this::onFileChanged,
            listConfigGetter = mainState::getListConfig
        )
    }

    private fun onIconClick(file: FileRef) {
        fileUseCases.onPreview(file, fileScreenHelper.getPreview(file))
    }

    private fun onFileChanged(callback: (file: FileRef) -> Unit) {
        fileChangeListeners.add(callback)
    }

    private fun createFileTitleBlock(state: FileScreenState, showAdditionalAttrs: Boolean) =
        TitleBlock(
            screenState = state,
            showAdditionalAttributes = showAdditionalAttrs,
            hintRes = R.string.attachments_attr_name,
            onChanged = { file?.title = it?.toString() },
            onShowAttrs = {
                getSettings().noteShowAdditionalAttributes = it
                refreshState()
            },
            onEdit = { onEdit(FocusMode.TITLE) }
        )

    private fun createFileAbbreviationBlock(state: FileScreenState) =
        AbbreviationBlock(
            dialogState = dialogState,
            screenState = state,
            onChanged = { file?.abbreviation = it?.toString() },
            onEdit = { onEdit(FocusMode.ABBREVIATION) }
        )

    private fun createFileDescriptionBlock(state: FileScreenState) =
        DescriptionBlock(
            dialogState = dialogState,
            mainState = mainState,
            screenState = state,
            onChanged = { file?.description = it?.toString() },
            onEdit = { onEdit(FocusMode.DESCRIPTION) }
        )

    private fun createFileAttrsBlock(state: FileScreenState): BlockItem<Fragment> {
        return fileScreenHelper.createAttrsBlock(
            fileRef = state.value,
            fileProvider = { file },
            onChanged = {
                if (it.folderId != screenData.folderId) {
                    screenData = screenData.copy(folderId = it.folderId)
                    saveData()
                }
                updateBlocks(state)
            }
        )
    }

    private fun createFilesAttrsBlock(state: FileScreenState): BlockItem<Fragment> {
        return fileScreenHelper.createAttrsBlock(
            files = files,
            filesProvider = { files },
            onChanged = {
                val folderId = it.first().folderId
                if (folderId != screenData.folderId) {
                    screenData = screenData.copy(folderId = folderId)
                    saveData()
                }
                log("change folder id :: {}", folderId)
                updateBlocks(state)
            }
        )
    }

    private fun createFilePreviewBlock(state: FileScreenState): PreviewBlock<Fragment> =
        PreviewBlock(
            fileScreenHelper = fileScreenHelper,
            screenState = state,
            minHeightProvider = { getContentMinHeight() }
        )

    private fun createFileUploadBlock(state: FileScreenState, onSaveAs: () -> Unit): ProgressBlock<Fragment> {
        val id = screenData.screenType.index.toLong()
        val label: String?
        val actionIcon: Int
        val actionTitle: Int
        val trackColor: Int
        val textColor: Int
        var indicatorColor: Int = app.getColorPositive()
        val actionListener: (fragment: Fragment) -> Unit
        val blockClickListener: (fragment: Fragment) -> Unit
        val fileRef = state.value

        log(
            "FileRepository :: createUploadBlock {} :: progress = {}, file = {}, uploaded = {}",
            id,
            fileRef.progress,
            fileRef.title,
            fileRef.uploaded
        )

        when {
            fileRef.isUploaded() -> {
                actionIcon = 0
                actionTitle = 0
                label = string(R.string.attachments_state_uploaded)
                trackColor = app.getColorPositive()
                textColor = Color.WHITE
                blockClickListener = { dismiss() }
                actionListener = { dismiss() }
            }
            fileRef.hasError() -> {
                actionIcon = 0
                actionTitle = 0
                indicatorColor = app.getColorNegative()
                trackColor = app.getColorNegative()
                textColor = Color.WHITE
                label = fileRef.error
                blockClickListener = { onSaveAs() }
                actionListener = { onSaveAs() }
            }
            fileRef.isNew() -> {
                actionIcon = 0
                actionTitle = 0
                trackColor = app.getColorPositive()
                textColor = Color.WHITE
                label = string(R.string.button_save)
                blockClickListener = { onSaveAs() }
                actionListener = { onSaveAs() }
            }
            else -> {
                actionIcon = R.drawable.ic_cancel_progress
                actionTitle = R.string.attachments_state_uploading
                trackColor = app.getBackgroundHighlightColor()
                textColor = app.getTextColorPrimary()
                label = string(R.string.attachments_state_uploading)
                blockClickListener = { onCancelUpload() }
                actionListener = { onCancelUpload() }
            }
        }

        return ProgressBlock(
            app,
            id = "save",
            progress = state.value.progress,
            label = label,
            textColor = textColor,
            indicatorColor = indicatorColor,
            trackColor = trackColor,
            actionIcon = actionIcon,
            actionTitle = actionTitle,
            actionListener = actionListener,
            blockClickListener = blockClickListener,
            contentModificationState = id
        )
    }

    private fun createAttrsBlock(): BlockItem<Fragment> {
        return clipScreenHelper.createAttrsBlock(clip, onChanged = {
            refreshState()
            if (it.folderId != screenData.folderId) {
                screenData = screenData.copy(folderId = it.folderId)
                saveData()
            }
        })
    }

    private fun createClipTitleBlock(state: FileScreenState, showAdditionalAttrs: Boolean) =
        TitleBlock(
            screenState = ClipScreenState(clip, state.viewMode, state.focusMode),
            showAdditionalAttributes = showAdditionalAttrs,
            hintRes = R.string.clip_hint_title,
            onChanged = { clip.title = it?.toString() },
            onShowAttrs = {
                getSettings().noteShowAdditionalAttributes = it
                refreshState()
            },
            onEdit = { onEdit(FocusMode.TITLE) }
        )

    private fun createClipAbbreviationBlock(state: FileScreenState) =
        AbbreviationBlock(
            dialogState = dialogState,
            screenState = ClipScreenState(clip, state.viewMode, state.focusMode),
            onChanged = { clip.abbreviation = it?.toString() },
            onEdit = { onEdit(FocusMode.ABBREVIATION) }
        )

    private fun createClipDescriptionBlock(state: FileScreenState) =
        DescriptionBlock(
            mainState = mainState,
            dialogState = dialogState,
            screenState = ClipScreenState(clip, state.viewMode, state.focusMode),
            onChanged = { clip.description = it?.toString() },
            onEdit = { onEdit(FocusMode.DESCRIPTION) }
        )

    private fun createClipSearchBlock(state: FileScreenState) =
        TextInputLayoutBlock<Fragment>(
            text = getClipsSearchByText(),
            hint = string(R.string.main_search),
            onTextChanged = { text ->
                clipScreenHelper.onSearch(getClipsFilter(), text?.toString())
                null
            }
        )

    private fun createTagsBlock(state: FileScreenState) =
        TagsBlock(
            screenState = ClipScreenState(clip, state.viewMode, state.focusMode),
            filterDetailsState = filterDetailsState,
            onRemoveTag = {
                clip.tagIds = clip.tagIds.minus(it.uid.notNull())
                refreshState()
            },
            onEdit = {
                clipScreenHelper.onEditTags(clip) {
                    refreshState()
                }
            },
            bgColorAttr = R.attr.colorContext
        )

    private fun createClipAttachmentsBlock() =
        AttachmentsBlock(
            onGetFilesChanges = { callback -> fileChangeListeners.add(callback) },
            screenState = ClipScreenState(clip, ViewMode.VIEW, FocusMode.NONE),
            onGetFiles = { callback -> callback.invoke(files) },
            backgroundColor = app.getColorContext()
        )

    private fun createClipTextBlock(state: FileScreenState) =
        TextBlock<Fragment>(
            appConfig = appConfig,
            getClip = { clip },
            screenState = ClipScreenState(clip, state.viewMode, state.focusMode),
            getState = { fileState?.let { ClipScreenState(clip, it.viewMode, it.focusMode) } },
            showHidePreview = !getSettings().doNotPreviewLinks,
            textHelper = dynamicTextHelper,
            getMinHeight = { getContentMinHeight() },
            onEdit = { onEdit(FocusMode.TEXT_AUTO_SCROLL) },
            onTextChanged = { clip.text = it?.toString() }
        )

    private fun onSaveAsFile() {
        log("onSaveAs :: file")
        fileState?.let { state ->
            withAuth {
                fileRepository.uploadAll(files)
                    .doOnSuccess { updateBlocks(state.copy(value = it.first(), viewMode = ViewMode.PREVIEW)) }
                    .doOnError { dialogState.showError(it) }
                    .subscribeBy("onSaveAs")
            }
        }
    }

    private fun onSaveAsNote() {
        log("onSaveAs :: note")
        fileState?.let { state ->
            withAuth {
                fileRepository.uploadAll(files)
                    .doOnSuccess { updateBlocks(state.copy(value = it.first(), viewMode = ViewMode.PREVIEW)) }
                    .flatMap { clipRepository.save(clip, copied = false) }
                    .doOnError { dialogState.showError(it) }
                    .doOnSuccess { clip = it }
                    .subscribeBy("onSaveAs")
            }
        }
    }

    private fun onCancelUpload() {
        fileState?.let { state ->
            fileRepository.cancelUploadProgress(files)
                .doOnSubscribe { updateBlocks(state.copy(viewMode = ViewMode.VIEW)) }
                .subscribeBy("onCancelUpload")
        }
    }

    private fun onEdit(focusMode: FocusMode) {
        fileState?.let { state ->
            if (state.value.isNew() || state.value.hasError()) {
                updateBlocks(state.copy(viewMode = ViewMode.EDIT, focusMode = focusMode))
            } else {
                updateBlocks(state.copy(viewMode = ViewMode.VIEW, focusMode = FocusMode.NONE))
            }
        }
    }

    private fun refreshState() {
        fileState?.let { updateBlocks(it) }
    }

}