package clipto.presentation.clip.tags

import android.app.Application
import androidx.fragment.app.Fragment
import clipto.domain.Filter
import clipto.extensions.createTag
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.blocks.domain.TagsFlowBlock
import clipto.presentation.blocks.TextAutoCompleteBlock
import clipto.presentation.common.fragment.blocks.BlocksViewModel
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.widget.AutoCompleteItem
import clipto.repository.IClipRepository
import clipto.repository.IFilterRepository
import clipto.utils.DomainUtils
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CommonTagsViewModel @Inject constructor(
    app: Application,
    private val clipRepository: IClipRepository,
    private val filterRepository: IFilterRepository
) : BlocksViewModel(app) {

    val selectedClips by lazy { mainState.getSelectedObjects() }
    private val selectedTags by lazy { DomainUtils.getCommonTagIds(selectedClips).toMutableSet() }
    private val allTags by lazy { appState.getFilters().getSortedTags().map { AutoCompleteItem(it.uid, it.name) } }

    override fun doCreate() {
        updateBlocks()
    }

    private fun updateBlocks() {
        val blocks = mutableListOf<BlockItem<Fragment>>()

        blocks.add(SpaceBlock(heightInDp = 16))

        blocks.add(
            TextAutoCompleteBlock(
                hintRes = R.string.clip_hint_tags,
                maxLength = appConfig.maxLengthTag(),
                actionIconRes = R.drawable.filter_tag_outline,
                actionTitleRes = R.string.clip_button_tag_create,
                selectedItems = selectedTags.toList(),
                allItems = allTags.toList(),
                onSelectListener = { tagName ->
                    onAddTag(tagName) {
                        it.uid?.let(selectedTags::add)
                        updateBlocks()
                    }
                }
            )
        )

        blocks.add(SpaceBlock(heightInDp = 16))

        blocks.add(TagsFlowBlock(
            tags = appState.getFilters().getSortedTags(),
            selectedTagIds = selectedTags.toList(),
            onClicked = { tag ->
                tag.uid?.let { uid ->
                    if (selectedTags.contains(uid)) {
                        selectedTags.remove(uid)
                    } else {
                        selectedTags.add(uid)
                    }
                    updateBlocks()
                }
            }
        ))

        blocks.add(SpaceBlock(heightInDp = 16))

        postBlocks(blocks)
    }

    fun oAssignTags() {
        mainState.getSelectedClips().toList().let {
            clipRepository.tagAll(it, selectedTags.toList())
                .doOnSuccess { mainState.clearSelection() }
                .subscribeBy("oAssignTags", appState) { dismiss() }
        }
    }

    private fun onAddTag(tagName: String, callback: (filter: Filter) -> Unit) {
        val filter = appState.getFilters().findFilterByTagName(tagName)
        if (filter != null) {
            callback.invoke(filter)
        } else {
            val tagFilter = Filter.createTag(tagName)
            filterRepository.save(tagFilter)
                .subscribeBy("onCreateTag") { callback.invoke(it) }
        }
    }

}