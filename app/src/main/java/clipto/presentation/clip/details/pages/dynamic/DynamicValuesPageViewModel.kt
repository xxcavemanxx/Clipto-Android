package clipto.presentation.clip.details.pages.dynamic

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.PagedList
import clipto.action.SaveClipAction
import clipto.common.misc.IdUtils
import clipto.common.presentation.mvvm.RxViewModel
import clipto.dao.objectbox.model.ClipBox
import clipto.domain.*
import clipto.dynamic.DynamicField
import clipto.dynamic.DynamicValuesFactory
import clipto.dynamic.fields.provider.IFieldProvider
import clipto.dynamic.fields.provider.SnippetValueProvider
import clipto.dynamic.presentation.field.DynamicFieldState
import clipto.dynamic.presentation.field.model.ResultCode
import clipto.extensions.from
import clipto.presentation.blocks.*
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.clip.details.ClipDetailsState
import clipto.presentation.clip.ClipScreenHelper
import clipto.presentation.common.recyclerview.BlockItem
import clipto.store.app.AppState
import clipto.store.clip.ClipState
import clipto.store.main.MainState
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DynamicValuesPageViewModel @Inject constructor(
    app: Application,
    appState: AppState,
    val mainState: MainState,
    val clipState: ClipState,
    private val state: ClipDetailsState,
    private val factory: DynamicValuesFactory,
    private val saveClipAction: SaveClipAction,
    private val dynamicFieldState: DynamicFieldState,
    private val snippetValueProvider: SnippetValueProvider,
    private val clipScreenHelper: ClipScreenHelper
) : RxViewModel(app) {

    val settings = mainState.getSettings()
    val clipsLive:LiveData<PagedList<Clip>> = clipScreenHelper.getClipsLive()
    val filter by lazy { Filter.Snapshot().copy(appState.getFilterBySnippets()).copy(pinSnippets = true, snippets = false) }
    val listConfig by lazy { mainState.getListConfig().copy(appState.getFilterBySnippets()).copy(listStyle = ListStyle.PREVIEW) }

    private val viewModeLive by lazy { MutableLiveData(ViewMode.valueOf(state.selectedTab.requireValue())) }

    val actionsLive = MutableLiveData<List<BlockItem<DynamicValuesPageFragment>>>()

    val blocksLive: LiveData<List<BlockItem<DynamicValuesPageFragment>>> by lazy {
        Transformations.map(viewModeLive) { viewMode ->
            viewMode!!
            val blocks = mutableListOf<BlockItem<DynamicValuesPageFragment>>()
            blocks.add(SpaceBlock(heightInDp = 16))
            blocks.add(createViewModeBlock(viewMode))

            when (viewMode) {
                ViewMode.VALUES -> {
                    clearClips()
                    factory.getDynamicValueProviders().forEach { fp ->
                        blocks.add(
                            DynamicFieldBlock(
                                provider = fp,
                                onClicked = ::onDynamicFieldClicked
                            )
                        )
                        blocks.add(SeparatorVerticalBlock(marginHoriz = 0))
                    }
                    blocks.add(SpaceBlock(heightInDp = 16))
                }
                ViewMode.FIELDS -> {
                    clearClips()
                    factory.getDynamicFieldProviders().forEach { fp ->
                        blocks.add(
                            DynamicFieldBlock(
                                provider = fp,
                                onClicked = ::onDynamicFieldClicked
                            )
                        )
                        blocks.add(SeparatorVerticalBlock(marginHoriz = 0))
                    }
                    blocks.add(SpaceBlock(heightInDp = 16))
                }
                ViewMode.SNIPPETS -> {
                    blocks.add(SpaceBlock(heightInDp = 12))
                    blocks.add(TextInputLayoutBlock(
                        text = getSearchByText(),
                        hint = string(R.string.clip_add_snippet_new_hint),
                        onTextChanged = { text ->
                            onSearch(text?.toString())
                            null
                        }
                    ))
                    blocks.add(SpaceBlock(heightInDp = 12))
                    onSearch()
                }
            }

            blocks
        }
    }

    fun getSearchByText() = clipScreenHelper.getSearchByText()

    fun onSnippet(clip: Clip) {
        val snippetId = clip.snippetId
        if (snippetId != null) {
            onInsertSnippet(clip)
        } else {
            val newClip = ClipBox().apply(clip)
            val newSnippetId = newClip.firestoreId ?: IdUtils.autoId()
            newClip.snippetId = newSnippetId
            newClip.snippet = true
            saveClipAction.execute(newClip, withLoadingState = true, withSilentValidation = true) {
                onInsertSnippet(it)
            }
        }
    }

    private fun onCreateSnippet() {
        getSearchByText()?.let { text ->
            val clip = Clip.from(text)
            onSnippet(clip)
        }
    }

    private fun onInsertSnippet(clip: Clip) {
        val snippetId = clip.snippetId
        val params = mapOf(
            DynamicField.ATTR_SNIPPET_REF to snippetId,
            DynamicField.ATTR_LABEL to clip.title,
            DynamicField.ATTR_VALUE to clip.text,
        )
        onDynamicFieldClicked(snippetValueProvider, params)
    }

    private fun onDynamicFieldClicked(provider: IFieldProvider<out DynamicField>, params: Map<String, Any?> = emptyMap()) {
        val flow =
            if (clipState.activeFocus.getValue() == FocusMode.TEXT) {
                dynamicFieldState.requestInsertField(provider.createField(params))
            } else {
                dynamicFieldState.requestCopyField(provider.createField(params))
            }
        flow
            .filter { it.resultCode == ResultCode.INSERT || it.resultCode == ResultCode.COPY }
            .map { it.field }
            .map { field -> factory.getFieldProvider(field).createPlaceholder(field) }
            .subscribeBy("onDynamicFieldClicked") { onDynamicValue(it) }
    }

    private fun onSearch(textToSearchBy: String? = getSearchByText()) {
        clipScreenHelper.onSearch(filter, textToSearchBy)
        if (textToSearchBy.isNullOrBlank()) {
            actionsLive.postValue(emptyList())
        } else {
            actionsLive.postValue(listOf(
                PrimaryButtonBlock(
                    titleRes = R.string.button_create,
                    clickListener = { onCreateSnippet() }
                ),
                SpaceBlock(heightInDp = 6)
            ))
        }
    }

    private fun clearClips() {
        clipScreenHelper.onClearSearch(postEmptyState = true)
        actionsLive.postValue(emptyList())
    }

    private fun createViewModeBlock(viewMode: ViewMode): ThreeButtonsToggleBlock<DynamicValuesPageFragment> {
        return ThreeButtonsToggleBlock(
            firstButtonTextRes = R.string.clip_details_tab_dynamic_values,
            secondButtonTextRes = R.string.clip_details_tab_dynamic_fields,
            thirdButtonTextRes = R.string.clip_details_tab_snippets,
            onFirstButtonClick = { onChangeViewMode(ViewMode.VALUES) },
            onSecondButtonClick = { onChangeViewMode(ViewMode.FIELDS) },
            onThirdButtonClick = { onChangeViewMode(ViewMode.SNIPPETS) },
            selectedButtonIndex = viewMode.position
        )
    }

    private fun onChangeViewMode(viewMode: ViewMode) {
        state.selectedTab.setValue(viewMode.tab)
        viewModeLive.postValue(viewMode)
    }

    private fun onDynamicValue(value: String) {
        state.dynamicValue.setValue(value, force = true)
    }

}