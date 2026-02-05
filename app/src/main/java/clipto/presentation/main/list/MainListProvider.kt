package clipto.presentation.main.list

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import clipto.action.CheckUserSessionAction
import clipto.common.extensions.disposeSilently
import clipto.common.extensions.toNullIfEmpty
import clipto.common.misc.IntentUtils
import clipto.common.presentation.mvvm.RxViewModel
import clipto.common.presentation.mvvm.lifecycle.SingleLiveData
import clipto.common.presentation.mvvm.lifecycle.UniqueLiveData
import clipto.config.IAppConfig
import clipto.dao.firebase.mapper.FileMapper
import clipto.dao.objectbox.ClipBoxDao
import clipto.dao.objectbox.FileBoxDao
import clipto.dao.objectbox.model.FileRefBox
import clipto.domain.*
import clipto.domain.factory.FileRefFactory
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.clip.ClipScreenHelper
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.file.FileScreenHelper
import clipto.presentation.main.list.blocks.*
import clipto.presentation.main.list.data.ClipItemListData
import clipto.presentation.main.list.data.FileItemListData
import clipto.presentation.main.list.data.ItemListStats
import clipto.presentation.usecases.FileUseCases
import clipto.repository.IClipRepository
import clipto.repository.IFileRepository
import clipto.repository.IFilterRepository
import clipto.store.app.AppState
import clipto.store.clipboard.ClipboardState
import clipto.store.files.FilesState
import clipto.store.folder.FolderRequest
import clipto.store.folder.FolderState
import clipto.store.main.MainState
import clipto.store.main.ScreenState
import clipto.store.user.UserState
import dagger.hilt.android.scopes.ViewModelScoped
import io.objectbox.android.ObjectBoxDataSource
import io.objectbox.query.Query
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@ViewModelScoped
class MainListProvider @Inject constructor(
    app: Application,
    private val dialogState: DialogState,
    private val appState: AppState,
    private val mainState: MainState,
    private val userState: UserState,
    private val appConfig: IAppConfig,
    private val filesState: FilesState,
    private val folderState: FolderState,
    private val clipboardState: ClipboardState,
    private val clipBoxDao: ClipBoxDao,
    private val fileBoxDao: FileBoxDao,
    private val fileMapper: FileMapper,
    private val fileUseCases: FileUseCases,
    private val fileRepository: IFileRepository,
    private val clipRepository: IClipRepository,
    private val fileScreenHelper: FileScreenHelper,
    private val filterRepository: IFilterRepository,
    private val clipScreenHelper: ClipScreenHelper,
    private val checkUserSessionAction: CheckUserSessionAction
) : RxViewModel(app) {

    private val settings by lazy { appState.getSettings() }
    private val filters by lazy { appState.getFilters() }
    private val lastFilter by lazy { filters.last }

    private val clipsLive: MediatorLiveData<ClipItemListData> = MediatorLiveData()
    private var lastClipsLive: LiveData<PagedList<ClipItemBlock<Unit>>>? = null
    private var reloadClipsDisposable: Disposable? = null
    fun getClipsLive(): LiveData<ClipItemListData> = clipsLive

    private val fileChangeListeners = mutableListOf<(file: FileRef) -> Unit>()
    private val filesLive: MediatorLiveData<FileItemListData> = MediatorLiveData()
    private var lastFilesLive: LiveData<PagedList<FileItemBlock<Unit>>>? = null
    fun getFilesLive(): LiveData<FileItemListData> = filesLive

    private val headerBlocksLive = MutableLiveData<List<BlockItem<Unit>>>()
    fun getHeaderBlocksLive(): LiveData<List<BlockItem<Unit>>> = headerBlocksLive

    private val emptyBlocksLive = UniqueLiveData<List<BlockItem<Unit>>>()
    fun getEmptyBlocksLive(): LiveData<List<BlockItem<Unit>>> = emptyBlocksLive

    private val forceLayoutLive = SingleLiveData<Boolean>()
    fun getForceLayoutLive(): LiveData<Boolean> = forceLayoutLive

    private lateinit var viewModel: MainListViewModel

    private val detectFlatMode = AtomicBoolean(false)

    fun onNavigateHierarchyBack(): Boolean {
        val activeFilter = mainState.activeFilter.requireValue()
        val folderId = activeFilter.folderId
        if (!folderId.isNullOrBlank()) {
            fileRepository.getParent(folderId)
                .onErrorReturn { FileRefFactory.root() }
                .subscribeBy("onNavigateHierarchyBack") { folderRef ->
                    onOpenFolder(folderRef)
                }
            return true
        }
        return false
    }

    fun refresh() {
        clipsLive.value?.let { clipsLive.postValue(it) }
        filesLive.value?.let { filesLive.postValue(it) }
    }

    fun requestLayout() {
        forceLayoutLive.postValue(true)
    }

    fun bind(viewModel: MainListViewModel) {
        this.viewModel = viewModel
        mainState.requestApplyFilter.getLiveChanges()
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .flatMapSingle {
                applyFilter(
                    force = it.force,
                    template = it.template,
                    withTextLike = it.withTextLike,
                    closeNavigation = it.closeNavigation,
                    snapshotInterceptor = it.snapshotInterceptor
                )
            }
            .subscribeBy("requestApplyFilter") {}

        var skipFirst = true
        mainState.listConfig.getLiveChanges()
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .observeOn(getBackgroundScheduler())
            .subscribeBy("listConfigChanges") {
                if (!skipFirst) {
                    log("requestUpdateSwipeActions :: update list config :: {}", it)
                    requestLayout()
                } else {
                    skipFirst = false
                }
            }

        filesState.changes.getLiveChanges()
            .filter { fileChangeListeners.isNotEmpty() }
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .observeOn(getViewScheduler())
            .subscribeBy("getLiveChanges") { file ->
                log("getLiveChanges :: {} - {}", file.title, file.progress)
                fileChangeListeners.forEach { it.invoke(file) }
            }

        folderState.requestUpdateFolder.getLiveChanges()
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .subscribeBy("requestUpdateFolder") {
                val activeFilter = mainState.activeFilter.requireValue()
                when {
                    activeFilter.folderId == it.getUid() -> {
                        mainState.requestApplyFilter(it, force = true, closeNavigation = false)
                    }
                    activeFilter.isFolder() -> {
                        requestLayout()
                    }
                    else -> {
                        appState.refreshFilters()
                    }
                }
            }
    }

    fun unbind() {
        doClear()
        fileScreenHelper.unbind()
        clipScreenHelper.unbind()
    }

    private fun applyFilter(
        template: Filter,
        force: Boolean = false,
        withTextLike: Boolean = false,
        closeNavigation: Boolean = true,
        snapshotInterceptor: (snapshot: Filter.Snapshot) -> Unit = {}
    ): Single<*> {
        val filter = filters.findActive(template)
        val activeSnapshot = mainState.getActiveFilterSnapshot()
        val listConfig = mainState.listConfig.requireValue().copy(filter)
        val snapshot = activeSnapshot.copy(filter)
        log(
            "SEARCH :: force={}, folder={}, ids={}, sortBy={}, textLike=[{}, {}]",
            force,
            filter.folderId != null,
            snapshot.folderIds,
            snapshot.sortBy,
            template.textLike,
            filter.textLike
        )
        snapshot.textLike =
            if (withTextLike) {
                activeSnapshot.textLike
            } else {
                template.textLike
            }
        snapshotInterceptor.invoke(snapshot)
        snapshot.pinStarred = appState.getGroupNotes().pinStarredEnabled || appState.getFilterByStarred().pinStarredEnabled
        snapshot.textLike = snapshot.textLike.toNullIfEmpty()

        val folderIds = snapshot.folderIds
        val activeTextLike = activeSnapshot.textLike
        val activeFolderIds = activeSnapshot.folderIds
        val hasTextLike = !snapshot.textLike.isNullOrEmpty()
        val activeHasTextLike = !activeTextLike.isNullOrEmpty()
        val requestFlatMode = detectFlatMode.getAndSet(false)
        val isSameFolder = folderIds.isNotEmpty() && folderIds.firstOrNull() == activeFolderIds.firstOrNull()
        val toggleFlatMode = (isSameFolder && requestFlatMode)
                || (hasTextLike && folderIds.isNotEmpty())
                || (!hasTextLike && activeHasTextLike && folderIds.isNotEmpty())

        val flow =
            if (!force && !toggleFlatMode && snapshot == activeSnapshot) {
                log("skip reloading")
                Single.just(0L)
            } else {
                val stats = ItemListStats(template)
                Single.just(snapshot)
                    .observeOn(getBackgroundScheduler())
                    .flatMap {
                        val reloadFlatMode = !toggleFlatMode && isSameFolder && force && activeSnapshot.isFolderFlat()
                        if (toggleFlatMode || reloadFlatMode) {
                            if (!reloadFlatMode && !hasTextLike && (activeFolderIds.size > 1 || activeHasTextLike)) {
                                val newIds = folderIds.firstOrNull()?.let { listOf(it) } ?: emptyList()
                                Single.just(snapshot.copy(folderIds = newIds))
                            } else {
                                val folderId = snapshot.folderIds.firstOrNull()?.toNullIfEmpty()
                                fileRepository.getChildren(folderId, deep = true, listOf(FileType.FOLDER))
                                    .map { newFolders -> newFolders.mapNotNull { it.getUid() } }
                                    .map { newIds -> folderIds.plus(newIds) }
                                    .map { newIds -> snapshot.copy(folderIds = newIds) }
                            }
                        } else if (isSameFolder) {
                            Single.just(snapshot.copy(folderIds = activeFolderIds))
                        } else {
                            Single.just(snapshot)
                        }
                    }
                    .map { newSnapshot ->
                        if (newSnapshot.folderIds.size > 1) {
                            if (requestFlatMode || !hasTextLike) {
                                newSnapshot.copy(
                                    textLike = null,
                                    fileTypes = listOf(FileType.FOLDER),
                                    fileTypesWhereType = Filter.WhereType.NONE_OF
                                )
                            } else {
                                newSnapshot
                            }
                        } else {
                            newSnapshot
                        }
                    }
                    .map { newSnapshot ->
                        log("APPLY FILTER :: toggleFlatMode={}, folderIds :: {}", toggleFlatMode, newSnapshot.folderIds)
                        emptyBlocksLive.postValue(emptyList())
                        filesLive.postValue(FileItemListData(_listConfig = listConfig, _snapshot = newSnapshot))
                        clipsLive.postValue(ClipItemListData(_listConfig = listConfig, _snapshot = newSnapshot))
                        mainState.filterSnapshot.setValue(newSnapshot)
                        newSnapshot
                    }
                    .flatMap { newSnapshot ->
                        if (settings.restoreFilterOnStart) {
                            lastFilter.withSnapshot(newSnapshot)
                            lastFilter.name = template.name
                            lastFilter.color = template.color
                            filterRepository.save(lastFilter).map { newSnapshot }
                        } else {
                            lastFilter.withSnapshot(newSnapshot)
                            Single.just(newSnapshot)
                        }
                    }
                    .flatMap { newSnapshot ->
                        reloadFiles(
                            stats = stats,
                            snapshot = newSnapshot,
                            listConfig = listConfig
                        ).map { newSnapshot }
                    }
                    .flatMap { newSnapshot ->
                        reloadClips(
                            stats = stats,
                            activeSnapshot = activeSnapshot,
                            snapshot = newSnapshot,
                            listConfig = listConfig,
                            force = force
                        ).map { newSnapshot }
                    }
            }

        return flow
            .doOnError { dialogState.showError(it) }
            .doOnSubscribe {
                mainState.activeFilter.setValue(filter, notifyChanged = false)
                mainState.listConfig.setValue(listConfig, notifyChanged = false)
                if (closeNavigation) mainState.requestCloseLeftNavigation(closeNavigation)
            }
    }

    private fun updateEmptyState(stats: ItemListStats) {
        if (stats.isInitialized()) {
            if (stats.hasData()) {
                emptyBlocksLive.postValue(emptyList())
            } else {
                val filter = filters.findActive(stats.filter)
                emptyBlocksLive.postValue(listOf(EmptyStateBlock(filter, listConfigGetter = this::getListConfig)))
            }
        }
        if (stats.isInitialized()) {
            log("MainList :: initialized :: files={}, notes={}", stats.filesCount, stats.notesCount)
            appState.refreshFilters()
        }
    }

    private fun reloadFiles(
        stats: ItemListStats,
        snapshot: Filter.Snapshot,
        listConfig: ListConfig
    ): Single<QuerySnapshot<*, *>> {
        mainState.filesQuery.clearValue()
        if (!snapshot.isFolder()) {
            onMain {
                lastFilesLive?.let {
                    it.value?.dataSource?.invalidate()
                    filesLive.removeSource(it)
                }
                headerBlocksLive.value = emptyList()
                filesLive.value = FileItemListData(_listConfig = listConfig, _snapshot = snapshot)
            }
            return Single
                .fromCallable {
                    stats.filesCount = 0
                    QuerySnapshot<Any, Any>()
                }
                .map { it }
        } else {
            val folder = FileRefFactory.newFolderWithParentId(snapshot.folderIds.firstOrNull())
            clipsLive.postValue(ClipItemListData(_listConfig = listConfig, _snapshot = snapshot))
            filesLive.postValue(FileItemListData(_listConfig = listConfig, _snapshot = snapshot))
            return fileRepository.getFilePath(folder)
                .map { folders ->
                    val pathBlock = fileScreenHelper.createPathBlock<Unit>(
                        onChanged = { onOpenFolder(it, detectFlatMode = true) },
                        flat = snapshot.isFolderFlat(),
                        onLongClicked = this::onShowFolder,
                        onNewFolder = this::onNewFolder,
                        withNewFolder = true,
                        folders = folders
                    )
                    headerBlocksLive.postValue(
                        listOf(
                            SpaceBlock.xs(),
                            pathBlock,
                            SpaceBlock.sm()
                        )
                    )
                }
                .map {
                    val query = fileBoxDao.getFiltered(snapshot)
                    val dataSource = ObjectBoxDataSource.Factory(query)
                        .mapByPage { it.map { file -> map(file, snapshot) } }
                    mainState.filesQuery.setValue(query)
                    QuerySnapshot(
                        query = query,
                        dataSource = dataSource
                    )
                }
                .doOnSuccess { qs ->
                    onMain {
                        lastFilesLive?.let {
                            it.value?.dataSource?.invalidate()
                            filesLive.removeSource(it)
                        }
                        val pageSize = appConfig.getClipListSize()
                        lastFilesLive = LivePagedListBuilder(qs.dataSource!!, pageSize).build().also { live ->
                            filesLive.addSource(live) {
                                emptyBlocksLive.postValue(emptyList())
                                filesLive.postValue(
                                    FileItemListData(
                                        _blocks = it,
                                        _scrollToTop = false,
                                        _snapshot = snapshot,
                                        _listConfig = listConfig,
                                        _stats = stats
                                    )
                                )
                                val count = qs.query!!.count()
                                filters.last.filesCount = count
                                stats.filesCount = count
                                updateEmptyState(stats)
                            }
                        }
                    }
                }
                .onErrorReturn { QuerySnapshot() }
                .map { it }
        }
    }

    private fun reloadClips(
        stats: ItemListStats,
        activeSnapshot: Filter.Snapshot,
        snapshot: Filter.Snapshot,
        listConfig: ListConfig,
        force: Boolean = false
    ): Single<QuerySnapshot<*, *>> {
        val scrollToTop = AtomicBoolean((force && !snapshot.isFolder()) || snapshot.sortBy != activeSnapshot.sortBy || snapshot.listStyle != activeSnapshot.listStyle)
        return Single
            .fromCallable {
                val query = clipBoxDao.getFiltered(snapshot)
                val dataSource = ObjectBoxDataSource.Factory(query)
                    .mapByPage {
                        val activeClip = clipboardState.clip.getValue()
                        var canBeActive = activeClip != null
                        it.map { clip ->
                            if (canBeActive) {
                                clip.isActive = canBeActive && (clip == activeClip || clip.text == activeClip?.text)
                                canBeActive = !clip.isActive
                                if (clip.isActive) {
                                    clipboardState.clip.setValue(clip)
                                }
                            }
                            map(clip, snapshot)
                        }
                    }
                mainState.clipsQuery.setValue(query)
                QuerySnapshot(
                    query = query,
                    dataSource = dataSource
                )
            }
            .doOnSuccess { qs ->
                onMain {
                    lastClipsLive?.let {
                        it.value?.dataSource?.invalidate()
                        clipsLive.removeSource(it)
                    }
                    val pageSize = appConfig.getClipListSize()
                    lastClipsLive = LivePagedListBuilder(qs.dataSource!!, pageSize).build().also { live ->
                        clipsLive.addSource(live) {
                            emptyBlocksLive.postValue(emptyList())
                            clipsLive.postValue(
                                ClipItemListData(
                                    _blocks = it,
                                    _scrollToTop = scrollToTop.getAndSet(false),
                                    _listConfig = listConfig,
                                    _snapshot = snapshot,
                                    _stats = stats
                                )
                            )
                            reloadClipsDisposable.disposeSilently()
                            if (clipBoxDao.consumeClipsCountChanged()) {
                                reloadClipsDisposable = onBackground {
                                    val isStarredFilter = lastFilter.isStarred()
                                    val isDeletedFilter = lastFilter.isDeleted()
                                    val isUntaggedFilter = lastFilter.isUntagged()
                                    val isSnippetsFilter = lastFilter.isSnippets()
                                    val isClipboardFilter = lastFilter.isClipboard()
                                    val textIsNull = lastFilter.textLike == null
                                    val filteredCount = qs.query!!.count()
                                    filters.untagged.notesCount = if (isUntaggedFilter && textIsNull) filteredCount else clipBoxDao.getUntaggedClipsCount()
                                    filters.snippets.notesCount = if (isSnippetsFilter && textIsNull) filteredCount else clipBoxDao.getSnippetClipsCount()
                                    filters.clipboard.notesCount = if (isClipboardFilter && textIsNull) filteredCount else clipBoxDao.getClipboardClipsCount()
                                    filters.deleted.notesCount = if (isDeletedFilter && textIsNull) filteredCount else clipBoxDao.getRecycleBinClipsCount()
                                    filters.starred.notesCount = if (isStarredFilter && textIsNull) filteredCount else clipBoxDao.getFavClipsCount()
                                    filters.all.notesCount = clipBoxDao.getAllClipsCount() - filters.deleted.notesCount
                                    filters.last.notesCount = filteredCount
                                    stats.notesCount = filteredCount
                                    updateEmptyState(stats)
                                    checkUserSessionAction.execute()
                                }
                            } else {
                                val count = qs.query!!.count()
                                filters.last.notesCount = count
                                stats.notesCount = count
                                updateEmptyState(stats)
                            }
                        }
                    }
                }
            }
            .onErrorReturn { QuerySnapshot() }
            .map { it }
    }

    private fun map(file: FileRef, snapshot: Filter.Snapshot): FileItemBlock<Unit> {
        return FileItemDefaultBlock(
            file = file,
            fileScreenHelper = fileScreenHelper,
            isSelectedGetter = mainState::isSelected,
            onFileClicked = this::onClick,
            onLongClick = this::onLongClick,
            onFileIconClicked = this::onIconClick,
            onFileChanged = this::onFileChanged,
            highlight = snapshot.textLike,
            listConfigGetter = this::getListConfig,
            flatMode = snapshot.isFolderFlat(),
            folderId = snapshot.folderIds.firstOrNull(),
            relativePathGetter = this::onGetRelativePath
        )
    }

    private fun onGetRelativePath(folderId: String?, fileRef: FileRef, callback: (path: String) -> Unit) {
        fileRepository.getRelativePath(folderId, fileRef)
            .observeOn(getViewScheduler())
            .subscribeBy(
                tag = "${folderId}_${fileRef.getUid()}",
                onSuccess = callback
            )
    }

    private fun onGetRelativePath(folderId: String?, clip: Clip, callback: (path: String) -> Unit) {
        clipRepository.getRelativePath(folderId, clip)
            .observeOn(getViewScheduler())
            .subscribeBy(
                tag = "${folderId}_${clip.firestoreId}",
                onSuccess = callback
            )
    }

    private fun map(clip: Clip, snapshot: Filter.Snapshot): ClipItemBlock<Unit> {
        val block: ClipItemBlock<Unit> = when (snapshot.listStyle) {
            ListStyle.DEFAULT -> ClipItemDefaultBlock(
                clip = clip,
                synced = !userState.isNotSynced(clip),
                isSelectedGetter = mainState::isSelected,
                textLike = snapshot.textLike,
                listConfigGetter = this::getListConfig,
                onLongClick = this::onLongClick,
                onClick = this::onClick,
                onCopy = this::onCopy
            )
            ListStyle.PREVIEW -> ClipItemPreviewBlock(
                clip = clip,
                synced = !userState.isNotSynced(clip),
                isSelectedGetter = mainState::isSelected,
                textLike = snapshot.textLike,
                listConfigGetter = this::getListConfig,
                onLongClick = this::onLongClick,
                onClick = this::onClick,
                onCopy = this::onCopy
            )
            ListStyle.COMFORTABLE -> ClipItemComfortableBlock(
                clip = clip,
                synced = !userState.isNotSynced(clip),
                isSelectedGetter = mainState::isSelected,
                textLike = snapshot.textLike,
                listConfigGetter = this::getListConfig,
                onLongClick = this::onLongClick,
                onClick = this::onClick,
                onCopy = this::onCopy
            )
            ListStyle.COMPACT -> ClipItemCompactBlock(
                clip = clip,
                synced = !userState.isNotSynced(clip),
                isSelectedGetter = mainState::isSelected,
                textLike = snapshot.textLike,
                listConfigGetter = this::getListConfig,
                onLongClick = this::onLongClick,
                onClick = this::onClick,
                onCopy = this::onCopy
            )
            ListStyle.GRID -> ClipItemGridBlock(
                clip = clip,
                synced = !userState.isNotSynced(clip),
                isSelectedGetter = mainState::isSelected,
                textLike = snapshot.textLike,
                listConfigGetter = this::getListConfig,
                onLongClick = this::onLongClick,
                onClick = this::onClick,
                onCopy = this::onCopy
            )
            ListStyle.FOLDERS -> return ClipItemFolderBlock(
                clip = clip,
                synced = !userState.isNotSynced(clip),
                isSelectedGetter = mainState::isSelected,
                textLike = snapshot.textLike,
                listConfigGetter = this::getListConfig,
                onLongClick = this::onLongClick,
                onClick = this::onClick,
                onCopy = this::onCopy,
                onFetchPreview = clipScreenHelper::onFetchPreview,
                onClipIconClicked = this::onIconClick,
                flatMode = snapshot.folderIds.size > 1,
                folderId = snapshot.folderIds.firstOrNull(),
                relativePathGetter = this::onGetRelativePath
            )
        }
        return block
    }

    private fun getListConfig() = mainState.listConfig.requireValue()

    private fun onFileChanged(callback: (file: FileRef) -> Unit) {
        fileChangeListeners.add(callback)
    }

    private fun onShowFolder(folder: FileRef) {
        fileScreenHelper.onShowFolder(folder)
    }

    private fun onOpenFolder(folder: FileRef?, detectFlatMode: Boolean = false) {
        if (mainState.hasSelectedObjects()) {
            mainState.clearSelection()
        }
        this.detectFlatMode.set(detectFlatMode)
        folder?.let { mainState.requestApplyFilter(folder, force = true, withTextLike = false) }
    }

    private fun onNewFolder() {
        val newFolder = fileMapper.createNewFolder()
        newFolder.folderId = appState.getActiveFolderId()
        folderState.requestOpenFolder(FolderRequest(
            withConfigurablePath = true,
            folderRef = newFolder,
            onConsumeFolder = {
                log("files :: consume new folder :: {}", it)
                onOpenFolder(it)
                true
            }
        ))
    }

    private fun onClick(file: FileRef) {
        if (mainState.getScreen().isContextScreen()) {
            viewModel.onSelect(file)
        } else {
            if (file.isFolder) {
                onOpenFolder(file)
            } else {
                mainState.filesQuery.getValue()?.let { query ->
                    Single.fromCallable { query.find().filter { !it.isFolder } }
                        .onErrorReturn { emptyList<FileRefBox>() }
                        .subscribeBy("viewFiles", appState) { files ->
                            fileUseCases.onView(file, files)
                        }
                }
            }
        }
    }

    private fun onIconClick(file: FileRef) {
        if (file.isFolder) {
            onShowFolder(file)
        } else {
            fileUseCases.onPreview(file, fileScreenHelper.getPreview(file))
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
        return if (mainState.getScreen().isContextScreen()) {
            viewModel.onSelect(clip)
            true
        } else {
            viewModel.noteUseCases.onViewNote(clip)
            false
        }
    }

    private fun onCopy(clip: Clip): Boolean {
        if (clip.isDeleted()) {
            viewModel.onUndoDelete(listOf(clip))
        } else {
            viewModel.onCopy(clip)
        }
        return true
    }

    private fun onLongClick(obj: AttributedObject): Boolean {
        val state = mainState.getScreen()
        if (state == ScreenState.STATE_MAIN) {
            viewModel.onSelect(obj)
            return true
        } else if (state.isContextScreen()) {
            viewModel.onSelectRange(obj)
        }
        return false
    }

    private data class QuerySnapshot<T, I>(
        val dataSource: DataSource.Factory<Int, I>? = null,
        val query: Query<T>? = null
    )

}