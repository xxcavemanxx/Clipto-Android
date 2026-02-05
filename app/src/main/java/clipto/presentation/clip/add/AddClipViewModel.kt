package clipto.presentation.clip.add

import android.app.Application
import android.content.res.Resources
import android.view.Gravity
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagedList
import clipto.action.SaveClipAction
import clipto.action.intent.IntentActionFactory
import clipto.action.intent.provider.AppPreviewNoteProvider
import clipto.analytics.Analytics
import clipto.common.extensions.notNull
import clipto.common.misc.Units
import clipto.dao.objectbox.ClipBoxDao
import clipto.dao.sharedprefs.SharedPrefsDao
import clipto.dao.sharedprefs.data.AddClipScreenData
import clipto.domain.*
import clipto.dynamic.DynamicTextHelper
import clipto.extensions.*
import clipto.presentation.blocks.*
import clipto.presentation.blocks.domain.MainActionBlock
import clipto.presentation.blocks.domain.SeparatorsBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.clip.ClipScreenHelper
import clipto.presentation.clip.add.data.AddClipRequest
import clipto.presentation.clip.add.data.AddClipType
import clipto.presentation.clip.view.blocks.TextBlock
import clipto.presentation.common.StyleHelper
import clipto.presentation.common.dialog.blocks.BlocksDialogViewModel
import clipto.presentation.common.fragment.attributed.blocks.AbbreviationBlock
import clipto.presentation.common.fragment.attributed.blocks.DescriptionBlock
import clipto.presentation.common.fragment.attributed.blocks.TagsBlock
import clipto.presentation.common.fragment.attributed.blocks.TitleBlock
import clipto.presentation.common.fragment.blocks.BlocksViewModel
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.preview.link.LinkPreviewState
import clipto.repository.IClipRepository
import clipto.repository.ISettingsRepository
import clipto.store.clip.ClipScreenState
import clipto.store.clipboard.ClipboardState
import clipto.store.filter.FilterDetailsState
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Single
import javax.inject.Inject

