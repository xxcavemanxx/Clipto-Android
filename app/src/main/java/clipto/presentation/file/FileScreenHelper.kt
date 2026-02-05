package clipto.presentation.file

import android.app.Application
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.ConcatAdapter
import clipto.common.extensions.*
import clipto.common.misc.FormatUtils
import clipto.common.presentation.mvvm.RxViewModel
import clipto.config.IAppConfig
import clipto.dao.firebase.FirebaseDaoHelper
import clipto.dao.firebase.mapper.FileMapper
import clipto.dao.objectbox.FileBoxDao
import clipto.domain.*
import clipto.domain.factory.FileRefFactory
import clipto.extensions.getColorPositive
import clipto.extensions.getTextColorAccent
import clipto.presentation.blocks.*
import clipto.presentation.blocks.bottomsheet.ObjectNameReadOnlyBlock
import clipto.presentation.blocks.domain.MainActionBlock
import clipto.presentation.blocks.layout.RowBlock
import clipto.presentation.blocks.ux.SeparatorHorizontalBlock
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.blocks.ux.ZeroStateVerticalBlock
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.recyclerview.BlockListAdapter
import clipto.presentation.common.recyclerview.BlockPagedListAdapter
import clipto.presentation.file.blocks.FilePathBlock
import clipto.presentation.file.blocks.SelectFileBlock
import clipto.presentation.preview.link.LinkPreview
import clipto.presentation.usecases.FileUseCases
import clipto.repository.IFileRepository
import clipto.store.StoreObject
import clipto.store.app.AppState
import clipto.store.files.FilesState
import clipto.store.folder.FolderRequest
import clipto.store.folder.FolderState
import clipto.store.main.MainState
import clipto.store.user.UserState
import com.wb.clipboard.R
import dagger.hilt.android.scopes.ViewModelScoped
import io.objectbox.android.ObjectBoxDataSource
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ViewModelScoped
class FileScreenHelper @Inject constructor(
    app: Application,
    private val mainState: MainState,
    private val appConfig: IAppConfig,
    private val appState: AppState,
    private val fileBoxDao: FileBoxDao,
    private val fileMapper: FileMapper,
    private val filesState: FilesState,
    private val userState: UserState,
    private val dialogState: DialogState,
    private val folderState: FolderState,
    private val fileUseCases: FileUseCases,
    private val fileRepository: IFileRepository,
    private val firebaseDaoHelper: FirebaseDaoHelper
) : RxViewModel(app) {

    private val filesLive: MediatorLiveData<PagedList<Any>> by lazy { MediatorLiveData() }
    private var lastFilesLive: LiveData<PagedList<Any>>? = null
    private var searchDisposable: Disposable? = null
    private var searchByText: String? = null

    val filesCountState = StoreObject<Long>(id = "files_count")

    fun getFilesLive(): LiveData<PagedList<Any>> = filesLive

    fun getSearchByText() = searchByText

    fun unbind() {
        doClear()
    }

    fun onSearch(
        filter: Filter.Snapshot,
        textToSearchBy: String? = getSearchByText(),
        pageMapper: (page: List<FileRef>) -> List<Any> = { it },
    ) {
        searchDisposable.disposeSilently()
        searchDisposable = getViewScheduler().scheduleDirect(
            {
                log("files :: onSearch :: text={}, filter={}", textToSearchBy, filter)
                searchByText = textToSearchBy
                val filterSnapshot = filter.copy(textLike = textToSearchBy)
                val query = fileBoxDao.getFiltered(filterSnapshot)
                val dataSource = ObjectBoxDataSource.Factory(query)
                    .mapByPage { pageMapper.invoke(it) }
                onClearSearch()
                val pageSize = appConfig.getClipListSize()
                lastFilesLive = LivePagedListBuilder(dataSource, pageSize).build()
                    .also { live ->
                        filesLive.addSource(live) {
                            filesLive.postValue(it)
                            onBackground {
                                filesCountState.setValue(query.count())
                            }
                        }
                    }
            },
            appConfig.getUiTimeout(),
            TimeUnit.MILLISECONDS
        )
    }

    fun getPreview(
        fileRef: FileRef,
        cornerRadiusInDp: Float = 12f,
        withSquarePreview: Boolean = false,
        withPreviewClickable: Boolean = true
    ): LinkPreview? {
        if (fileRef.isFolder) return null
        val collection = firebaseDaoHelper.getAuthUserCollection()
        val previewUrl = fileRef.getPreviewUrl(app, collection)
        if (previewUrl != null) {
            val preview = LinkPreview(
                withSquarePreview = withSquarePreview,
                cornerRadiusInDp = cornerRadiusInDp,
                withPreviewPlaceholder = withPreviewClickable,
                mediatype = fileRef.mediaType,
                url = previewUrl.toString(),
                title = fileRef.title,
                imageUrl = previewUrl
            )
            if (preview.isImage()) {
                preview.thumbUrl = fileRef.getThumbUrl(app, collection)?.takeIf { it != previewUrl }
            }
            if (fileRef.isReadOnly()) {
                preview.playbackUrl = previewUrl.toString()
            }
            return preview.takeIf { it.isPreviewable() }
        }
        return null
    }

    fun onPreview(file: FileRef, preview: LinkPreview?) = fileUseCases.onPreview(file, preview)

    fun onSelectFolder(
        attributedObject: AttributedObject,
        withNewFolder: Boolean = false,
        onSelected: (folderId: String?) -> Unit
    ) {
        onSelectFolder(
            fromFolderId = attributedObject.folderId,
            excludedIds = listOf(attributedObject.firestoreId),
            withNewFolder = withNewFolder,
            onSelected = onSelected
        )
    }

    fun onSelectFolder(
        fromFolderId: String?,
        excludedIds: List<String?> = emptyList(),
        withNewFolder: Boolean = false,
        onSelected: (folderId: String?) -> Unit
    ) {
        var lastFolderId: String? = null
        var onChanged: (folderId: String?) -> Unit = {}
        dialogState.requestBlocksDialog(
            onBackConsumed = {
                log("files :: on back :: {}", lastFolderId)
                if (lastFolderId != null) {
                    fileRepository.getParent(lastFolderId)
                        .doOnError { onChanged.invoke(null) }
                        .subscribeBy("onSelectFolder") {
                            onChanged.invoke(it.getUid())
                        }
                    true
                } else {
                    false
                }
            },
            onReady = { vm ->
                onChanged = onSelectFolder(
                    id = fromFolderId,
                    excludedIds = excludedIds,
                    onSelected = { folderId ->
                        log("files :: onSelectFolder {} -> {}", fromFolderId, folderId)
                        if (folderId != fromFolderId) {
                            onSelected.invoke(folderId)
                        }
                        vm.dismiss()
                    },
                    onBlocksReady = { vm.postBlocks(it, true) },
                    withNewFolder = withNewFolder,
                    onFolderChanged = {
                        lastFolderId = it
                    }
                )
            }
        )
    }

    fun onSelectFolder(
        id: String?,
        withNewFolder: Boolean = false,
        withBottomSpace: Boolean = true,
        excludedIds: List<String?> = emptyList(),
        onSelected: (folderId: String?) -> Unit,
        onFolderChanged: (folderId: String?) -> Unit = {},
        onBlocksReady: (blocks: List<BlockItem<Fragment>>) -> Unit
    ): (folderId: String?) -> Unit {
        var lastFolderId = id
        var onChanged: (folderId: String?) -> Unit = {}
        val topSpace = SpaceBlock.sm<Fragment>()
        val bottomSpace = SpaceBlock.fullSize<Fragment>(app, minusHeightInDp = 204)
        val onFileIconClicked: (fileRef: FileRef) -> Unit = this::onShowFolder
        val onFileClicked: (fileRef: FileRef?) -> Unit = { onChanged.invoke(it?.getUid()) }
        val onFileChanged: (callback: (file: FileRef) -> Unit) -> Unit = {}
        val isChecked: (file: FileRef) -> Boolean = { false }
        onChanged = { folderId ->
            lastFolderId = folderId
            onFolderChanged(folderId)
            val filter = Filter.Snapshot(
                fileTypes = listOf(FileType.FOLDER),
                folderIds = listOf(folderId.notNull()),
                fileIds = excludedIds.filterNotNull(),
                fileIdsWhereType = Filter.WhereType.NONE_OF
            )
            log("files :: onSelectFolder :: request :: {}", folderId)
            val blocks = mutableListOf<BlockItem<Fragment>>()
            fileRepository.getFilePath(FileRefFactory.newFolderWithParentId(folderId))
                .map { folders ->
                    val onSelectActionBlock =
                        if (folderId.toNullIfEmpty() != id.toNullIfEmpty()) {
                            PrimaryButtonBlock(
                                enabled = true,
                                titleRes = R.string.folder_select,
                                clickListener = {
                                    fileRepository.getByUid(lastFolderId)
                                        .observeOn(getViewScheduler())
                                        .onErrorReturn { FileRefFactory.root() }
                                        .subscribeBy("onSelectFolder", appState) { folder ->
                                            onSelected(folder.getUid()?.toNullIfEmpty())
                                            it.hapticKeyRelease()
                                        }
                                }
                            )
                        } else {
                            OutlinedButtonBlock<Fragment>(
                                enabled = false,
                                titleRes = R.string.folder_select,
                                clickListener = {}
                            )
                        }
                    log("files :: folders :: {}", folders)
                    blocks.add(topSpace)
                    blocks.add(createPathBlock(
                        withNewFolder = withNewFolder,
                        onLongClicked = onFileIconClicked,
                        folders = folders,
                        onChanged = onFileClicked,
                        onNewFolder = {
                            val newFolder = fileMapper.createNewFolder()
                            newFolder.folderId = lastFolderId
                            folderState.requestOpenFolder(FolderRequest(
                                withConfigurablePath = true,
                                folderRef = newFolder,
                                onConsumeFolder = {
                                    log("files :: consume new folder :: {}", it)
                                    onFileClicked(it)
                                    true
                                }
                            ))
                        }
                    ))
                    blocks.add(topSpace)
                    blocks.add(onSelectActionBlock)
                    blocks.add(SpaceBlock(heightInDp = 8))
                }
                .flatMap { fileRepository.getFiltered(filter) }
                .onErrorReturn { emptyList() }
                .map { folders ->
                    val foldersBlocks = folders.map { file ->
                        SelectFileBlock<Fragment>(
                            file,
                            fileScreenHelper = this,
                            onFileIconClicked = onFileIconClicked,
                            onFileClicked = onFileClicked,
                            onFileChanged = onFileChanged,
                            highlight = getSearchByText(),
                            isSelectedGetter = isChecked,
                            listConfigGetter = mainState::getListConfig
                        )
                    }
                    log("files :: onSelectFolder :: response :: {} -> {}", filter.folderIds, folders.size)
                    blocks.addAll(foldersBlocks)
                    if (withBottomSpace) {
                        blocks.add(bottomSpace)
                    }
                    blocks
                }
                .subscribeBy(tag = "onSelectFolder", onSuccess = onBlocksReady)
        }
        fileRepository.getByUid(lastFolderId)
            .onErrorReturn { FileRefFactory.root() }
            .subscribeBy("onChangedFolder") {
                onChanged.invoke(it.getUid().toNullIfEmpty())
            }

        folderState.requestUpdateFolder.getLiveChanges()
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .subscribeBy("requestUpdateFolder") {
                log("files :: requestUpdateFolder :: {}", it)
                onChanged.invoke(lastFolderId)
            }

        return onChanged
    }

    fun onShowFolder(fileRef: FileRef) {
        folderState.requestOpenFolder(
            FolderRequest(
                folderRef = fileRef,
                withConfigurablePath = true
            )
        )
    }

    fun <V> createPathBlock(
        flat: Boolean = false,
        withNewFolder: Boolean = false,
        folders: List<FileRef>,
        onLongClicked: (fileRef: FileRef) -> Unit,
        onChanged: (fileRef: FileRef?) -> Unit,
        onNewFolder: () -> Unit = {}
    ): BlockItem<V> {
        return FilePathBlock(
            flat = flat,
            folders = folders,
            onChanged = onChanged,
            withNewFolder = withNewFolder,
            onLongClicked = onLongClicked,
            onNewFolder = onNewFolder
        )
    }

    fun <V> createAttrsBlock(
        fileRef: FileRef,
        onChanged: (fileRef: FileRef) -> Unit,
        fileProvider: () -> FileRef? = { fileRef },
    ): BlockItem<V> {
        val isFolder = fileRef.isFolder
        val attrs = mutableListOf<BlockItem<V>>()

        // common
        attrs.add(
            AttrIconBlock(
                id = "file",
                title = string(R.string.filter_label_fav),
                iconRes = fileRef.getFavIcon(),
                onClicked = {
                    fileProvider()?.let { changed ->
                        changed.fav = !changed.fav
                        onChanged(changed)
                    }
                }
            )
        )
        attrs.add(SeparatorHorizontalBlock())
        attrs.add(
            AttrHorizontalBlock(
                id = "file",
                title = string(R.string.file_attr_location),
                value = FormatUtils.DASH,
                valueKey = fileRef.folderId,
                valueProvider = this::onGetFolderName,
                onClicked = {
                    fileProvider()?.let { ref ->
                        onSelectFolder(
                            attributedObject = ref,
                            withNewFolder = true,
                            onSelected = { folderId ->
                                ref.folderId = folderId
                                onChanged(ref)
                            }
                        )
                    }
                }
            )
        )

        if (!isFolder) {
            attrs.add(SeparatorHorizontalBlock())
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

            if (fileRef.modifyDate != null) {
                attrs.add(SeparatorHorizontalBlock())
                attrs.add(
                    AttrHorizontalBlock(
                        title = string(R.string.file_attr_last_modified),
                        value = FormatUtils.formatDateTime(fileRef.modifyDate).dashIfNullOrEmpty()
                    )
                )
            }
        } else {
            attrs.add(SeparatorHorizontalBlock())
            attrs.add(
                AttrHorizontalBlock(
                    title = string(R.string.attachments_attr_created),
                    value = FormatUtils.formatDateTime(fileRef.createDate).dashIfNullOrEmpty()
                )
            )
        }

        return RowBlock(attrs, spacingInDp = 0, scrollToPosition = 0)
    }

    fun <V> createAttrsBlock(
        files: List<FileRef>,
        onChanged: (files: List<FileRef>) -> Unit,
        filesProvider: () -> List<FileRef>? = { files },
    ): BlockItem<V> {
        val attrs = mutableListOf<BlockItem<V>>()

        attrs.add(
            AttrHorizontalBlock(
                title = string(R.string.file_attr_location),
                value = FormatUtils.DASH,
                valueKey = files.first().folderId,
                valueProvider = this::onGetFolderName,
                onClicked = {
                    filesProvider()?.let { refs ->
                        val ref = refs.first()
                        onSelectFolder(
                            attributedObject = ref,
                            withNewFolder = true,
                            onSelected = { folderId ->
                                refs.forEach { it.folderId = folderId }
                                onChanged.invoke(refs)
                            }
                        )
                    }
                }
            )
        )

        return RowBlock(attrs, spacingInDp = 0, scrollToPosition = 0)
    }

    fun onGetFolderName(key: String?, callback: (value: String) -> Unit) {
        if (key.isNullOrBlank()) {
            callback.invoke(FileRefFactory.ROOT)
        } else {
            fileRepository.getByUid(key)
                .onErrorReturn { FileRefFactory.root() }
                .observeOn(getViewScheduler())
                .map { it.title.notNull() }
                .subscribeBy("onGetFolderName", callback)
        }
    }

    fun onClearSearch(postEmptyState: Boolean = false) {
        if (isMainThread()) {
            log("files :: onClearSearch :: postEmptyState={}", postEmptyState)
            lastFilesLive?.let {
                it.value?.dataSource?.invalidate()
                filesLive.removeSource(it)
                lastFilesLive = null
            }
            if (postEmptyState) {
                filesLive.postValue(null)
                searchDisposable.disposeSilently()
            }
        }
    }

    fun onSelectFiles(
        folder: FileRef? = null,
        canAddExternal: Boolean = false,
        title: String? = folder?.title,
        uid: String? = folder?.getUid(),
        iconColor: String? = folder?.color,
        iconRes: Int = R.drawable.file_type_folder,
        actionIconRes: Int = R.drawable.main_action_folder_move_file,
        excludedIds: List<String> = emptyList(),
        callback: (files: List<FileRef>) -> Unit,
    ) {
        onClearSearch(postEmptyState = true)
        val getIgnoredFilesFlow =
            if (excludedIds.isEmpty() && folder != null) {
                fileRepository.getChildren(folder.getUid().notNull(), deep = false)
                    .onErrorReturn { emptyList() }
                    .map { it.mapNotNull { it.getUid() } }
            } else {
                Single.just(excludedIds)
            }
        getIgnoredFilesFlow
            .map { ignoreIds ->
                Filter.Snapshot(
                    listStyle = ListStyle.FOLDERS,
                    fileIds = ignoreIds,
                    fileIdsWhereType = Filter.WhereType.NONE_OF,
                    fileTypes = listOf(FileType.FOLDER),
                    fileTypesWhereType = Filter.WhereType.NONE_OF,
                    sortBy = appState.getFilterByFolders().sortBy
                )
            }
            .subscribeBy("onSelectFiles", appState) { filter ->
                dialogState.requestBlocksDialog(
                    onDestroy = { onClearSearch(postEmptyState = true) },
                    onCreateAdapter = { vm, fragment, adapter ->
                        val filesAdapter = BlockPagedListAdapter(Unit)

                        // DATA
                        val selectedFiles = mutableSetOf<FileRef>()
                        val fileChangeListeners = mutableListOf<(file: FileRef) -> Unit>()
                        val isSelectedGetter: (file: FileRef) -> Boolean = { selectedFiles.contains(it) }
                        val onFileChanged: (callback: (file: FileRef) -> Unit) -> Unit = { fileChangeListeners.add(it) }
                        val onFileClicked: (file: FileRef) -> Unit = {
                            if (selectedFiles.contains(it)) {
                                selectedFiles.remove(it)
                            } else {
                                selectedFiles.add(it)
                            }
                        }
                        val onFileIconClicked: (file: FileRef) -> Unit = {
                            fileUseCases.onPreview(it, getPreview(it))
                        }
                        val filesMapper: (files: List<FileRef>) -> List<BlockItem<Fragment>> = { files ->
                            files.map { file ->
                                SelectFileBlock(
                                    file = file,
                                    fileScreenHelper = this,
                                    listConfigGetter = mainState::getListConfig,
                                    onFileIconClicked = onFileIconClicked,
                                    isSelectedGetter = isSelectedGetter,
                                    onFileClicked = onFileClicked,
                                    onFileChanged = onFileChanged,
                                    highlight = getSearchByText()
                                )
                            }
                        }

                        // HEADER
                        fileRepository.getFiles(excludedIds)
                            .onErrorReturn { emptyList() }
                            .subscribeBy("getSelectedFiles") { files ->
                                val blocks = mutableListOf<BlockItem<Fragment>>()
                                blocks.add(
                                    ObjectNameReadOnlyBlock(
                                        name = title,
                                        uid = uid,
                                        color = iconColor,
                                        iconRes = iconRes,
                                        actionIconColor = app.getTextColorAccent(),
                                        actionIconRes = actionIconRes,
                                        onActionClick = {
                                            callback.invoke(selectedFiles.toList())
                                            vm.dismiss()
                                        }
                                    )
                                )

                                if (canAddExternal) {
                                    val fileMaxSize = appConfig.attachmentUploadLimit()
                                    val hasCamera = app.canTakePhoto()
                                    val hasVideo = app.canRecordVideo()
                                    blocks.add(SeparatorVerticalBlock(marginHoriz = 0))
                                    blocks.add(SpaceBlock(4))
                                    blocks.add(
                                        MainActionBlock(
                                            titleRes = R.string.main_action_files_file_title,
                                            description = string(R.string.main_action_files_file_description, fileMaxSize),
                                            iconRes = R.drawable.file_type_file,
                                            iconColor = app.getColorPositive().takeIf { userState.isAuthorized() },
                                            onClick = {
                                                takeFile(it) { files ->
                                                    selectedFiles.addAll(files)
                                                    callback.invoke(selectedFiles.toList())
                                                    vm.dismiss()
                                                }
                                            }
                                        )
                                    )
                                    if (hasCamera) {
                                        blocks.add(
                                            MainActionBlock(
                                                titleRes = R.string.main_action_files_photo_title,
                                                description = string(R.string.main_action_files_photo_description),
                                                iconRes = R.drawable.file_type_photo,
                                                iconColor = app.getTextColorAccent().takeIf { userState.isAuthorized() },
                                                onClick = {
                                                    takePhoto(it) { files ->
                                                        selectedFiles.addAll(files)
                                                        callback.invoke(selectedFiles.toList())
                                                        vm.dismiss()
                                                    }
                                                }
                                            )
                                        )
                                    }
                                    if (hasVideo) {
                                        blocks.add(
                                            MainActionBlock(
                                                titleRes = R.string.main_action_files_record_title,
                                                description = string(R.string.main_action_files_record_description, fileMaxSize),
                                                iconRes = R.drawable.file_type_record,
                                                iconColor = app.getTextColorAccent().takeIf { userState.isAuthorized() },
                                                onClick = {
                                                    recordVideo(it) { files ->
                                                        selectedFiles.addAll(files)
                                                        callback.invoke(selectedFiles.toList())
                                                        vm.dismiss()
                                                    }
                                                }
                                            )
                                        )
                                    }
                                }

                                if (files.isNotEmpty()) {
                                    selectedFiles.addAll(files)
                                    blocks.add(SpaceBlock(12))
                                    blocks.add(SeparatorVerticalBlock())
                                    blocks.add(SpaceBlock(12))
                                    blocks.addAll(filesMapper.invoke(files))
                                    blocks.add(SpaceBlock(12))
                                } else {
                                    blocks.add(SpaceBlock(8))
                                }
                                blocks.add(TextInputLayoutBlock(
                                    text = getSearchByText(),
                                    changedTextProvider = this::getSearchByText,
                                    hint = string(R.string.file_search_hint),
                                    onTextChanged = { text ->
                                        onSearch(
                                            filter = filter,
                                            pageMapper = filesMapper,
                                            textToSearchBy = text?.toString()
                                        )
                                        null
                                    }
                                ))
                                blocks.add(SpaceBlock(heightInDp = 12))
                                vm.postBlocks(blocks, scrollToTop = true)
                            }

                        // FOOTER
                        val footerAdapter = BlockListAdapter(Unit)
                        footerAdapter.submitList(
                            listOf(
                                ZeroStateVerticalBlock(),
                                SpaceBlock.screenSize(app, 0.8f)
                            )
                        )

                        // FILES
                        getFilesLive().observe(fragment) {
                            if (it != null) {
                                footerAdapter.submitList(listOf(SpaceBlock.screenSize(app, 0.8f)))
                            }
                            @Suppress("UNCHECKED_CAST")
                            filesAdapter.submitList(it as? PagedList<BlockItem<Unit>>)
                        }

                        // REALTIME
                        filesState.changes.getLiveChanges()
                            .filter { fileChangeListeners.isNotEmpty() }
                            .filter { it.isNotNull() }
                            .map { it.requireValue() }
                            .observeOn(getViewScheduler())
                            .subscribeBy("getLiveFileChanges") { file ->
                                log("getLiveFileChanges :: {} - {}", file.title, file.progress)
                                fileChangeListeners.forEach { it.invoke(file) }
                            }

                        onSearch(filter = filter, pageMapper = filesMapper)

                        ConcatAdapter(adapter, filesAdapter, footerAdapter)
                    }
                )
            }
    }

    private fun withAuth(callback: () -> Unit = {}) {
        userState.signIn(UserState.SignInRequest.newRequireAuthRequest())
            .observeOn(getViewScheduler())
            .subscribeBy("withAuth") { callback.invoke() }
    }

    private fun takeFile(fragment: Fragment, callback: (files: List<FileRef>) -> Unit) {
        withAuth {
            fragment.activity?.let { act ->
                act.withFile {
                    onAddFile(it, FileType.FILE, callback)
                }
            }
        }
    }

    private fun takePhoto(fragment: Fragment, callback: (files: List<FileRef>) -> Unit) {
        withAuth {
            fragment.activity?.let { act ->
                act.withPhoto {
                    onAddFile(it, FileType.PHOTO, callback)
                }
            }
        }
    }

    private fun recordVideo(fragment: Fragment, callback: (files: List<FileRef>) -> Unit) {
        withAuth {
            fragment.activity?.let { act ->
                act.withVideoRecord {
                    onAddFile(it, FileType.RECORD, callback)
                }
            }
        }
    }

    private fun onAddFile(uri: Uri, fileType: FileType, callback: (files: List<FileRef>) -> Unit) {
        fileRepository.upload(uri, fileType)
            .doOnError { dialogState.showError(it) }
            .doOnSuccess { callback.invoke(listOf(it)) }
            .subscribeBy("onAddFile")
    }

}