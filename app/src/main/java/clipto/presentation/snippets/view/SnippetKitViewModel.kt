package clipto.presentation.snippets.view

import android.app.Application
import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import clipto.AppContext
import clipto.domain.*
import clipto.dynamic.DynamicTextHelper
import clipto.dynamic.FormField
import clipto.dynamic.presentation.field.DynamicFieldState
import clipto.presentation.blocks.ux.ZeroStateHorizontalBlock
import clipto.presentation.clip.view.blocks.AttachmentsBlock
import clipto.presentation.clip.view.blocks.TextBlock
import clipto.presentation.common.fragment.attributed.AttributedObjectViewModel
import clipto.presentation.common.fragment.attributed.blocks.DescriptionBlock
import clipto.presentation.common.fragment.attributed.blocks.TitleBlock
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.usecases.FileUseCases
import clipto.repository.ISnippetRepository
import clipto.store.clip.ClipScreenState
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SnippetKitViewModel @Inject constructor(
    app: Application,
    val dynamicTextHelper: DynamicTextHelper,
    private val fileUseCases: FileUseCases,
    private val savedStateHandle: SavedStateHandle,
    private val dynamicFieldState: DynamicFieldState,
    private val snippetRepository: ISnippetRepository
) : AttributedObjectViewModel<Clip, ClipScreenState>(app) {

    private val snippetKit: SnippetKit by lazy { savedStateHandle.get(ATTR_SNIPPET_KIT)!! }
    private val snippetIndex: Int by lazy { savedStateHandle.get(ATTR_SNIPPET_INDEX)!! }
    private val snippets: List<Snippet> by lazy { snippetKit.getSortedSnippets() }
    private val snippetsDetails = mutableMapOf<String, SnippetDetails>()
    private val clipScreenStateLive = MutableLiveData<ClipScreenState>()

    override fun doCreate() {
        super.doCreate()
        onNavigate(snippetIndex)
    }

    override fun onCreateBlocks(from: ClipScreenState?, blocksCallback: (blocks: List<BlockItem<Fragment>>) -> Unit) {
        val screenState = from ?: return blocksCallback.invoke(emptyList())
        val clip = screenState.value
        val blocks = mutableListOf<BlockItem<Fragment>>()
        val files = snippetsDetails[clip.snippetId]?.files ?: emptyList()
        val showAdditionalAttrs = getSettings().noteShowAdditionalAttributes
        val showPreview = clip.textType == TextType.LINK && linkPreviewState.canShowPreview.requireValue()

        // TITLE
        blocks.add(createTitleBlock(screenState, showAdditionalAttrs))

        // DESCRIPTION
        if (showAdditionalAttrs) {
            blocks.add(createDescriptionBlock(screenState))
        }

        // FILES
        if (files.isNotEmpty()) {
            blocks.add(createAttachmentsBlock(screenState, files))
        } else if (clip.hasFiles()) {
            blocks.add(ZeroStateHorizontalBlock())
        }

        // TEXT
        blocks.add(createTextBlock(screenState, showPreview))

        blocksCallback.invoke(blocks)
    }

    override fun createScreenStateLive(): MutableLiveData<ClipScreenState> = clipScreenStateLive
    override fun onUpdateState() = clipScreenStateLive.postValue(clipScreenStateLive.value)
    override fun onInitNavigator(callback: (index: Int) -> Unit) = callback.invoke(snippetIndex)
    override fun getNavigatorMaxValue(): Int = snippets.size
    override fun hasNavigator(): Boolean = snippets.size > 1

    override fun onNavigate(index: Int) {
        snippets.getOrNull(index)?.let { snippet ->
            log("onNavigate :: {} -> {}", index, snippet.id)
            val clip = snippet.asClip(snippetKit.id)
            clipScreenStateLive.postValue(
                ClipScreenState(
                    value = clip,
                    viewMode = ViewMode.VIEW,
                    focusMode = FocusMode.NONE,
                    title = snippetKit.name
                )
            )

            if (snippet.fileIds.isNotEmpty() && !snippetsDetails.containsKey(snippet.id)) {
                snippetRepository.getSnippetDetails(snippet).subscribeBy("getSnippetDetails") { details ->
                    log("getSnippetDetails :: {}", details.files)
                    snippetsDetails[details.snippetId] = details
                    clipScreenStateLive.value?.takeIf { it.value.snippetId == details.snippetId }?.let { state ->
                        if (details.files.isNotEmpty()) {
                            clipScreenStateLive.postValue(state)
                        }
                    }
                }
            }

            savedStateHandle.set(ATTR_SNIPPET_INDEX, snippetIndex)
        }
    }

    fun onCopy() {
        clipScreenStateLive.value?.value?.let { clip ->
            AppContext.get().onCopy(
                clip,
                saveCopied = !clip.isDeleted()
            )
        }
    }

    private fun createTitleBlock(screenState: ClipScreenState, showAdditionalAttrs: Boolean) =
        TitleBlock(
            screenState = screenState,
            showAdditionalAttributes = showAdditionalAttrs,
            hintRes = R.string.clip_hint_title,
            onShowAttrs = this::onShowHideAdditionalAttributes
        )

    private fun createDescriptionBlock(screenState: ClipScreenState) =
        DescriptionBlock(
            dialogState = dialogState,
            mainState = mainState,
            screenState = screenState
        )

    private fun createTextBlock(screenState: ClipScreenState, showPreview: Boolean): BlockItem<Fragment> =
        TextBlock<SnippetKitFragment>(
            appConfig = appConfig,
            screenState = screenState,
            showHidePreview = showPreview,
            textHelper = dynamicTextHelper,
            getMinHeight = { it.getMinHeight() },
            onDynamicFieldClicked = this::onDynamicFieldClicked,
            onListConfigChanged = this::onListConfigChanged,
            getState = this::getScreenState
        ) as BlockItem<Fragment>

    private fun createAttachmentsBlock(screenState: ClipScreenState, files: List<FileRef>) =
        AttachmentsBlock(
            screenState = screenState,
            onShowAttachment = { onShowAttachment(screenState.value, it, files) },
            onGetFiles = { callback -> callback.invoke(files) }
        )

    private fun onShowAttachment(clip: Clip, fileRef: FileRef, files: List<FileRef>) {
        fileUseCases.onView(fileRef, files, clip.title)
    }

    private fun onDynamicFieldClicked(formField: FormField, editable: Editable?) {
        dynamicFieldState.requestViewField(formField.field)
            .subscribeBy("onDynamicFieldClicked")
    }

    companion object {
        private const val ATTR_SNIPPET_KIT = "ATTR_SNIPPET_KIT"
        private const val ATTR_SNIPPET_INDEX = "ATTR_SNIPPET_INDEX"

        fun buildArgs(kit: SnippetKit, snippet: Snippet): Bundle? {
            val snippedIndex = kit.getSortedSnippets().indexOf(snippet)
            if (snippedIndex == -1) {
                return null
            }
            val bundle = Bundle()
            bundle.putSerializable(ATTR_SNIPPET_KIT, kit)
            bundle.putInt(ATTR_SNIPPET_INDEX, snippedIndex)
            return bundle
        }
    }

}