@HiltViewModel
class AddClipViewModel @Inject constructor(
    app: Application,
    private val clipBoxDao: ClipBoxDao,
    private val clipboardState: ClipboardState,
    private val filterDetailsState: FilterDetailsState,
    private val clipRepository: IClipRepository,
    private val linkPreviewState: LinkPreviewState,
    private val dynamicTextHelper: DynamicTextHelper,
    private val clipScreenHelper: ClipScreenHelper,
    private val savedStateHandle: SavedStateHandle,
    private val saveClipAction: SaveClipAction,
    private val sharedPrefsDao: SharedPrefsDao,
    private val settingsRepository: ISettingsRepository,
    private val intentActionFactory: IntentActionFactory
) : BlocksViewModel(app) {

    companion object {
        const val PEEK_HEIGHT = 0.75f
    }

    private val request: AddClipRequest? by lazy { savedStateHandle.get(AddClipFragment.ATTR_REQUEST) }

    private var screenData: AddClipScreenData = AddClipScreenData()
    private var screenState: ClipScreenState? = null
    private var clipChanged: Boolean = false
    private var clip: Clip = Clip.NULL

    fun isClipChanged(): Boolean = clipChanged
    fun getScreenState(): ClipScreenState? = screenState
    private fun getContentMinHeight(): Int = (Resources.getSystem().displayMetrics.heightPixels * PEEK_HEIGHT - Units.DP.toPx(12f)).toInt()

    fun getClipsSearchByText() = clipScreenHelper.getSearchByText()
    fun getClipsLive(): LiveData<PagedList<Clip>> = clipScreenHelper.getClipsLive()
    fun getClipsListConfig() = mainState.getListConfig().copy(appState.getFilterByAll()).copy(listStyle = ListStyle.PREVIEW)
    private fun getClipsFilter() = Filter.Snapshot().copy(appState.getFilterByAll())

    private fun saveData() {
        sharedPrefsDao.saveAddClipData(screenData).subscribeBy("saveScreenData")
    }

    override fun doClear() {
        clipScreenHelper.unbind()
        super.doClear()
    }

    override fun doCreate() {
        val req = request
        if (req == null) {
            dismiss()
        } else {
            val flow = sharedPrefsDao
                .getAddClipData()
                .map { data ->
                    screenData = data.copy(screenType = request?.screenType ?: data.screenType)
                }
            log("doCreate :: {}", req)
            when {
                req.id != null -> {
                    flow.flatMap { clipRepository.getById(req.id) }
                        .doOnError { dialogState.showError(it) }
                        .subscribeBy("doCreate") {
                            onUpdateBlocks(ClipScreenState(it, viewMode = ViewMode.VIEW, focusMode = FocusMode.NONE))
                        }
                }
                req.text != null -> {
                    flow.flatMap { clipRepository.getByText(req.text) }
                        .flatMap { if (req.tracked) clipRepository.save(it, true) else Single.just(it) }
                        .onErrorReturn {
                            val clip = Clip.from(req.text, req.tracked)
                            clip.textType = clipBoxDao.defineClipType(req.text)
                            clip.folderId = req.folderId ?: screenData.folderId
                            clipBoxDao.applyAutoTags(clip)
                            clip.title = req.title
                            clip
                        }
                        .subscribeBy("doCreate") { clip ->
                            onUpdateBlocks(ClipScreenState(clip, viewMode = ViewMode.VIEW, focusMode = FocusMode.NONE))
                        }
                }
                else -> {
                    dismiss()
                }
            }

            linkPreviewState.canShowPreview.getLiveChanges()
                .filter { it.isNotNull() }
                .map { it.requireValue() }
                .subscribeBy("canShowPreview") {
                    onRefreshState()
                }

            dialogState.fastActionRequest.getLiveChanges()
                .filter { it.isNull() }
                .subscribeBy("fastActionRequest") {
                    onRefreshState()
                }
        }
    }

    override fun onShowHideKeyboard(visible: Boolean) {
        super.onShowHideKeyboard(visible)
        if (!visible) {
            log("update view state because of the keyboard hidden")
            screenState?.takeIf { it.isEditMode() }?.let {
                onUpdateBlocks(it.copy(viewMode = ViewMode.VIEW))
            }
        }
    }

    fun onInsertInto(clip: Clip, requestSettings: Boolean) {
        getClip()?.let { curr ->
            val complete: () -> Unit = {
                clip.tagIds = clip.tagIds.plus(curr.tagIds).distinct()
                clip.text = StyleHelper.getNewText(clip, curr.text.notNull())
                saveClipAction.execute(clip) {
                    Analytics.onNoteInserted()
                    dismiss()
                }
            }
            if (getSettings().textInsertRemember && !requestSettings) {
                complete()
            } else {
                dialogState.requestBlocksDialog { onInsertInto(clip, curr, it, complete) }
            }
        }
    }

    private fun onInsertInto(clip: Clip, curr: Clip, viewModel: BlocksDialogViewModel, complete: () -> Unit) {
        val blocks = mutableListOf<BlockItem<Fragment>>()

        // POSITION
        blocks.add(SpaceBlock(12))
        blocks.add(
            TitleBlock(
                R.string.clip_info_label_position,
                gravity = Gravity.CENTER_HORIZONTAL,
                width = ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        blocks.add(
            TwoButtonsToggleBlock(
                firstButtonTextRes = R.string.clip_info_label_beginning,
                onFirstButtonClick = {
                    getSettings().textPositionBeginning = true
                },
                secondButtonTextRes = R.string.clip_info_label_end,
                onSecondButtonClick = {
                    getSettings().textPositionBeginning = false
                },
                selectedButtonIndex = if (getSettings().textPositionBeginning) 0 else 1
            )
        )

        // SEPARATOR
        blocks.add(SpaceBlock(20))
        blocks.add(
            TitleBlock(
                R.string.clip_multiple_merge_separator,
                gravity = Gravity.CENTER_HORIZONTAL,
                width = ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        blocks.add(SeparatorsBlock(getSettings().textSeparator) {
            getSettings().textSeparator = it
        })

        // REMEMBER
        blocks.add(SpaceBlock(20))
        blocks.add(SwitchBlock(
            titleRes = R.string.clip_info_label_note_insert_remember,
            checked = getSettings().textInsertRemember,
            textSize = 12,
            clickListener = { _, checked ->
                getSettings().textInsertRemember = checked
            }
        ))

        // COMPLETE
        blocks.add(SpaceBlock(16))
        blocks.add(TwoButtonsBlock(
            primaryTitleRes = R.string.button_insert,
            primaryClickListener = {
                settingsRepository.update(getSettings())
                    .observeOn(getViewScheduler())
                    .subscribeBy("saveSettings", appState) {
                        viewModel.dismiss()
                        complete()
                        Analytics.onNoteInserted()
                    }
            },
            secondaryTitleRes = R.string.button_preview,
            secondaryClickListener = {
                settingsRepository.update(getSettings())
                    .observeOn(getViewScheduler())
                    .subscribeBy("saveSettings", appState) {
                        viewModel.dismiss()
                        dismiss()
                        Analytics.onNoteInsertedWithPreview()
                        clip.tagIds = clip.tagIds.plus(curr.tagIds).distinct()
                        clip.newText = StyleHelper.getNewText(clip, curr.text.notNull())
                        val intent = intentActionFactory.getIntent(AppPreviewNoteProvider.Action(clip))
                        app.startActivity(intent)
                    }
            }
        ))
        blocks.add(SpaceBlock(12))

        viewModel.postBlocks(blocks)
    }

    fun onSave() {
        getClip()?.let { clip ->
            saveClipAction.execute(clip) {
                dismiss()
            }
        }
    }

    private fun getClip(): Clip? = clip.takeIf { it !== Clip.NULL }?.also { it.forceSync = true }

    private fun getClipboardText() = clipboardState.getPrimaryClip().toClip(app)?.text

    private fun onHideHint() {
        screenData = screenData.screenType.hideHint(screenData)
        onRefreshState()
        saveData()
    }

    private fun onEdit(focusMode: FocusMode) {
        screenState?.let { state ->
            clipChanged = true
            onUpdateBlocks(state.copy(viewMode = ViewMode.EDIT, focusMode = focusMode))
        }
    }

    private fun onRefreshState() {
        screenState?.let { onUpdateBlocks(it) }
    }

    private fun onUpdateBlocks(state: ClipScreenState) {
        log("updateBlocks :: type={}, text={}, id={}", state.value.textType, state.value.text, state.value.getId())
        screenState = state
        clip = state.value
        clip.isActive = clip.text == getClipboardText()
        if (clip.isDeleted()) {
            clip.deleteDate = null
            clipChanged = true
        }

        val screenType = screenData.screenType
        val showAdditionalAttrs = getSettings().noteShowAdditionalAttributes
        val blocks = mutableListOf<BlockItem<Fragment>>()

        // SWITCH
        blocks.add(SpaceBlock(heightInDp = 16))
        blocks.add(
            ThreeButtonsToggleBlock(
                firstButtonTextRes = AddClipType.EDIT.titleRes,
                secondButtonTextRes = AddClipType.ACTION.titleRes,
                thirdButtonTextRes = AddClipType.INSERT.titleRes,
                onFirstButtonClick = { onChangeTab(AddClipType.EDIT) },
                onSecondButtonClick = { onChangeTab(AddClipType.ACTION) },
                onThirdButtonClick = { onChangeTab(AddClipType.INSERT) },
                selectedButtonIndex = screenType.index
            )
        )

        if (screenType.canShowHint(screenData)) {
            blocks.add(SpaceBlock(heightInDp = 16))
            blocks.add(
                DescriptionSecondaryBlock(
                    description = string(screenType.descriptionRes),
                    onCancel = this::onHideHint
                )
            )
            blocks.add(SpaceBlock(heightInDp = 4))
        }

        when (screenType) {

            AddClipType.EDIT -> {
                clipScreenHelper.onClearSearch(postEmptyState = true)

                blocks.add(SpaceBlock(heightInDp = 12))

                // ACTIONS
                if (clip.isNew() || clipChanged) {
                    blocks.add(PrimaryButtonBlock(
                        titleRes = R.string.button_save,
                        clickListener = { onSave() }
                    ))
                } else {
                    blocks.add(OutlinedButtonBlock(
                        titleRes = R.string.button_save,
                        clickListener = { onSave() }
                    ))
                }

                // TITLE
                blocks.add(createClipTitleBlock(state, showAdditionalAttrs))

                if (showAdditionalAttrs) {
                    // ABBREVIATION
                    blocks.add(createClipAbbreviationBlock(state))
                    // DESCRIPTION
                    blocks.add(createClipDescriptionBlock(state))
                }

                // ATTRS
                blocks.add(createAttrsBlock(state))

                val tags = clip.getTags(noExcluded = true)
                val hasTags = tags.isNotEmpty()
                if (hasTags) {
                    blocks.add(SpaceBlock(heightInDp = 12))
                    blocks.add(createTagsBlock(state))
                }

                if (clip.hasFiles()) {
                    if (!hasTags) {
                        blocks.add(SpaceBlock(heightInDp = 12))
                    } else {
                        blocks.add(SpaceBlock(heightInDp = 8))
                    }
                    blocks.add(createAttachmentsBlock(state))
                }

                // TEXT
                blocks.add(createClipTextBlock(state))
            }

            AddClipType.ACTION -> {
                clipScreenHelper.onClearSearch(postEmptyState = true)

                // TEXT
                clip.text?.takeIf { !screenType.canShowHint(screenData) }?.let { text ->
                    blocks.add(SpaceBlock(heightInDp = 16))
                    blocks.add(
                        DescriptionSecondaryBlock(
                            description = text,
                            plainText = true,
                            maxLines = 3
                        )
                    )
                    blocks.add(SpaceBlock(heightInDp = 4))
                }

                blocks.add(SpaceBlock(heightInDp = 12))

                // ACTIONS
                FastAction.getAllClipActions(app, clip)
                    .forEach { ca ->
                        val title = string(ca.action.titleRes)
                        val description = ca.label.takeIf { it != title }
                        val iconColor = ca.getIconColor(app)
                        blocks.add(
                            MainActionBlock(
                                onClick = { dialogState.requestFastAction(ca.action, clip) },
                                iconRes = ca.action.getIconRes(),
                                description = description,
                                iconColor = iconColor,
                                title = title
                            )
                        )
                    }
            }

            AddClipType.INSERT -> {
                clipScreenHelper.onSearch(getClipsFilter())
                blocks.add(SpaceBlock(heightInDp = 12))
                blocks.add(createClipSearchBlock())
                blocks.add(SpaceBlock(heightInDp = 8))
            }
        }

        postBlocks(blocks)

        if (state.viewMode != ViewMode.EDIT) {
            onHideKeyboard()
        }
    }

    private fun onChangeTab(type: AddClipType) {
        screenData = screenData.copy(screenType = type)
        onRefreshState()
        saveData()
    }

    private fun createAttrsBlock(state: ClipScreenState): BlockItem<Fragment> {
        return clipScreenHelper.createAttrsBlock(
            withAttachments = true,
            clip = state.value
        ) {
            clipChanged = true
            onRefreshState()
            if (it.folderId != screenData.folderId) {
                screenData = screenData.copy(folderId = it.folderId)
                saveData()
            }
        }
    }

    private fun createClipTitleBlock(state: ClipScreenState, showAdditionalAttrs: Boolean) =
        TitleBlock(
            screenState = state,
            showAdditionalAttributes = showAdditionalAttrs,
            hintRes = R.string.clip_hint_title,
            onChanged = { clip.title = it?.toString() },
            onShowAttrs = {
                getSettings().noteShowAdditionalAttributes = it
                onRefreshState()
            },
            onEdit = { onEdit(FocusMode.TITLE) }
        )

    private fun createClipAbbreviationBlock(state: ClipScreenState) =
        AbbreviationBlock(
            dialogState = dialogState,
            screenState = state,
            onChanged = { clip.abbreviation = it?.toString() },
            onEdit = { onEdit(FocusMode.ABBREVIATION) }
        )

    private fun createClipDescriptionBlock(state: ClipScreenState) =
        DescriptionBlock(
            mainState = mainState,
            dialogState = dialogState,
            screenState = state,
            onChanged = { clip.description = it?.toString() },
            onEdit = { onEdit(FocusMode.DESCRIPTION) }
        )

    private fun createClipSearchBlock() =
        TextInputLayoutBlock<Fragment>(
            text = getClipsSearchByText(),
            hint = string(R.string.main_search),
            onTextChanged = { text ->
                clipScreenHelper.onSearch(getClipsFilter(), text?.toString())
                null
            }
        )

    private fun createTagsBlock(state: ClipScreenState) =
        TagsBlock(
            screenState = state,
            filterDetailsState = filterDetailsState,
            onRemoveTag = {
                it.uid?.let { uid ->
                    clip.excludedTagIds = clip.excludedTagIds.plus(uid)
                    clip.tagIds = clip.tagIds.minus(uid)
                    clipChanged = true
                    onRefreshState()
                }
            },
            onEdit = {
                clipScreenHelper.onEditTags(state.value) {
                    clipChanged = true
                    onRefreshState()
                }
            },
            bgColorAttr = R.attr.colorContext
        )

    private fun createClipTextBlock(state: ClipScreenState) =
        TextBlock<Fragment>(
            appConfig = appConfig,
            getClip = { clip },
            screenState = state,
            getState = { screenState },
            showHidePreview = linkPreviewState.canShowPreview.requireValue(),
            textHelper = dynamicTextHelper,
            getMinHeight = { getContentMinHeight() },
            onEdit = { onEdit(FocusMode.TEXT_AUTO_SCROLL) },
            onTextChanged = { clip.text = it?.toString() }
        )

    private fun createAttachmentsBlock(screenState: ClipScreenState) =
        clipScreenHelper.createAttachmentsBlock(
            screenState,
            backgroundColor = app.getColorContext(),
            onRemove = this::onRemoveAttachment
        )

    private fun onRemoveAttachment(fileRef: FileRef) {
        screenState?.value?.let { clip ->
            val fileId = fileRef.getUid() ?: return@let
            val fileIds = clip.fileIds
            clip.fileIds = fileIds.minus(fileId)
            clipChanged = true
            onRefreshState()
        }
    }

}