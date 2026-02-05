package clipto.presentation.main.nav

import android.app.Application
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import clipto.action.SaveFilterAction
import clipto.common.misc.IntentUtils
import clipto.common.presentation.mvvm.RxViewModel
import clipto.config.IAppConfig
import clipto.dao.objectbox.FileBoxDao
import clipto.domain.FileRef
import clipto.domain.Filter
import clipto.domain.Filters
import clipto.domain.MainAction
import clipto.domain.factory.FileRefFactory
import clipto.domain.factory.FilterFactory
import clipto.extensions.normalize
import clipto.presentation.blocks.OutlinedButtonBlock
import clipto.presentation.blocks.TextButtonBlock
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.main.nav.blocks.FilterBlock
import clipto.presentation.main.nav.blocks.FilterGroupBlock
import clipto.presentation.main.nav.blocks.HeaderBlock
import clipto.presentation.snippets.details.SnippetKitDetailsViewModel
import clipto.presentation.usecases.MainActionUseCases
import clipto.repository.IFileRepository
import clipto.repository.ISettingsRepository
import clipto.store.app.AppState
import clipto.store.filter.FilterDetailsState
import clipto.store.filter.FilterState
import clipto.store.folder.FolderState
import clipto.store.internet.InternetState
import clipto.store.main.MainState
import clipto.store.user.UserState
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainNavViewModel @Inject constructor(
    app: Application,
    filterState: FilterState,
    val mainState: MainState,
    val userState: UserState,
    val appConfig: IAppConfig,
    private val appState: AppState,
    private val fileBoxDao: FileBoxDao,
    private val dialogState: DialogState,
    private val folderState: FolderState,
    private val internetState: InternetState,
    private val fileRepository: IFileRepository,
    private val filterDetailsState: FilterDetailsState,
    private val saveFilterAction: SaveFilterAction,
    private val settingsRepository: ISettingsRepository,
    private val mainActionUseCases: MainActionUseCases
) : RxViewModel(app) {

    val requestUpdateFilter = filterState.requestUpdateFilter.getLiveData()

    var currentBlocks: MutableList<BlockItem<MainNavFragment>> = mutableListOf()

    private val filerBlocksLive by lazy {
        val liveData = MutableLiveData<List<BlockItem<MainNavFragment>>>()

        val appGuideBlock = TextButtonBlock<MainNavFragment>(
            titleRes = R.string.app_guide,
            clickListener = {
                internetState.withInternet(
                    success = {
                        val args = SnippetKitDetailsViewModel.buildArgs(appConfig.getAppGuideId())
                        appState.requestNavigateTo(R.id.action_snippet_kit_details, args)
                    }
                )
            }
        )

        appState.filters.getLiveChanges()
            .observeOn(getBackgroundScheduler())
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .subscribeBy { filters ->
                val settings = appState.getSettings()

                val authorized = userState.isAuthorized()

                val blocks = mutableListOf<BlockItem<MainNavFragment>>()

                val snippetsCollapsed = settings.filterGroupSnippetsCollapsed
                val filtersCollapsed = settings.filterGroupFiltersCollapsed
                val foldersCollapsed = settings.filterGroupFoldersCollapsed
                val notesCollapsed = settings.filterGroupNotesCollapsed
                val tagsCollapsed = settings.filterGroupTagsCollapsed
                val filesCollapsed = settings.filterGroupFilesCollapsed

                // header
                blocks.add(HeaderBlock(this))

                // notes
                blocks.add(
                    FilterGroupBlock(
                        viewModel = this,
                        expanded = !notesCollapsed,
                        filter = filters.groupNotes,
                        limit = userState.getSyncLimit().takeIf { authorized },
                        onClick = this::onNotesClicked,
                        onActionClick = this::onNewNote
                    )
                )
                if (!notesCollapsed) {
                    val manual = filters.groupNotes.isManualSorting()
                    val categories = filters.getSortedCategories()
                    categories.forEach { category ->
                        blocks.add(
                            FilterBlock(
                                viewModel = this,
                                manualSort = manual,
                                filter = category
                            )
                        )
                    }
                    if (categories.isNotEmpty()) {
                        withSeparator(blocks)
                    }
                }

//        // files
//        blocks.add(
//            FilterGroupBlock(
//                viewModel = this,
//                expanded = !filesCollapsed,
//                filter = filters.groupFiles,
//                onClick = this::onFilesClicked,
//                onActionClick = this::onNewFile
//            )
//        )

                // tags
                blocks.add(
                    FilterGroupBlock(
                        viewModel = this,
                        expanded = !tagsCollapsed,
                        filter = filters.groupTags,
                        onClick = this::onTagsClicked,
                        onActionClick = this::onNewTag,
                    )
                )
                if (!tagsCollapsed) {
                    val manual = filters.groupTags.isManualSorting()
                    val tags = filters.getSortedTags()
                    tags.forEach { tag ->
                        blocks.add(
                            FilterBlock(
                                viewModel = this,
                                manualSort = manual,
                                filter = tag
                            )
                        )
                    }
                    if (tags.isEmpty()) {
                        withSpace(blocks)
                        blocks.add(
                            OutlinedButtonBlock(
                                titleRes = R.string.main_action_organize_tag_title,
                                clickListener = { onNewTag() }
                            )
                        )
                        withSpace(blocks)
                    } else {
                        withSeparator(blocks)
                    }
                }

                // folders
                val folders = fileBoxDao.getByFavEqAndFolderEq(fav = true, isFolder = true)
                filters.groupFolders.notesCount = folders.size.toLong()

                blocks.add(
                    FilterGroupBlock(
                        viewModel = this,
                        expanded = !foldersCollapsed,
                        filter = filters.groupFolders,
                        onClick = this::onFoldersClicked,
                        onActionClick = this::onNewFolder
                    )
                )
                if (!foldersCollapsed) {
                    if (!authorized) {
                        withSpace(blocks)
                        blocks.add(
                            OutlinedButtonBlock(
                                titleRes = R.string.main_action_organize_folder_title,
                                clickListener = { onNewFolder() }
                            )
                        )
                        withSpace(blocks)
                    } else {
                        val foldersFilters = folders
                            .map { FilterFactory.createViewFolder(it, filters.folders) }
                            .let { Filters.sorted(filters.groupFolders, it) }
                        if (foldersFilters.isNotEmpty()) {
                            val isManualSorting = filters.groupFolders.isManualSorting()
                            foldersFilters.forEach { filter ->
                                blocks.add(
                                    FilterBlock(
                                        viewModel = this,
                                        manualSort = isManualSorting,
                                        filter = filter,
                                        count = null
                                    )
                                )
                            }
                        }

                        withSpace(blocks)
                        blocks.add(
                            OutlinedButtonBlock(
                                titleRes = R.string.folder_root,
                                clickListener = { onApplyFolder(FileRefFactory.root()) }
                            )
                        )
                        withSpace(blocks)
                    }
                }

                // filters
                blocks.add(
                    FilterGroupBlock(
                        viewModel = this,
                        expanded = !filtersCollapsed,
                        filter = filters.groupFilters,
                        onClick = this::onFiltersClicked,
                        onActionClick = this::onNewFilter
                    )
                )
                if (!filtersCollapsed) {
                    val manual = filters.groupFilters.isManualSorting()
                    val namedFilters = filters.getSortedNamedFilters()
                    namedFilters.forEach { named ->
                        blocks.add(
                            FilterBlock(
                                viewModel = this,
                                manualSort = manual,
                                filter = named,
                                count = null
                            )
                        )
                    }
                    if (namedFilters.isEmpty()) {
                        withSpace(blocks)
                        blocks.add(
                            OutlinedButtonBlock(
                                titleRes = R.string.main_action_organize_filter_title,
                                clickListener = { onNewFilter() }
                            )
                        )
                        withSpace(blocks)
                    } else {
                        withSeparator(blocks)
                    }
                }

                // snippets
                blocks.add(
                    FilterGroupBlock(
                        viewModel = this,
                        expanded = !snippetsCollapsed,
                        filter = filters.groupSnippets,
                        onClick = this::onSnippetsClicked,
                        onActionClick = this::onNewSnippetKit,
                    )
                )
                if (!snippetsCollapsed) {
                    val manual = filters.groupSnippets.isManualSorting()
                    val snippets = filters.getSortedSnippetKits()
                    log("FilterRepository :: refresh snippets :: {}", snippets)
                    snippets.forEach { set ->
                        blocks.add(
                            FilterBlock(
                                viewModel = this,
                                manualSort = manual,
                                filter = set
                            )
                        )
                    }
                    if (appConfig.isSnippetsPublicLibraryAvailable()) {
                        withSpace(blocks)
                        blocks.add(
                            OutlinedButtonBlock(
                                title = string(R.string.snippet_kit_public_library),
                                clickListener = { onPublicSnippetLibrary() }
                            )
                        )
                        withSpace(blocks)
                    }
                }

                if (appConfig.getAppGuideId().isNotBlank()) {
                    blocks.add(SpaceBlock.xxs())
                    blocks.add(appGuideBlock)
                }

                currentBlocks = blocks

                log("main nav :: update :: {} - {}", blocks.size, Thread.currentThread())

                liveData.postValue(blocks)
            }

        liveData
    }

    fun getFiltersLive(): LiveData<List<BlockItem<MainNavFragment>>> = filerBlocksLive

    fun getFilters() = appState.getFilters()

    fun getSettings() = appState.getSettings()

    fun isActive(filter: Filter): Boolean = getFilters().isActive(filter)

    fun onApplyFilter(filter: Filter) {
        mainState.requestApplyFilter(filter.normalize(), force = true)
    }

    fun onOpenFilter(filter: Filter) {
        if (filter.isFolder()) {
            fileRepository.getByUid(filter.folderId)
                .doOnError { dialogState.showError(it) }
                .subscribeBy("onOpenFolder", folderState::requestOpenFolder)
        } else {
            filterDetailsState.requestOpenFilter(filter)
        }
    }

    private fun onApplyFolder(folder: FileRef) {
        mainState.requestApplyFilter(folder, force = true)
    }

    fun onMoved(filter: Filter) {
        val filterItems = currentBlocks.filterIsInstance<FilterBlock>().map { it.filter }
        val group = getFilters().findGroup(filter)
        val filtersInGroup = when {
            filter.isTag() -> {
                filterItems.filter { it.isTag() }
            }
            filter.isSnippetKit() -> {
                filterItems.filter { it.isSnippetKit() }
            }
            filter.isNamedFilter() -> {
                filterItems.filter { it.isNamedFilter() }
            }
            filter.isNotesCategory() -> {
                filterItems.filter { it.isNotesCategory() }
            }
            filter.isFolder() -> {
                filterItems.filter { it.isFolder() }
            }
            else -> {
                null
            }
        }
        if (filtersInGroup != null && group != null) {
            var tagIds = filtersInGroup.mapNotNull { it.uid }
            if (group.sortBy.desc) {
                tagIds = tagIds.reversed()
            }
            group.tagIds = tagIds
            saveFilterAction.execute(group)
        }
    }

    private fun withSeparator(blocks: MutableList<BlockItem<MainNavFragment>>) {
        blocks.add(SeparatorVerticalBlock(marginVert = 8, marginHoriz = 0))
    }

    private fun withSpace(blocks: MutableList<BlockItem<MainNavFragment>>) {
        blocks.add(SpaceBlock(8))
    }

    private fun onNewNote(fragment: Fragment) {
        fragment.activity?.let { act ->
            mainActionUseCases.onAction(MainAction.NOTE_NEW, act)
        }
    }

    private fun onNewTag(fragment: Fragment) {
        fragment.activity?.let { act ->
            mainActionUseCases.onAction(MainAction.TAG_NEW, act)
        }
    }

    private fun onNewTag() {
        mainActionUseCases.onAction(MainAction.TAG_NEW)
    }

    private fun onNewFilter(fragment: Fragment) {
        fragment.activity?.let { act ->
            mainActionUseCases.onAction(MainAction.FILTER_NEW, act)
        }
    }

    private fun onNewFilter() {
        mainActionUseCases.onAction(MainAction.FILTER_NEW)
    }

    private fun onNewSnippetKit(fragment: Fragment) {
        fragment.activity?.let { act ->
            mainActionUseCases.onAction(MainAction.SNIPPET_KIT_NEW, act)
        }
    }

    private fun onNewFile(fragment: Fragment) {
        fragment.activity?.let { act ->
            mainActionUseCases.onAction(MainAction.FILE_SELECT, act)
        }
    }

    private fun onNewFolder(fragment: Fragment) {
        fragment.activity?.let { act ->
            mainActionUseCases.onAction(MainAction.FOLDER_NEW, act)
        }
    }

    private fun onNewFolder() {
        mainActionUseCases.onAction(MainAction.FOLDER_NEW)
    }

    private fun onPublicSnippetLibrary() {
        internetState.withInternet(
            success = {
                if (appConfig.isSnippetsPublicLibraryAuthRequired()) {
                    userState.withAuth {
                        appState.requestNavigateTo(R.id.action_snippet_kit_library)
                    }
                } else {
                    appState.requestNavigateTo(R.id.action_snippet_kit_library)
                }
            }
        )
    }

    private fun onNotesClicked() {
        val settings = appState.getSettings()
        settings.filterGroupNotesCollapsed = !settings.filterGroupNotesCollapsed
        onSaveSettings()
    }

    private fun onTagsClicked() {
        val settings = appState.getSettings()
        settings.filterGroupTagsCollapsed = !settings.filterGroupTagsCollapsed
        onSaveSettings()
    }

    private fun onFiltersClicked() {
        val settings = appState.getSettings()
        settings.filterGroupFiltersCollapsed = !settings.filterGroupFiltersCollapsed
        onSaveSettings()
    }

    private fun onFilesClicked() {
        val settings = appState.getSettings()
        settings.filterGroupFilesCollapsed = !settings.filterGroupFilesCollapsed
        onSaveSettings()
    }

    private fun onFoldersClicked() {
        val settings = appState.getSettings()
        settings.filterGroupFoldersCollapsed = !settings.filterGroupFoldersCollapsed
        onSaveSettings()
    }

    private fun onSnippetsClicked() {
        val settings = appState.getSettings()
        settings.filterGroupSnippetsCollapsed = !settings.filterGroupSnippetsCollapsed
        onSaveSettings()
    }

    private fun onSaveSettings() {
        settingsRepository.update(getSettings())
            .subscribeBy("onSaveSettings")
        appState.refreshFilters()
    }

}