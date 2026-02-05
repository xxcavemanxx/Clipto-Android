package clipto.presentation.clip.view

import android.app.Application
import android.text.Editable
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.LeadingMarginSpan
import android.text.style.MetricAffectingSpan
import android.text.style.StrikethroughSpan
import androidx.core.text.getSpans
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import clipto.AppContext
import clipto.action.CopyClipsAction
import clipto.action.SaveClipAction
import clipto.common.extensions.toNullIfEmpty
import clipto.common.presentation.mvvm.lifecycle.UniqueLiveData
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.dao.objectbox.model.ClipBox
import clipto.domain.*
import clipto.dynamic.DynamicTextHelper
import clipto.dynamic.DynamicValuesFactory
import clipto.dynamic.FormField
import clipto.dynamic.presentation.field.DynamicFieldState
import clipto.dynamic.presentation.field.model.ResultCode
import clipto.extensions.createTag
import clipto.extensions.from
import clipto.extensions.getId
import clipto.extensions.isNew
import clipto.presentation.blocks.domain.SeparatorsBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.clip.ClipScreenHelper
import clipto.presentation.clip.details.ClipDetails
import clipto.presentation.clip.view.blocks.TextBlock
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.presentation.common.dialog.hint.HintDialogData
import clipto.presentation.common.fragment.attributed.AttributedObjectViewModel
import clipto.presentation.common.fragment.attributed.blocks.*
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.usecases.FileUseCases
import clipto.presentation.usecases.NoteUseCases
import clipto.repository.IClipRepository
import clipto.repository.IFileRepository
import clipto.repository.IFilterRepository
import clipto.store.clip.ClipScreenState
import clipto.store.clip.ClipState
import clipto.store.user.UserState
import clipto.utils.DomainUtils
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Single
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ClipViewModel @Inject constructor(
    app: Application,
    val clipState: ClipState,
    val fileUseCases: FileUseCases,
    val noteUseCases: NoteUseCases,
    val dynamicTextHelper: DynamicTextHelper,
    private val userState: UserState,
    private val saveClipAction: SaveClipAction,
    private val copyClipsAction: CopyClipsAction,
    private val clipRepository: IClipRepository,
    private val savedStateHandle: SavedStateHandle,
    private val filterRepository: IFilterRepository,
    private val dynamicFieldState: DynamicFieldState,
    private val dynamicValuesFactory: DynamicValuesFactory,
    private val fileRepository: IFileRepository,
    private val clipScreenHelper: ClipScreenHelper
) : AttributedObjectViewModel<Clip, ClipScreenState>(app) {

    companion object {
        private const val CLIP_ID = "last_clip_id"
    }

    private var currentState: ClipScreenState? = null
    private var settingsChanged = false

    val maxNotesCountLive = UniqueLiveData<Int>()

    fun isMergeMode(): Boolean = isEditMode() && !getScreenState()?.value?.sourceClips.isNullOrEmpty()
    override fun isSettingsChanged(): Boolean = super.isSettingsChanged() || settingsChanged

    override fun doCreate() {
        super.doCreate()
        savedStateHandle.get<Long>(CLIP_ID)
            ?.takeIf { it != 0L }
            ?.takeIf { clipState.screenState.isNull() }
            ?.let { id ->
                clipRepository.getById(id).subscribeBy(
                    "getById",
                    { clip ->
                        clipState.setViewState(clip)
                    },
                    {
                        savedStateHandle.remove<Long>(CLIP_ID)
                        clipState.setNewState()
                    })
            }
        var prevCount = getNavigatorMaxValue()
        appState.filters.getLiveChanges()
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .map { it.last.notesCount.toInt() }
            .filter { it != prevCount }
            .subscribeBy("maxNotesCountLive") {
                maxNotesCountLive.postValue(it)
                prevCount = it
            }
    }

    override fun doClear() {
        super.doClear()
        clipScreenHelper.unbind()
        if (isEditMode()) onAutoSave(true)
    }

    fun isNotSynced(): Boolean {
        return userState.isNotSynced(getScreenState()?.value)
    }

    override fun onUpdateState() {
        getClip()?.let { clipRef ->
            screenStateLive.postValue(screenStateLive.value?.copy(value = clipRef, focusMode = FocusMode.NONE))
        }
    }

    override fun createScreenStateLive(): MutableLiveData<ClipScreenState> {
        return clipState.screenState.getMutableLiveData()
    }

    override fun onCreateBlocks(from: ClipScreenState?, blocksCallback: (blocks: List<BlockItem<Fragment>>) -> Unit) {
        if (from == null) {
            currentState = null
            return blocksCallback.invoke(emptyList())
        }

        val clip = from.value
        val newClip = ClipBox().apply(clip)
        val hasTags = newClip.getTags(noExcluded = true).isNotEmpty()
        val hasFiles = newClip.hasFiles()

        val screenState = from.copy(value = newClip)
        val blocks = mutableListOf<BlockItem<Fragment>>()
        val showAdditionalAttrs = getSettings().noteShowAdditionalAttributes || (!showHideAdditionalAttributesCalled && newClip.isNew() && newClip.isSnippet())

        currentState = screenState

        val hasAttrsBlock = false

        when (screenState.viewMode) {
            ViewMode.VIEW, ViewMode.PREVIEW -> {
                blocks.add(createTitleBlock(screenState, showAdditionalAttrs))
                if (showAdditionalAttrs) {
                    blocks.add(createAbbreviationBlock(screenState))
                    blocks.add(createDescriptionBlock(screenState))
                }
                if (hasAttrsBlock) {
                    blocks.add(createAttrsBlock(screenState))
                }
                if (hasTags) {
                    if (hasAttrsBlock) {
                        blocks.add(SpaceBlock(heightInDp = 12))
                    }
                    blocks.add(createTagsBlock(screenState))
                }
                if (hasFiles) {
                    if (!hasTags && hasAttrsBlock) {
                        blocks.add(SpaceBlock(heightInDp = 12))
                    } else if (hasTags) {
                        blocks.add(SpaceBlock(heightInDp = 6))
                    }
                    blocks.add(createAttachmentsBlock(screenState))
                }
                val showPreview = clip.textType == TextType.LINK && linkPreviewState.canShowPreview.requireValue()
                blocks.add(createTextBlock(screenState, showPreview))

                if (screenState.isViewMode()) {
                    savedStateHandle[CLIP_ID] = clip.getId()
                }
            }
            ViewMode.EDIT -> {
                blocks.add(createTitleBlock(screenState, showAdditionalAttrs))
                if (showAdditionalAttrs) {
                    blocks.add(createAbbreviationBlock(screenState))
                    blocks.add(createDescriptionBlock(screenState))
                }
                if (hasAttrsBlock) {
                    blocks.add(createAttrsBlock(screenState))
                }
                if (hasTags) {
                    if (hasAttrsBlock) {
                        blocks.add(SpaceBlock(heightInDp = 12))
                    }
                    blocks.add(createTagsBlock(screenState))
                }
                blocks.add(createAddTagBlock(screenState))
                if (hasFiles) {
                    if (!hasTags && hasAttrsBlock) {
                        blocks.add(SpaceBlock(heightInDp = 12))
                    }
                    blocks.add(createAttachmentsBlock(screenState))
                }
                if (isMergeMode()) {
                    blocks.add(SpaceBlock(heightInDp = 8))
                    blocks.add(createSeparatorsBlock(screenState))
                    blocks.add(SpaceBlock(heightInDp = 8))
                }
                blocks.add(createTextBlock(screenState, false))
            }
            else -> Unit
        }

        blocksCallback.invoke(blocks)
    }

    override fun onNavigate(index: Int) {
        mainState.clipsQuery.getValue()?.takeIf { index >= 0 && index <= getNavigatorMaxValue() }?.let { query ->
            Single
                .fromCallable { query.find((index.toLong()), 1).firstOrNull()?.let { clip -> clipState.setViewState(clip) } }
                .subscribeBy("onNavigate")
        }
    }

    override fun hasNavigator(): Boolean = appConfig.noteSupportFastPager()
            && isViewMode()
            && mainState.clipsQuery.isNotNull()
            && getNavigatorMaxValue() > 1

    override fun getNavigatorMaxValue(): Int = appState.getFilterByLast().notesCount.toInt()

    override fun onInitNavigator(callback: (index: Int) -> Unit) {
        Single
            .fromCallable {
                getScreenState()
                    ?.value
                    ?.let { clip ->
                        mainState.clipsQuery
                            .getValue()
                            ?.findIds()
                            ?.indexOf(clip.getId())
                    }
                    ?.takeIf { it != -1 }
                    ?: 0
            }
            .subscribeBy("onInitNavigator") { callback(it) }
    }

    fun getClip(): Clip? = currentState?.value?.let { clip ->
        ClipBox().apply(clip).apply {
            val clipSnippetKits = clipState.changedSnippetKitIds.getValue()
            val clipAbbreviation = clipState.changedAbbreviation.getValue()
            val clipDescription = clipState.changedDescription.getValue()
            val clipFileIds = clipState.changedFileIds.getValue()
            val clipFolder = clipState.changedFolderId.getValue()
            val clipTitle = clipState.changedTitle.getValue()
            val clipText = clipState.changedText.getValue()
            val clipTags = clipState.changedTags.getValue()

            snippetSetsIds = clipSnippetKits ?: emptyList()
            abbreviation = clipAbbreviation.toNullIfEmpty(trim = false)
            description = clipDescription.toNullIfEmpty(trim = false)
            title = clipTitle.toNullIfEmpty(trim = false)
            text = clipText.toNullIfEmpty(trim = false)
            fileIds = clipFileIds ?: emptyList()
            tagIds = clipTags ?: emptyList()
            folderId = clipFolder

            if (isNew() && textType == TextType.TEXT_PLAIN) {
                if (clipText is Spanned) {
                    clipText
                        .getSpans<CharacterStyle>(0)
                        .takeIf {
                            it.isNotEmpty() &&
                                    it.find { span ->
                                        span is LeadingMarginSpan
                                                || span is MetricAffectingSpan
                                                || span is StrikethroughSpan
                                    } != null
                        }
                        ?.let {
                            textType = TextType.MARKDOWN
                        }
                }
            }
        }
    }

    private fun onGetFiles(clip: Clip, changesCallback: (files: List<FileRef>) -> Unit) {
        fileRepository.getFiles(clip.fileIds)
            .observeOn(getViewScheduler())
            .subscribeBy("onGetFiles") { changesCallback.invoke(it) }
    }

    fun onAddAttachment(fileRef: FileRef) {
        currentState?.value?.let {
            val fileId = fileRef.getUid() ?: return@let
            val fileIds = clipState.changedFileIds.getValue() ?: emptyList()
            val newFileIds = fileIds.plus(fileId).distinct()
            onAttachmentsChanged(newFileIds)
        }
    }

    private fun onRemoveAttachment(fileRef: FileRef) {
        currentState?.value?.let {
            val fileId = fileRef.getUid() ?: return@let
            val fileIds = clipState.changedFileIds.getValue() ?: emptyList()
            val newFileIds = fileIds.minus(fileId)
            onAttachmentsChanged(newFileIds)
        }
    }

    private fun onShowAttachment(fileRef: FileRef) {
        currentState?.value?.let { clip ->
            onGetFiles(clip) { files ->
                fileUseCases.onView(fileRef, files, clip.title)
            }
        }
    }

    fun onSave() {
        onCancelAutoSave()
        getClip()?.let { clip ->
            saveClipAction.execute(
                clip,
                callback = { onUpdate(it, ViewMode.VIEW) },
                withLoadingState = false
            )
        }
    }

    private fun createTitleBlock(screenState: ClipScreenState, showAdditionalAttrs: Boolean) =
        TitleBlock(
            screenState = screenState,
            showAdditionalAttributes = showAdditionalAttrs,
            onChanged = this::onTitleChanged,
            hintRes = R.string.clip_hint_title,
            isNew = screenState.value.isNew(),
            onEdit = { onUpdate(viewMode = ViewMode.EDIT, focusMode = FocusMode.TITLE) },
            onShowAttrs = this::onShowHideAdditionalAttributes,
            onNextFocus = this::onNextFocus
        )

    private fun createAbbreviationBlock(screenState: ClipScreenState) =
        AbbreviationBlock(
            dialogState = dialogState,
            screenState = screenState,
            onChanged = this::onAbbreviationChanged,
            onEdit = { onUpdate(viewMode = ViewMode.EDIT, focusMode = FocusMode.ABBREVIATION) },
            onNextFocus = this::onNextFocus
        )

    private fun createDescriptionBlock(screenState: ClipScreenState) =
        DescriptionBlock(
            dialogState = dialogState,
            mainState = mainState,
            screenState = screenState,
            onChanged = this::onDescriptionChanged,
            onEdit = { onUpdate(viewMode = ViewMode.EDIT, focusMode = FocusMode.DESCRIPTION) }
        )

    private fun createAttrsBlock(screenState: ClipScreenState): BlockItem<Fragment> {
        return clipScreenHelper.createAttrsBlock(
            clip = screenState.value,
            clipToChange = { getClip() },
            onChanged = { clip ->
                onUpdate(clip)
                onAutoSave()
            }
        )
    }

    private fun createTagsBlock(screenState: ClipScreenState) =
        TagsBlock(
            screenState = screenState,
            filterDetailsState = filterDetailsState,
            onRemoveTag = this::onRemoveTag,
            onEdit = { onUpdate(viewMode = ViewMode.EDIT, focusMode = FocusMode.TAGS) }
        )

    private fun createAddTagBlock(screenState: ClipScreenState) =
        AddTagBlock(
            viewModel = this,
            screenState = screenState,
            tagsLive = clipState.changedTags,
            onAdded = this::onAddTag
        )

    private fun createAttachmentsBlock(screenState: ClipScreenState) =
        clipScreenHelper.createAttachmentsBlock(
            screenState,
            onEdit = { onUpdate(viewMode = ViewMode.EDIT) },
            onShow = this::onShowAttachment,
            onRemove = this::onRemoveAttachment
        )

    private fun createTextBlock(screenState: ClipScreenState, showPreview: Boolean): BlockItem<Fragment> =
        TextBlock<ClipFragment>(
            appConfig = appConfig,
            getClip = { getClip() },
            screenState = screenState,
            showHidePreview = showPreview,
            textHelper = dynamicTextHelper,
            getMinHeight = { it.getMinHeight() },
            onEdit = { onUpdate(viewMode = ViewMode.EDIT, focusMode = FocusMode.TEXT_AUTO_SCROLL) },
            onDynamicFieldClicked = this::onDynamicFieldClicked,
            onListConfigChanged = this::onListConfigChanged,
            onTextChanged = this::onTextChanged,
            getState = this::getScreenState,
            onUpdate = this::onUpdate
        ) as BlockItem<Fragment>

    private fun createSeparatorsBlock(screenState: ClipScreenState) =
        SeparatorsBlock<Fragment>(getSettings().textSeparator) {
            val clips = screenState.value.sourceClips ?: emptyList()
            val text = DomainUtils.getText(clips, it)
            clipState.changedText.setValue(text)
            contentChangedLive.postValue(true)
            getSettings().textSeparator = it
            settingsChanged = true
            onUpdate()
        }

    private fun onTagsChanged(tagIds: List<String>, notify: Boolean) {
        if (isEditMode()) {
            if (clipState.changedTags.setValue(tagIds)) {
                log("onTagsChanged :: {}", tagIds)
                onAutoSave()
                if (notify) {
                    onUpdate(focusMode = FocusMode.TAGS)
                } else if (tagIds.isEmpty()) {
                    onUpdate()
                }
            }
        }
    }

    private fun onAttachmentsChanged(fileIds: List<String>) {
        if (clipState.changedFileIds.setValue(fileIds)) {
            log("onAttachmentsChanged :: {}", fileIds)
            onAutoSave()
            onUpdate()
        }
    }

    private fun onTitleChanged(title: CharSequence?) {
        if (isEditMode()) {
            if (clipState.changedTitle.setValue(title?.toNullIfEmpty(trim = false))) {
                log("onTitleChanged")
                onAutoSave()
            }
        }
    }

    private fun onDescriptionChanged(description: CharSequence?) {
        if (isEditMode()) {
            if (clipState.changedDescription.setValue(description?.toNullIfEmpty(trim = false))) {
                log("onDescriptionChanged")
                onAutoSave()
            }
        }
    }

    private fun onAbbreviationChanged(abbreviation: CharSequence?) {
        if (isEditMode()) {
            if (clipState.changedAbbreviation.setValue(abbreviation?.toNullIfEmpty(trim = false))) {
                log("onAbbreviationChanged")
                onAutoSave()
            }
        }
    }

    private fun onTextChanged(text: CharSequence?) {
        if (isEditMode()) {
            if (clipState.changedText.setValue(text?.toNullIfEmpty(trim = false))) {
                log("onTextChanged")
                onAutoSave()
            }
        }
    }

    private fun onAddTag(tagName: CharSequence?) {
        tagName.toNullIfEmpty()?.let {
            onCreate(it) { filter ->
                val uid = filter.uid!!
                val tags = clipState.changedTags.getValue() ?: emptyList()
                val newTags = tags.plus(uid).distinct()
                currentState?.value?.let { clip -> clip.excludedTagIds = clip.excludedTagIds.minus(uid) }
                onTagsChanged(newTags, true)
            }
        }
    }

    private fun onCreate(tagName: String, callback: (filter: Filter) -> Unit) {
        val filter = appState.getFilters().findFilterByTagName(tagName)
        if (filter != null) {
            callback.invoke(filter)
        } else {
            val tagFilter = Filter.createTag(tagName)
            filterRepository.save(tagFilter)
                .observeOn(getViewScheduler())
                .subscribeBy("onCreateTag") { callback.invoke(it) }
        }
    }

    private fun onRemoveTag(tag: Filter) {
        currentState?.value?.let { clip ->
            val tagId = tag.uid ?: return@let
            val tagIds = clipState.changedTags.getValue() ?: emptyList()
            val newTagIds = tagIds.minus(tagId)
            clip.excludedTagIds = clip.excludedTagIds.plus(tagId)
            onTagsChanged(newTagIds, false)
        }
    }

    private fun onAutoSave(force: Boolean = isViewMode()) {
        val interval =
            when {
                force -> 0L
                getSettings().autoSave -> appConfig.autoSaveInterval()
                else -> -1L
            }
        if (interval != -1L && !isMergeMode()) {
            getClip()?.let { clip ->
                onAutoSaveStateChanged(true)
                Single
                    .fromCallable {
                        val isAutoSaveInProgress = autoSaveState.getValue()?.second == true
                        val isClipTheSame = currentState?.value == clip
                        log("auto save :: isAutoSaveInProgress={}, isClipTheSame={}", isAutoSaveInProgress, isClipTheSame)
                        if (isAutoSaveInProgress && isClipTheSame) {
                            saveClipAction.execute(
                                clip,
                                withLoadingState = false,
                                withSilentValidation = true,
                                callback = { autoSaved ->
                                    val currentClip = currentState?.value
                                    val isDeleted = currentClip?.isDeleted() == true || autoSaved.isDeleted()
                                    val isUpdated = currentClip.isNew() || currentClip.getId() == autoSaved.getId()
                                    if (isUpdated || isDeleted) {
                                        currentState = currentState?.copy(value = autoSaved)
                                        currentState?.takeIf { it.isPreviewMode() }?.let { onUpdate(it) }
                                        clipState.screenState.setValue(currentState?.copy(), notifyChanged = false)
                                        onAutoSaveStateChanged(false)
                                    }
                                }
                            )
                        }
                    }
                    .delaySubscription(interval, TimeUnit.MILLISECONDS)
                    .subscribeOn(getBackgroundScheduler())
                    .subscribeBy("onAutoSave")
            }
        } else {
            contentChangedLive.postValue(true)
        }
    }

    private fun doOnCancel() {
        onCancelAutoSave()
        clipState.screenState.getValue()?.value?.let { clip ->
            clip.clearTempState()
            onUpdate(clip = clip, viewMode = ViewMode.VIEW, focusMode = FocusMode.NONE)
        }
    }

    fun onCancel() {
        if (!getSettings().autoSave && contentChangedLive.value == true) {
            val confirmData = ConfirmDialogData(
                iconRes = R.drawable.ic_attention,
                title = string(R.string.confirm_exit_edit_mode_title),
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

    fun onCancelMerge() {
        dialogState.showConfirm(
            ConfirmDialogData(
                iconRes = R.drawable.ic_attention,
                title = string(R.string.clip_multiple_exit_without_save_title),
                description = string(R.string.clip_multiple_exit_without_save_description),
                confirmActionTextRes = R.string.button_yes,
                onConfirmed = { dismiss() },
                cancelActionTextRes = R.string.button_no
            )
        )
    }

    fun onMerge() {
        getClip()?.let { clip ->
            dialogState.showConfirm(ConfirmDialogData(
                iconRes = R.drawable.ic_merge,
                title = string(R.string.main_delete_merged_title),
                description = string(R.string.main_delete_merged_description),
                confirmActionTextRes = R.string.button_yes,
                onConfirmed = {
                    saveClipAction.execute(
                        clip,
                        callback = {
                            mainState.clearSelection()
                            onUpdate(it, viewMode = ViewMode.VIEW, title = null)
                        },
                        withLoadingState = true
                    )
                },
                cancelActionTextRes = R.string.button_no,
                onCanceled = {
                    clip.sourceClips = null
                    saveClipAction.execute(
                        clip,
                        callback = {
                            mainState.clearSelection()
                            onUpdate(it, viewMode = ViewMode.VIEW, title = null)
                        },
                        withLoadingState = true
                    )
                }
            ))
        }
    }

    fun onEdit() {
        currentState?.let {
            onUpdate(it.copy(viewMode = ViewMode.EDIT, focusMode = FocusMode.NONE))
        }
    }

    fun onSync() {
        getClip()?.let { clip ->
            val title = string(R.string.sync_off_title)
            val description = SimpleSpanBuilder()
                .append(string(R.string.sync_off_caption))
                .append("\n\n")
                .append(string(R.string.account_sync_plan_warning_limit_reached_title))
                .append("\n\n")
                .append(string(R.string.sync_off_reason_universal_clipboard))
                .append("\n\n")
                .append(string(R.string.sync_off_reason_manual_off))
                .build()
                .toString()
            val data = ConfirmDialogData(
                iconRes = R.drawable.ic_clip_not_synced_action,
                title = title,
                description = description,
                confirmActionTextRes = R.string.button_sync,
                onConfirmed = {
                    clipRepository
                        .syncAll(listOf(clip)) {
                            it.firstOrNull()?.let { synced -> onUpdate(synced) }
                        }
                })
            dialogState.showConfirm(data)
        }
    }

    fun onCopy() {
        getClip()?.let {
            AppContext.get().onCopy(it, saveCopied = !it.isDeleted())
        }
    }

    fun onCopyPlaceholder(text: String) {
        val clip = Clip.from(text).apply {
            objectType = ObjectType.INTERNAL_GENERATED
        }
        copyClipsAction.execute(listOf(clip), withToast = false) {
            dialogState.showHint(
                HintDialogData(
                    title = string(R.string.dynamic_value_placeholder_copied_title),
                    description = string(R.string.dynamic_value_placeholder_copied_description, text),
                    descriptionIsMarkdown = true
                )
            )
        }
    }

    fun onToggleFav() {
        getClip()?.let { clip ->
            val fav = !clip.fav
            clipRepository.favAll(listOf(clip), fav)
                .subscribeBy("onToggleFav") {
                    it.firstOrNull()?.let { starred -> clipState.setViewState(starred) }
                }
        }
    }

    fun onDelete() {
        getClip()?.let { clip ->
            dialogState.showConfirm(
                ConfirmDialogData(
                    iconRes = R.drawable.ic_attention,
                    title = string(R.string.main_delete_selected_title),
                    description = string(R.string.main_delete_selected_description),
                    confirmActionTextRes = R.string.menu_delete,
                    onConfirmed = {
                        clipRepository.deleteAll(listOf(clip))
                            .subscribeBy("onDelete") {
                                it.firstOrNull()?.let { deleted -> clipState.setViewState(deleted) }
                            }
                    },
                    cancelActionTextRes = R.string.menu_cancel
                )
            )
        }
    }


    fun onUndoDelete() {
        getClip()?.let { clip ->
            mainState.undoDeleteClips.clearValue()
            clipRepository.undoDeleteAll(listOf(clip))
                .doOnSuccess { appState.refreshFilters() }
                .subscribeBy("onUndoDelete") { it.firstOrNull()?.let { restored -> onUpdate(restored) } }
        }
    }

    fun onUpdate(screenState: ClipScreenState?) {
        val clip = screenState?.value
        clipState.changedSnippetKitIds.setValue(clip?.snippetSetsIds)
        clipState.changedAbbreviation.setValue(clip?.abbreviation)
        clipState.changedDescription.setValue(clip?.description)
        clipState.changedFileIds.setValue(clip?.fileIds)
        clipState.changedTitle.setValue(clip?.title)
        clipState.changedTags.setValue(clip?.tagIds)
        clipState.changedText.setValue(clip?.text)
        currentState = screenState
        screenStateLive.value = screenState
    }

    fun onUpdate(
        clip: Clip? = getClip(),
        viewMode: ViewMode = getViewMode(),
        focusMode: FocusMode = getFocusMode(),
        title: CharSequence? = getTitle()
    ) {
        log("onUpdate :: clip={}, viewMode: {} -> {}", clip, getViewMode(), viewMode)
        when (viewMode) {
            ViewMode.EDIT -> {
                clip?.let { newClip ->
                    val currentViewMode = getViewMode()
                    val newState = ClipScreenState(value = newClip, viewMode = viewMode, focusMode = focusMode)
                    onUpdate(newState)
                    contentChangedLive.postValue(currentViewMode == viewMode)
                }
            }
            else -> {
                getScreenState()?.value?.clearTempState()
                contentChangedLive.postValue(false)
                currentState =
                    if (clip == null) {
                        null
                    } else {
                        clipState.setState(clip, viewMode, focusMode, title)
                    }
            }
        }
    }

    fun onUpdate(newDetails: ClipDetails) {
        getClip()?.let { clip ->
            clip.excludedTagIds = newDetails.clip.excludedTagIds
            clip.snippetSetsIds = newDetails.snippetKitIds
            clip.folderId = newDetails.clip.folderId
            clip.publicLink = newDetails.publicLink
            clip.fileIds = newDetails.fileIds
            clip.textType = newDetails.type
            clip.tagIds = newDetails.tagIds
            clip.fav = newDetails.fav
            newDetails.clip.apply(clip)
            onUpdate(clip)
            onAutoSave()
        }
        if (newDetails.type != getSettings().textType) {
            getSettings().textType = newDetails.type
            settingsChanged = true
        }
    }

    fun onOneMore() {
        val clip = getClip()
        if (clip == null) {
            dialogState.showSnackbar(string(R.string.clip_snackbar_text_required))
            return
        }
        onCancelAutoSave()
        saveClipAction.execute(
            clip,
            withSilentValidation = true,
            withLoadingState = false
        )
        val oneMoreClip = ClipBox()
            .apply {
                snippetSetsIds = clip.snippetSetsIds
                textType = getSettings().textType
                folderId = clip.folderId
                tagIds = clip.tagIds
                fav = clip.fav
                createDate = Date()
            }
        val focusMode = if (getSettings().focusOnTitle) FocusMode.TITLE else FocusMode.TEXT
        clipState.setEditState(oneMoreClip, focusMode)
    }

    private fun onDynamicFieldClicked(formField: FormField, editable: Editable?) {
        if (isEditMode()) {
            val field = formField.field
            val fieldProvider = dynamicValuesFactory.getFieldProvider(field)
            val placeholderBefore = fieldProvider.createPlaceholder(field)
            dynamicFieldState.requestEditField(formField.field)
                .observeOn(getViewScheduler())
                .subscribeBy("onDynamicFieldClicked") {
                    when (it.resultCode) {
                        ResultCode.DELETE -> {
                            runCatching { editable?.replace(formField.startIndex, formField.endIndex, "") }
                        }
                        ResultCode.UPDATE -> {
                            val placeholderAfter = fieldProvider.createPlaceholder(field)
                            if (placeholderAfter != placeholderBefore) {
                                runCatching {
                                    runCatching { editable?.replace(formField.startIndex, formField.endIndex, placeholderAfter) }
                                }
                            }
                        }
                        else -> Unit
                    }
                }
        } else {
            dynamicFieldState.requestViewField(formField.field)
                .subscribeBy("onDynamicFieldClicked")
        }
    }

}