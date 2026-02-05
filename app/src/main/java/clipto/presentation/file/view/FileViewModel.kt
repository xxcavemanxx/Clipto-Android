package clipto.presentation.file.view

import android.app.Application
import android.graphics.Color
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import clipto.common.extensions.dashIfNullOrEmpty
import clipto.common.extensions.notNull
import clipto.common.extensions.toNullIfEmpty
import clipto.common.extensions.withFile
import clipto.common.misc.FormatUtils
import clipto.common.misc.IntentUtils
import clipto.domain.*
import clipto.domain.factory.FileRefFactory
import clipto.extensions.*
import clipto.presentation.blocks.AttrHorizontalBlock
import clipto.presentation.blocks.AttrIconBlock
import clipto.presentation.blocks.ProgressBlock
import clipto.presentation.blocks.SeparateScreenBlock
import clipto.presentation.blocks.layout.RowBlock
import clipto.presentation.blocks.ux.SeparatorHorizontalBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.clip.ClipScreenHelper
import clipto.presentation.common.StyleHelper
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.presentation.common.fragment.attributed.AttributedObjectViewModel
import clipto.presentation.common.fragment.attributed.blocks.AbbreviationBlock
import clipto.presentation.common.fragment.attributed.blocks.DescriptionBlock
import clipto.presentation.common.fragment.attributed.blocks.TitleBlock
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.file.FileScreenHelper
import clipto.presentation.file.view.blocks.PreviewBlock
import clipto.presentation.main.list.blocks.ClipItemFolderBlock
import clipto.presentation.usecases.FileUseCases
import clipto.presentation.usecases.NoteUseCases
import clipto.repository.ClipRepository
import clipto.repository.IClipRepository
import clipto.repository.IFileRepository
import clipto.store.files.FileScreenState
import clipto.store.files.FilesState
import clipto.store.user.UserState
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class FileViewModel @Inject constructor(
    app: Application,
    private val userState: UserState,
    private val filesState: FilesState,
    private val fileUseCases: FileUseCases,
    private val fileRepository: IFileRepository,
    private val fileScreenHelper: FileScreenHelper,
    private val clipScreenHelper: ClipScreenHelper,
    private val clipRepository: IClipRepository,
    private val noteUseCases: NoteUseCases
) : AttributedObjectViewModel<FileRef, FileScreenState>(app) {

    companion object {
        private var expandedClips = false
    }

    private var showHideClickableLayerCallback: (show: Boolean) -> Unit = {}

    override fun doCreate() {
        super.doCreate()
        filesState.changes.getLiveChanges()
            .filter { isViewMode() }
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .filter { newFile ->
                val currentFile = filesState.screenState.getValue()?.value
                val accept = newFile == currentFile
                log(
                    "FileRepository :: check file changes :: {} - {} - {}",
                    newFile.firestoreId,
                    currentFile?.firestoreId,
                    accept
                )
                accept
            }
            .subscribeBy("getFilesLiveChanges") {
                filesState.updateState(it)
            }
        hideActionsState.getLiveChanges()
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .observeOn(getViewScheduler())
            .subscribeBy("hideActionsStateLiveChanges") {
                showHideClickableLayerCallback.invoke(!it)
            }
    }

    override fun doClear() {
        fileScreenHelper.unbind()
        clipScreenHelper.unbind()
        super.doClear()
    }

    override fun onCreateBlocks(from: FileScreenState?, blocksCallback: (blocks: List<BlockItem<Fragment>>) -> Unit) {
        if (from == null) {
            return blocksCallback.invoke(emptyList())
        }

        log("createBlocks :: downloadUrl={}, downloaded={}, uid={}", from.value.downloadUrl, from.value.downloaded, from.value.getUid())

        val fileRef = from.value

        clipRepository.getByFile(fileRef)
            .observeOn(getViewScheduler())
            .subscribeBy(fileRef.getUid()) { clips ->
                val newFileRef = FileRefFactory.newInstance(fileRef)
                val screenState = from.copy(value = newFileRef)
                val blocks = mutableListOf<BlockItem<Fragment>>()
                val showAdditionalAttrs = getSettings().noteShowAdditionalAttributes

                // TITLE
                blocks.add(createTitleBlock(screenState, showAdditionalAttrs))
                if (showAdditionalAttrs) {
                    if (!fileRef.isReadOnly()) {
                        // ABBREVIATION
                        blocks.add(createAbbreviationBlock(screenState))
                    }
                    // DESCRIPTION
                    blocks.add(createDescriptionBlock(screenState))
                }

                // ATTRS
                blocks.add(createAttrsBlock(screenState))

                // UPLOAD
                if (screenState.isEditable() && !screenState.isEditMode()) {
                    blocks.add(createUploadBlock(screenState))
                }

                // DOWNLOAD
                if (!screenState.isEditMode() && screenState.value.isUploaded()) {
                    blocks.add(createDownloadBlock(screenState))
                }

                // CLIPS
                if (!screenState.isEditMode() && clips.isNotEmpty()) {
                    blocks.add(SpaceBlock.xs())

                    blocks.add(
                        SeparateScreenBlock(
                            titleRes = R.string.file_related_notes,
                            withBoldHeader = true,
                            withActionIcon = StyleHelper.getExpandIcon(expandedClips),
                            withBadge = !expandedClips,
                            clickListener = {
                                expandedClips = !expandedClips
                                onUpdateState()
                            }
                        )
                    )

                    if (expandedClips) {
                        val isSelectedGetter: (clip: Clip) -> Boolean = { false }
                        clips.forEach { clip ->
                            blocks.add(
                                ClipItemFolderBlock(
                                    clip = clip,
                                    checkable = false,
                                    synced = !userState.isNotSynced(clip),
                                    isSelectedGetter = isSelectedGetter,
                                    textLike = clipScreenHelper.getSearchByText(),
                                    listConfigGetter = mainState::getListConfig,
                                    onLongClick = this::onClick,
                                    onClick = this::onClick,
                                    onFetchPreview = clipScreenHelper::onFetchPreview,
                                    onClipIconClicked = this::onIconClick

                                )
                            )
                        }
                    }

                    blocks.add(SpaceBlock.dp8())
                }

                // PREVIEW
                if (screenState.value.canShowPreview()) {
                    blocks.add(
                        PreviewBlock<FileFragment>(
                            fileScreenHelper = fileScreenHelper,
                            screenState = screenState,
                            minHeightProvider = { it.getMinHeight() },
                            showHideClickableLayer = this::onShowHideActions
                        ) as BlockItem<Fragment>
                    )
                }

                blocksCallback.invoke(blocks)
            }
    }

    override fun createScreenStateLive(): MutableLiveData<FileScreenState> {
        return filesState.screenState.getMutableLiveData()
    }

    override fun onInitNavigator(callback: (index: Int) -> Unit) {
        callback.invoke(filesState.selectedFileIndex.getValue() ?: 0)
    }

    override fun getNavigatorMaxValue(): Int {
        return filesState.files.getValue()?.size ?: 0
    }

    override fun hasNavigator(): Boolean =
        appConfig.noteSupportFastPager()
                && isViewMode()
                && getNavigatorMaxValue() > 1

    override fun onNavigate(index: Int) {
        filesState.files.getValue()?.getOrNull(index)?.let {
            log("getById :: uid={}", it.getUid())
            fileRepository.getFile(it).subscribeBy("getById") { fileRef ->
                log("getById :: downloadUrl={}, downloaded={}, uid={}", fileRef.downloadUrl, fileRef.downloaded, fileRef.getUid())
                filesState.setViewState(fileRef, filesState.screenState.getValue()?.title)
            }
        }
    }

    private fun onShowHideActions(callback: (show: Boolean) -> Unit) {
        this.showHideClickableLayerCallback = callback
    }

    private fun doOnCancel() {
        onCancelAutoSave()
        filesState.screenState.getValue()?.value?.let { file ->
            onUpdate(file = file, viewMode = ViewMode.VIEW, focusMode = FocusMode.NONE)
        }
    }

    fun onCancel() {
        if (!getSettings().autoSave && contentChangedLive.value == true) {
            val confirmData = ConfirmDialogData(
                iconRes = R.drawable.ic_attention,
                title = string(R.string.clip_exit_without_save_title),
                description = string(R.string.clip_exit_without_save_description),
                confirmActionTextRes = R.string.button_yes,
                cancelActionTextRes = R.string.button_no,
                onConfirmed = { doOnCancel() }
            )
            dialogState.showConfirm(confirmData)
        } else {
            doOnCancel()
        }
    }

    fun onEdit() {
        filesState.screenState.getValue()?.value?.let { file ->
            onUpdate(file = file, viewMode = ViewMode.EDIT, focusMode = FocusMode.NONE)
        }
    }

    fun onShare() {
        getFile()?.let { file ->
            fileRepository.getPublicLink(file)
                .doOnSubscribe { appState.setLoadingState() }
                .doFinally { appState.setLoadedState() }
                .subscribeBy("onShare") {
                    IntentUtils.share(app, it)
                }
        }
    }

    fun onSave() {
        getFile()?.let { file ->
            fileRepository.save(file)
                .subscribeBy("onSave") { saved ->
                    onUpdate(saved, ViewMode.VIEW)
                }
        }
    }

    override fun onUpdateState() {
        getFile()?.let { fileRef ->
            screenStateLive.postValue(screenStateLive.value?.copy(value = fileRef, focusMode = FocusMode.NONE))
        }
    }

    private fun createTitleBlock(screenState: FileScreenState, showAdditionalAttrs: Boolean) =
        TitleBlock(
            screenState = screenState,
            showAdditionalAttributes = showAdditionalAttrs,
            onChanged = this::onTitleChanged,
            hintRes = R.string.attachments_attr_name,
            onEdit = { onUpdate(viewMode = ViewMode.EDIT, focusMode = FocusMode.TITLE) },
            onShowAttrs = this::onShowHideAdditionalAttributes,
            onNextFocus = this::onNextFocus
        )

    private fun createAbbreviationBlock(screenState: FileScreenState) =
        AbbreviationBlock(
            dialogState = dialogState,
            screenState = screenState,
            onChanged = this::onAbbreviationChanged,
            onEdit = { onUpdate(viewMode = ViewMode.EDIT, focusMode = FocusMode.ABBREVIATION) },
            onNextFocus = this::onNextFocus
        )

    private fun createDescriptionBlock(screenState: FileScreenState) =
        DescriptionBlock(
            dialogState = dialogState,
            mainState = mainState,
            screenState = screenState,
            onChanged = this::onDescriptionChanged,
            onEdit = { onUpdate(viewMode = ViewMode.EDIT, focusMode = FocusMode.DESCRIPTION) }
        )

    private fun createAttrsBlock(screenState: FileScreenState): BlockItem<Fragment> {
        val fileRef = screenState.value
        val attrs = mutableListOf<BlockItem<Fragment>>()

        if (!fileRef.isReadOnly()) {
            attrs.add(
                AttrIconBlock(
                    title = string(R.string.filter_label_fav),
                    iconRes = fileRef.getFavIcon(),
                    onClicked = {
                        val fav = filesState.changedFav.getValue() ?: false
                        onFavChanged(!fav)
                    }
                )
            )

            attrs.add(SeparatorHorizontalBlock())

            attrs.add(
                AttrHorizontalBlock(
                    title = string(R.string.file_attr_location),
                    value = FormatUtils.DASH,
                    onClicked = {
                        fileScreenHelper.onSelectFolder(
                            attributedObject = fileRef,
                            withNewFolder = true,
                            onSelected = this::onFolderChanged
                        )
                    },
                    valueKey = filesState.changedFolderId.getValue(),
                    valueProvider = fileScreenHelper::onGetFolderName
                )
            )

            attrs.add(SeparatorHorizontalBlock())
        }

        attrs.add(
            AttrHorizontalBlock(
                title = string(R.string.attachments_attr_size),
                value = FormatUtils.formatSize(app, fileRef.size)
            )
        )

        attrs.add(SeparatorHorizontalBlock())
        attrs.add(
            AttrHorizontalBlock(
                title = string(R.string.attachments_attr_type),
                value = fileRef.mediaType.dashIfNullOrEmpty()
            )
        )
        if (!fileRef.isReadOnly()) {
            attrs.add(SeparatorHorizontalBlock())
            attrs.add(
                AttrHorizontalBlock(
                    title = string(R.string.attachments_attr_created),
                    value = FormatUtils.formatDateTime(fileRef.createDate).dashIfNullOrEmpty()
                )
            )
        }
        attrs.add(SeparatorHorizontalBlock())
        attrs.add(
            AttrHorizontalBlock(
                title = string(R.string.file_attr_updated),
                value = FormatUtils.formatDateTime(fileRef.updateDate).dashIfNullOrEmpty()
            )
        )
        if (!fileRef.isReadOnly()) {
            attrs.add(SeparatorHorizontalBlock())
            attrs.add(
                AttrHorizontalBlock(
                    title = string(R.string.file_attr_last_modified),
                    value = FormatUtils.formatDateTime(fileRef.modifyDate).dashIfNullOrEmpty()
                )
            )
        }
        return RowBlock(attrs, spacingInDp = 0, scrollToPosition = 0)
    }

    private fun createUploadBlock(screenState: FileScreenState): ProgressBlock<Fragment> {
        val label: String?
        val actionIcon: Int
        val actionTitle: Int
        val trackColor: Int
        val textColor: Int
        var indicatorColor: Int = app.getColorPositive()
        val actionListener: (fragment: Fragment) -> Unit
        val blockClickListener: (fragment: Fragment) -> Unit
        val fileRef = screenState.value

        log("FileRepository :: createUploadBlock :: progress = {}, file = {}, uploaded = {}", fileRef.progress, fileRef.title, fileRef.uploaded)

        when {
            fileRef.isUploaded() -> {
                actionIcon = R.drawable.ic_cloud_upload
                actionTitle = R.string.file_action_update_in_cloud
                label = string(R.string.attachments_state_uploaded)
                trackColor = app.getColorPositive()
                textColor = Color.WHITE
                blockClickListener = this::onUpload
                actionListener = this::onUpload
            }
            fileRef.hasError() -> {
                actionIcon = R.drawable.ic_cloud_upload
                actionTitle = R.string.file_action_update_in_cloud
                indicatorColor = app.getColorNegative()
                trackColor = app.getColorNegative()
                textColor = Color.WHITE
                label = fileRef.error
                blockClickListener = this::onUpload
                actionListener = this::onUpload
            }
            else -> {
                actionIcon = R.drawable.ic_cancel_progress
                actionTitle = R.string.attachments_state_uploading
                trackColor = app.getBackgroundHighlightColor()
                textColor = app.getTextColorPrimary()
                label = string(R.string.attachments_state_uploading)
                blockClickListener = this::onCancelUpload
                actionListener = this::onCancelUpload
            }
        }

        return ProgressBlock(
            app,
            id = "upload",
            progress = screenState.value.progress,
            label = label,
            textColor = textColor,
            indicatorColor = indicatorColor,
            trackColor = trackColor,
            actionIcon = actionIcon,
            actionTitle = actionTitle,
            actionListener = actionListener,
            blockClickListener = blockClickListener,
            blockLongClickListener = this::onShowFileInfo
        )
    }

    private fun createDownloadBlock(screenState: FileScreenState): ProgressBlock<Fragment> {
        val label: String?
        val actionIcon: Int
        val actionTitle: Int
        val trackColor: Int
        val indicatorColor: Int
        val textColor: Int
        val actionListener: (fragment: Fragment) -> Unit
        val blockClickListener: (fragment: Fragment) -> Unit
        val fileRef = screenState.value

        when {
            fileRef.isDownloaded() -> {
                actionIcon = R.drawable.ic_cloud_download
                actionTitle = R.string.file_action_download_from_cloud
                indicatorColor = app.getColorAttention()
                trackColor = app.getColorAttention()
                textColor = Color.BLACK
                label = fileRef.downloadUrl
                blockClickListener = this::onOpen
                actionListener = this::onDownloadAs
            }
            !fileRef.isDownloaded() && fileRef.downloadUrl != null && !fileRef.hasError() -> {
                actionIcon = R.drawable.ic_cancel_progress
                actionTitle = R.string.menu_cancel
                label = string(R.string.attachments_state_downloading)
                trackColor = app.getBackgroundHighlightColor()
                indicatorColor = app.getColorAttention()
                textColor = app.getTextColorPrimary()
                blockClickListener = this::onCancelDownload
                actionListener = this::onCancelDownload
            }
            fileRef.hasError() -> {
                actionIcon = R.drawable.ic_cloud_download
                actionTitle = R.string.file_action_download_from_cloud
                indicatorColor = app.getColorNegative()
                trackColor = app.getColorNegative()
                textColor = Color.WHITE
                label = fileRef.error
                blockClickListener = this::onDownload
                actionListener = this::onDownloadAs
            }
            else -> {
                actionIcon = R.drawable.ic_cloud_download
                actionTitle = R.string.file_action_download_from_cloud
                trackColor = app.getBackgroundHighlightColor()
                indicatorColor = app.getColorAttention()
                textColor = app.getTextColorPrimary()
                label = string(R.string.file_action_download_from_cloud)
                blockClickListener = this::onDownload
                actionListener = this::onDownloadAs
            }
        }

        return ProgressBlock(
            app,
            id = "download",
            progress = screenState.value.progress,
            label = label,
            textColor = textColor,
            trackColor = trackColor,
            indicatorColor = indicatorColor,
            actionIcon = actionIcon,
            actionTitle = actionTitle,
            actionListener = actionListener,
            blockClickListener = blockClickListener,
            blockLongClickListener = this::onShowFileInfo
        )
    }

    private fun onShowFileInfo() {
        filesState.screenState.getValue()?.value?.let { fileRef ->
            val text = fileRef.toString(app)
            val data = ConfirmDialogData(
                title = fileRef.title.notNull(),
                description = text,
                descriptionIsMarkdown = true,
                iconRes = R.drawable.ic_attention,
                confirmActionTextRes = R.string.button_send,
                onConfirmed = { IntentUtils.share(app, text) }
            )
            dialogState.showConfirm(data)
        }
    }

    private fun onTitleChanged(title: CharSequence?) {
        if (isEditMode()) {
            if (filesState.changedName.setValue(title?.toNullIfEmpty(trim = false))) {
                log("onTitleChanged")
                onAutoSave()
            }
        }
    }

    private fun onFolderChanged(folderId: String?) {
        if (filesState.changedFolderId.setValue(folderId)) {
            if (isEditMode()) {
                log("onFolderChanged")
                onUpdateState()
                onAutoSave()
            } else {
                onSave()
            }
        }
    }

    private fun onFavChanged(fav: Boolean) {
        if (filesState.changedFav.setValue(fav)) {
            if (isEditMode()) {
                log("onFavChanged")
                onUpdateState()
                onAutoSave()
            } else {
                onSave()
            }
        }
    }

    private fun onDescriptionChanged(description: CharSequence?) {
        if (isEditMode()) {
            if (filesState.changedDescription.setValue(description?.toNullIfEmpty(trim = false))) {
                log("onDescriptionChanged")
                onAutoSave()
            }
        }
    }

    private fun onAbbreviationChanged(abbreviation: CharSequence?) {
        if (isEditMode()) {
            if (filesState.changedAbbreviation.setValue(abbreviation?.toNullIfEmpty(trim = false))) {
                log("onAbbreviationChanged")
                onAutoSave()
            }
        }
    }

    private fun onAutoSave(force: Boolean = isViewMode()) {
        val interval =
            when {
                force -> 0L
                getSettings().autoSave -> appConfig.autoSaveInterval()
                else -> -1L
            }
        if (interval != -1L) {
            getFile()?.let { fileRef ->
                onAutoSaveStateChanged(true)
                fileRepository.save(fileRef)
                    .delaySubscription(interval, TimeUnit.MILLISECONDS)
                    .subscribeBy("onSave") { autoSaved ->
                        log("onAutoSave :: completed :: {}", fileRef == autoSaved)
                        val currentFile = filesState.screenState.getValue()?.value
                        if (currentFile == autoSaved) {
                            filesState.screenState.updateValue { it?.copy(value = autoSaved) }
                            onAutoSaveStateChanged(false)
                        }
                    }
            }
        } else {
            contentChangedLive.postValue(true)
        }
    }

    private fun getFile(): FileRef? = getScreenState()?.value?.let { fileRef ->
        val fileName = filesState.changedName.getValue()
        if (fileName.isNullOrBlank()) {
            dialogState.showSnackbar(string(R.string.file_error_name_required))
            null
        } else {
            FileRefFactory.newInstance(fileRef).apply {
                val fileAbbreviation = filesState.changedAbbreviation.getValue()
                val fileDescription = filesState.changedDescription.getValue()
                val fileFolderId = filesState.changedFolderId.getValue()
                val fileFav = filesState.changedFav.getValue() ?: false

                abbreviation = fileAbbreviation.toNullIfEmpty(trim = false)
                description = fileDescription.toNullIfEmpty(trim = false)
                title = fileName.toNullIfEmpty(trim = false)
                folderId = fileFolderId
                fav = fileFav
            }
        }
    }

    private fun onUpdate(file: FileRef? = getFile(), viewMode: ViewMode = getViewMode(), focusMode: FocusMode = getFocusMode()) {
        if (file != null) {
            filesState.setState(fileRef = file, viewMode = viewMode, focusMode = focusMode, title = getTitle())
            contentChangedLive.postValue(false)
        }
    }

    private fun onUpload(fragment: Fragment) {
        filesState.screenState.getValue()?.value?.let { fileRef ->
            dialogState.showConfirm(
                ConfirmDialogData(
                    iconRes = R.drawable.ic_attention,
                    title = string(R.string.file_confirm_update_file),
                    description = string(R.string.file_confirm_update_file_description),
                    confirmActionTextRes = R.string.button_update,
                    onConfirmed = {
                        fragment.activity?.withFile { uri ->
                            fileRepository.update(fileRef, uri)
                                .doOnError { dialogState.showError(it) }
                                .subscribeBy("onUpload") { filesState.updateState(it) }
                        }
                    },
                    cancelActionTextRes = R.string.menu_cancel,
                    autoConfirm = { !fileRef.isUploaded() }
                )
            )

        }
    }

    private fun onCancelUpload(fragment: Fragment) {
        filesState.screenState.getValue()?.value?.let { fileRef ->
            fileRepository.cancelUploadProgress(fileRef)
                .subscribeBy("onCancelUpload")
        }
    }

    private fun onCancelDownload(fragment: Fragment) {
        filesState.screenState.getValue()?.value?.let { fileRef ->
            fileRepository.cancelDownloadProgress(fileRef)
                .subscribeBy("onCancelDownload")
        }
    }

    private fun onDownload(fragment: Fragment) {
        filesState.screenState.getValue()?.value?.let { fileRef ->
            fileUseCases.onOpen(fileRef)
        }
    }

    private fun onDownloadAs(fragment: Fragment) {
        filesState.screenState.getValue()?.value?.let { fileRef ->
            fileUseCases.onSaveAs(fragment, fileRef)
        }
    }

    private fun onOpen(fragment: Fragment) {
        filesState.screenState.getValue()?.value?.let { fileRef ->
            fileUseCases.onOpen(fileRef)
        }
    }

    private fun onIconClick(clip: Clip, previewUrl: String?): Boolean {
        if (previewUrl != null) {
            IntentUtils.open(app, previewUrl)
        } else {
            onClick(clip)
        }
        return false
    }

    private fun onClick(clip: Clip): Boolean {
        noteUseCases.onViewNote(clip)
        return false
    }

}