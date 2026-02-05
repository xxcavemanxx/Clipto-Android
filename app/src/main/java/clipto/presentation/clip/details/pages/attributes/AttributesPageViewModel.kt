package clipto.presentation.clip.details.pages.attributes

import android.app.Application
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import clipto.common.presentation.mvvm.RxViewModel
import clipto.domain.Filter
import clipto.extensions.getIconColor
import clipto.extensions.getIconRes
import clipto.presentation.blocks.CheckableOptionBlock
import clipto.presentation.blocks.OutlinedButtonBlock
import clipto.presentation.blocks.PrimaryButtonBlock
import clipto.presentation.blocks.ThreeButtonsToggleBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.clip.details.ClipDetailsState
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.file.FileScreenHelper
import clipto.store.app.AppState
import clipto.store.filter.FilterDetailsState
import clipto.store.filter.FilterState
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AttributesPageViewModel @Inject constructor(
    app: Application,
    private val appState: AppState,
    private val state: ClipDetailsState,
    private val filterState: FilterState,
    private val fileScreenHelper: FileScreenHelper,
    private val filterDetailsState: FilterDetailsState
) : RxViewModel(app) {

    val viewModeLive by lazy { MutableLiveData(ViewMode.valueOf(state.selectedTab.requireValue())) }

    private val tagIdsLive by lazy {
        val liveData = state.tags.getMutableLiveData()
        val filters = appState.getFilters()
        var tags = filters.getTags()
        filterState.requestUpdateFilter.getLiveChanges()
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .filter { it.isTag() }
            .filter { !it.isNew() }
            .subscribeBy {
                when {
                    !tags.contains(it) && !state.tags.requireValue().contains(it.uid) -> onTagClicked(it)
                    else -> liveData.value?.let { liveData.postValue(it) }
                }
                filterState.requestUpdateFilter.clearValue()
                tags = filters.getTags()
            }
        liveData
    }

    private val snippetKitsLive by lazy {
        val liveData = state.snippetKits.getMutableLiveData()
        val filters = appState.getFilters()
        var kits = filters.getSnippetKits()
        filterState.requestUpdateFilter.getLiveChanges()
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .filter { it.isSnippetKit() }
            .filter { !it.isNew() }
            .subscribeBy {
                when {
                    !kits.contains(it) && !state.snippetKits.requireValue().contains(it.uid) -> onTagClicked(it)
                    else -> liveData.value?.let { liveData.postValue(it) }
                }
                filterState.requestUpdateFilter.clearValue()
                kits = filters.getSnippetKits()
            }
        liveData
    }

    val tagBlocksLive: LiveData<List<BlockItem<Fragment>>> by lazy {
        Transformations.map(tagIdsLive) { tagIds ->
            val filters = appState.getFilters()
            val tags = filters.getSortedTags()
            val blocks = mutableListOf<BlockItem<Fragment>>()
            blocks.add(SpaceBlock(heightInDp = 16))
            blocks.add(createViewModeBlock(ViewMode.TAGS))
            if (tags.isNotEmpty()) {
                val onClicked: (filter: Filter) -> Unit = { onTagClicked(it) }
                blocks.addAll(
                    tags
                        .map<Filter, CheckableOptionBlock<Filter, Fragment>> { tag ->
                            CheckableOptionBlock(
                                option = CheckableOptionBlock.Option(
                                    checked = tagIds.contains(tag.uid),
                                    iconColor = tag.getIconColor(app),
                                    iconRes = tag.getIconRes(),
                                    title = tag.name,
                                    model = tag
                                ),
                                onClicked = onClicked
                            )
                        }
                        .sortedByDescending { it.option.checked }
                )
                blocks.add(SpaceBlock(heightInDp = 12))
                blocks.add(
                    OutlinedButtonBlock(
                        titleRes = R.string.main_action_organize_tag_title,
                        clickListener = { filterDetailsState.requestNewTag() }
                    )
                )
            } else {
                blocks.add(SpaceBlock(heightInDp = 12))
                blocks.add(PrimaryButtonBlock(
                    titleRes = R.string.main_action_organize_tag_title,
                    clickListener = { filterDetailsState.requestNewTag() }
                ))
            }
            blocks
        }
    }

    val kitBlocksLive: LiveData<List<BlockItem<Fragment>>> by lazy {
        Transformations.map(snippetKitsLive) { kitIds ->
            val filters = appState.getFilters()
            val kits = filters.getSortedSnippetKits()
            val blocks = mutableListOf<BlockItem<Fragment>>()
            blocks.add(SpaceBlock(heightInDp = 16))
            blocks.add(createViewModeBlock(ViewMode.KITS))
            if (kits.isNotEmpty()) {
                val onClicked: (filter: Filter) -> Unit = { onKitClicked(it) }
                blocks.addAll(
                    kits
                        .map<Filter, CheckableOptionBlock<Filter, Fragment>> { kit ->
                            CheckableOptionBlock(
                                option = CheckableOptionBlock.Option(
                                    checked = kitIds.contains(kit.uid),
                                    iconColor = kit.getIconColor(app),
                                    iconRes = kit.getIconRes(),
                                    title = kit.name,
                                    model = kit
                                ),
                                onClicked = onClicked
                            )
                        }
                        .sortedByDescending { it.option.checked }
                )
                blocks.add(SpaceBlock(heightInDp = 12))
                blocks.add(OutlinedButtonBlock(
                    titleRes = R.string.main_action_snippets_set_title,
                    clickListener = { filterDetailsState.requestNewSnippetKit() }
                ))
            } else {
                blocks.add(SpaceBlock(heightInDp = 12))
                blocks.add(PrimaryButtonBlock(
                    titleRes = R.string.main_action_snippets_set_title,
                    clickListener = { filterDetailsState.requestNewSnippetKit() }
                ))
            }
            blocks
        }
    }

    val folderBlocksLive: LiveData<List<BlockItem<Fragment>>> by lazy {
        val liveData = MutableLiveData<List<BlockItem<Fragment>>>()
        var rebind: () -> Unit = {}
        rebind = {
            fileScreenHelper.onSelectFolder(
                id = state.folderId.getValue(),
                onSelected = { folderId ->
                    state.folderId.setValue(folderId)
                    rebind()
                },
                onBlocksReady = { foldersBlocks ->
                    val blocks = mutableListOf<BlockItem<Fragment>>()
                    blocks.add(SpaceBlock(heightInDp = 16))
                    blocks.add(createViewModeBlock(ViewMode.FOLDER))
                    blocks.addAll(foldersBlocks)
                    liveData.postValue(blocks)
                },
                withNewFolder = true,
                withBottomSpace = false
            )
        }
        rebind()
        liveData
    }

    private fun createViewModeBlock(viewMode: ViewMode): BlockItem<Fragment> {

        return ThreeButtonsToggleBlock(
            firstButtonTextRes = R.string.clip_details_tab_tags,
            secondButtonTextRes = R.string.clip_details_tab_folder,
            thirdButtonTextRes = R.string.clip_details_tab_snippet_kits,
            onFirstButtonClick = { onViewModeChanged(ViewMode.TAGS) },
            onSecondButtonClick = { onViewModeChanged(ViewMode.FOLDER) },
            onThirdButtonClick = { onViewModeChanged(ViewMode.KITS) },
            selectedButtonIndex = viewMode.position
        )
    }

    private fun onViewModeChanged(viewMode: ViewMode) {
        state.selectedTab.setValue(viewMode.tab)
        viewModeLive.postValue(viewMode)
    }

    private fun onTagClicked(tag: Filter) {
        val clip = state.clipDetails.getValue()?.clip ?: return
        val tagId = tag.uid ?: return
        var excludedTagIds = clip.excludedTagIds
        var tagIds = state.tags.requireValue()
        if (tagIds.contains(tagId)) {
            tagIds = tagIds.minus(tagId)
            excludedTagIds = excludedTagIds.plus(tagId)
        } else {
            tagIds = tagIds.plus(tagId)
            excludedTagIds = excludedTagIds.minus(tagId)
        }
        clip.excludedTagIds = excludedTagIds
        state.tags.setValue(tagIds)
    }

    private fun onKitClicked(kit: Filter) {
        val kitId = kit.uid ?: return
        var kitIds = state.snippetKits.requireValue()
        kitIds =
            if (kitIds.contains(kitId)) {
                kitIds.minus(kitId)
            } else {
                kitIds.plus(kitId)
            }
        state.snippetKits.setValue(kitIds)
    }

}