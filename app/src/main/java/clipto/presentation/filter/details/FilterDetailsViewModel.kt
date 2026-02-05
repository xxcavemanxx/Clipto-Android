package clipto.presentation.filter.details

import android.app.Application
import android.graphics.Color
import android.text.InputFilter
import androidx.fragment.app.Fragment
import clipto.AppContext
import clipto.action.DeleteFilterAction
import clipto.action.KitUninstallAction
import clipto.action.SaveFilterAction
import clipto.analytics.Analytics
import clipto.common.extensions.toNullIfEmpty
import clipto.dao.objectbox.model.FilterBox
import clipto.domain.*
import clipto.extensions.*
import clipto.presentation.blocks.*
import clipto.presentation.blocks.bottomsheet.ObjectNameEditBlock
import clipto.presentation.blocks.bottomsheet.ObjectNameViewBlock
import clipto.presentation.blocks.domain.ColorWheelBlock
import clipto.presentation.blocks.domain.ListStyleWheelBlock
import clipto.presentation.blocks.domain.SortByWheelBlock
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.blocks.ux.WarningBlock
import clipto.presentation.common.StyleHelper
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.presentation.common.fragment.blocks.BlocksWithHintViewModel
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.filter.details.blocks.HeaderBlock
import clipto.presentation.filter.details.blocks.HeaderClipboardBlock
import clipto.presentation.runes.clipboard.ClipboardRuneProvider
import clipto.presentation.snippets.details.SnippetKitDetailsViewModel
import clipto.presentation.usecases.MainActionUseCases
import clipto.repository.IClipRepository
import clipto.repository.IFilterRepository
import clipto.repository.ISnippetRepository
import clipto.store.StoreObject
import clipto.store.filter.FilterDetailsState
import clipto.store.filter.FilterState
import clipto.store.internet.InternetState
import clipto.store.user.UserState
import com.wb.clipboard.R
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Single
import javax.inject.Inject

@HiltViewModel
class FilterDetailsViewModel @Inject constructor(
    app: Application,
    private val userState: UserState,
    private val filterState: FilterState,
    private val internetState: InternetState,
    private val clipRepository: IClipRepository,
    private val saveFilterAction: SaveFilterAction,
    private val filterRepository: IFilterRepository,
    private val filterDetailsState: FilterDetailsState,
    private val deleteFilterAction: DeleteFilterAction,
    private val clipboardRuneProvider: ClipboardRuneProvider,
    private val snippetRepository: Lazy<ISnippetRepository>,
    private val kitUninstallAction: KitUninstallAction,
    private val mainActionUseCases: MainActionUseCases
) : BlocksWithHintViewModel(app) {

    private var isDeleted = false
    private var requestApply = false
    private var mode = ViewMode.VIEW
    private var filterBefore: Filter = FilterBox()
    private val colorsMatrix by lazy { appConfig.getColorsMatrix() }

    private val filterStore: StoreObject<Filter> by lazy {
        val store = StoreObject<Filter>(id = "filter_store")
        val filters = appState.getFilters()
        val request = filterDetailsState.requestOpenFilter.getValue()
        val openedFilter = request?.let { filters.findActive(it.filter) }
        val filter = openedFilter ?: Filter.createTag()
        requestApply = request?.requestApply ?: false
        filterBefore = FilterBox().apply(filter)
        if (filter.isNew()) mode = ViewMode.EDIT
        store.setValue(filter)
        if (filter.isNamedFilter()) {
            val notesCount = filter.notesCount
            filterRepository.updateNotesCount(filter)
                .subscribeBy("updateNotesCount") {
                    if (notesCount != it.notesCount) {
                        store.setValue(filter, force = true)
                    }
                }
        }
        filterState.requestUpdateFilter.getLiveChanges()
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .filter { it == store.getValue() }
            .flatMapSingle { if (it.isNamedFilter() && it.notesCount == 0L) filterRepository.updateNotesCount(it) else Single.just(it) }
            .subscribeBy("requestUpdateFilter") { store.setValue(filter, force = true) }
        store
    }

    override fun doCreate() {
        filterStore.getLiveChanges()
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .subscribeBy("getFilterChanges") {
                onUpdateBlocks(it)
            }
    }

    override fun doClear() {
        super.doClear()
        filterStore.getValue()?.let { save(it) }
    }

    fun isEditMode() = mode == ViewMode.EDIT

    private fun onUpdateBlocks(filter: Filter) {
        log("onUpdateBlocks :: {}", filter)
        val blocks = createBlocks(filter)
        postBlocks(blocks)
        onHideKeyboard()
        setHint(
            HintPresenter(
                id = filter.uid,
                editMode = isEditMode(),
                hideHint = filter.hideHint,
                value = filter.description,
                title = filter.getHint(app),
                onChanged = { filter.description = it }
            )
        )
    }

    private fun onChangeColor(color: String?) {
        filterStore.getValue()?.let { filter ->
            filter.updateColor(color)
            filterState.requestUpdateFilter(filter)
        }
    }

    private fun onApplyListStyle(listStyle: ListStyle) {
        filterStore.getValue()?.let { filter ->
            filter.listStyle = listStyle
            mainState.requestApplyListConfig(filter) { cfg -> cfg.copy(listStyle = listStyle) }
        }
    }

    private fun onApplySortBy(sortBy: SortBy) {
        filterStore.getValue()?.let { filter ->
            filter.sortBy = sortBy
            if (filter.isGroup()) {
                appState.refreshFilters()
            } else {
                mainState.requestApplyListConfig(filter) { cfg -> cfg.copy(sortBy = sortBy) }
            }
        }
    }

    fun onEdit() {
        filterStore.getValue()?.let { filter ->
            mode = ViewMode.EDIT
            filterStore.setValue(filter, force = true)
        }
    }

    private fun onSaveAs(f: Filter, fragment: Fragment) {
        filterStore.getValue()?.let { filter ->
            mode = ViewMode.EDIT
            val newFilter = FilterBox().apply(filter)
            newFilter.type = Filter.Type.NAMED
            newFilter.hideHint = true
            newFilter.uid = null
            requestApply = true
            filterStore.setValue(newFilter, force = true)
        }
    }

    private fun onCancelEdit() {
        filterStore.getValue()?.let { filter ->
            if (filter.isNew()) {
                dismiss()
            } else {
                mode = ViewMode.VIEW
                val newFilter = FilterBox().apply(filterBefore)
                filterStore.setValue(newFilter, force = true)
            }
        }
    }

    private fun ifValid(filter: Filter, name: String? = filter.name): Filter? {
        val newName = name.toNullIfEmpty()
        val errorRes = when {
            filter.isTag() -> R.string.filter_tag_name_required
            filter.isSnippetKit() -> R.string.snippet_kit_error_name_required
            else -> R.string.filter_error_name_required
        }
        if (newName == null) {
            appState.showToast(string(errorRes))
            return null
        }
        val same = when {
            filter.isTag() -> appState.getFilters().findFilterByTagName(newName)
            filter.isSnippetKit() -> appState.getFilters().findFilterBySnippetSetName(newName)
            else -> appState.getFilters().findFilterByName(newName)
        }
        if (same != null && same != filter) {
            appState.showToast(string(errorRes))
        }
        return filter
    }

    fun onRename(newName: String) {
        filterStore.getValue()?.let { ifValid(it, newName) }?.let { filter ->
            val name = newName.trim()
            val description = filter.description
            if (!filter.isNew() && filter.name == name && description == filterBefore.description) {
                mode = ViewMode.VIEW
                filterStore.setValue(filter, force = true)
                return
            }
            filter.name = name
            save(filter)
        }
    }

    fun onDelete() {
        filterStore.getValue()?.let { filter ->
            val snippetKit = filter.snippetKit
            if (snippetKit != null && snippetKit.userId != userState.getUserId()) {
                kitUninstallAction.execute(snippetKit) {
                    isDeleted = true
                    dismiss()
                }
            } else {
                deleteFilterAction.execute(filter) {
                    isDeleted = true
                    dismiss()
                }
            }
        }
    }

    private fun onAddToGroup(f: Filter, fragment: Fragment) {
        fragment.activity?.let { act ->
            filterStore.getValue()?.let { filter ->
                when (filter.type) {
                    Filter.Type.GROUP_NOTES -> mainActionUseCases.onAction(MainAction.NOTE_NEW, act)
                    Filter.Type.GROUP_FILTERS -> mainActionUseCases.onAction(MainAction.FILTER_NEW, act)
                    Filter.Type.GROUP_SNIPPETS -> mainActionUseCases.onAction(MainAction.SNIPPET_KIT_NEW, act)
                    Filter.Type.GROUP_TAGS -> mainActionUseCases.onAction(MainAction.TAG_NEW, act)
                    Filter.Type.GROUP_FILES -> mainActionUseCases.onAction(MainAction.FILE_SELECT, act)
                    Filter.Type.GROUP_FOLDERS -> mainActionUseCases.onAction(MainAction.FOLDER_NEW, act)
                    else -> Unit
                }
            }
        }
    }

    private fun onClearRecycleBin(f: Filter, fragment: Fragment) {
        dialogState.showConfirm(ConfirmDialogData(
            iconRes = R.drawable.ic_attention,
            title = string(R.string.filter_deleted_empty_title),
            description = string(R.string.filter_deleted_empty_description),
            confirmActionTextRes = R.string.menu_empty,
            onConfirmed = {
                clipRepository.clearRecycleBin()
                    .doFinally { Analytics.onClearDeleted() }
                    .subscribeBy("onClearRecycleBin", appState) { dismiss() }
            }
        ))
    }

    fun onClearClipboard() {
        dialogState.showConfirm(ConfirmDialogData(
            iconRes = R.drawable.ic_attention,
            title = string(R.string.filter_clipboard_empty_title),
            description = string(R.string.filter_clipboard_empty_description),
            confirmActionTextRes = R.string.menu_empty,
            onConfirmed = {
                clipRepository.clearClipboard()
                    .doFinally { Analytics.onClearClipboard() }
                    .subscribeBy("onClearClipboard", appState) { dismiss() }
            }
        ))
    }

    override fun onHideHint() {
        filterStore.getValue()?.let { filter ->
            filter.hideHint = true
            filterStore.setValue(filter, force = true)
        }
    }

    fun onShowHint() {
        filterStore.getValue()?.let { filter ->
            filter.hideHint = false
            filterStore.setValue(filter, force = true)
        }
    }

    private fun save(filter: Filter) {
        if (isDeleted) {
            return
        }
        if (filter.isTag()) {
            val tagName = filter.name.toNullIfEmpty() ?: return
            val same = appState.getFilters().findFilterByTagName(tagName)
            if (same != null && same != filter) return
        }
        if (filter.isSnippetKit()) {
            val name = filter.name.toNullIfEmpty() ?: return
            val same = appState.getFilters().findFilterBySnippetSetName(name)
            if (same != null && same != filter) return
        }
        if (filter.isNamedFilter()) {
            val filterName = filter.name.toNullIfEmpty() ?: return
            val same = appState.getFilters().findFilterByName(filterName)
            if (same != null && same != filter) return
        }
        if (filterBefore.isSame(filter)) return
        val reload = filterBefore.pinStarredEnabled != filter.pinStarredEnabled
        val isNew = filter.isNew()
        saveFilterAction.execute(filter, reload = reload) {
            mode = ViewMode.VIEW
            filterBefore = FilterBox().apply(filter)
            filterStore.setValue(filter, force = true)
            if (requestApply) {
                mainState.requestApplyFilter(filter)
            }
            if (isNew) {
                filterDetailsState.requestOpenFilter.getValue()
                    ?.onCreated
                    ?.let { createHandler ->
                        createHandler.invoke(filter)
                        dismiss()
                    }
            }
        }
    }

    private fun createBlocks(filter: Filter): List<BlockItem<Fragment>> {
        val blocks = mutableListOf<BlockItem<Fragment>>()
        when {
            filter.isTag() -> {
                withHeader(filter, mode, blocks)
                withColors(filter, blocks)
                withListStyle(filter, blocks)
                withSortBy(filter, blocks)
                withAutoRule(filter, blocks)
            }
            filter.isNamedFilter() -> {
                withHeader(filter, mode, blocks)
                withColors(filter, blocks)
                withListStyle(filter, blocks)
                withSortBy(filter, blocks)
            }
            filter.isSnippetKit() -> {
                withHeader(filter, mode, blocks)
                withColors(filter, blocks)
                withListStyle(filter, blocks)
                withSortBy(filter, blocks)
                withSnippetKitPublicLink(filter, blocks)
                withSnippetKitPublicLibrary(filter, blocks)
            }
            filter.isStarred() -> {
                withHeader(filter, blocks)
                withListStyle(filter, blocks)
                withSortBy(filter, blocks)
            }
            filter.isClipboard() -> {
                withHeaderClipboard(filter, blocks)
                withListStyle(filter, blocks)
                withSortBy(filter, blocks)
            }
            filter.isDeleted() -> {
                withHeaderDeleted(filter, blocks)
                withListStyle(filter, blocks)
                withSortBy(filter, blocks)
                withDeleteLimits(filter, blocks)
            }
            filter.isGroup() -> {
                withHeaderGroup(filter, blocks)
                withSortBy(filter, blocks)
                if (filter.isGroupNotes()) {
                    withPinStarred(filter, blocks)
                }
            }
            filter.isLast() -> {
                withHeaderSaveAs(filter, blocks)
                withColors(filter, blocks)
                withListStyle(filter, blocks)
                withSortBy(filter, blocks)
            }
            else -> {
                withHeader(filter, blocks)
                withListStyle(filter, blocks)
                withSortBy(filter, blocks)
            }
        }
        withSpaceLg(blocks)
        return blocks
    }

    private fun withHeader(filter: Filter, blocks: MutableList<BlockItem<Fragment>>) {
        blocks.add(HeaderBlock(this, filter))
        blocks.add(SeparatorVerticalBlock(marginHoriz = 0))
    }

    private fun withHeaderSaveAs(filter: Filter, blocks: MutableList<BlockItem<Fragment>>) {
        blocks.add(
            HeaderBlock(
                viewModel = this,
                filter,
                actionIcon = R.drawable.action_edit,
                actionListener = this::onSaveAs
            )
        )
        blocks.add(SeparatorVerticalBlock(marginHoriz = 0))
    }

    private fun withHeaderGroup(filter: Filter, blocks: MutableList<BlockItem<Fragment>>) {
        blocks.add(
            HeaderBlock(
                viewModel = this,
                filter,
                actionIcon = R.drawable.filter_group_add,
                actionListener = this::onAddToGroup
            )
        )
        blocks.add(SeparatorVerticalBlock(marginHoriz = 0))
    }

    private fun withHeaderDeleted(filter: Filter, blocks: MutableList<BlockItem<Fragment>>) {
        blocks.add(
            HeaderBlock(
                viewModel = this,
                filter,
                actionIcon = R.drawable.ic_clear_all,
                actionListener = this::onClearRecycleBin
            )
        )
        blocks.add(SeparatorVerticalBlock(marginHoriz = 0))
    }

    private fun withHeaderClipboard(filter: Filter, blocks: MutableList<BlockItem<Fragment>>) {
        blocks.add(
            HeaderClipboardBlock(
                viewModel = this,
                rune = clipboardRuneProvider,
                filter = filter
            )
        )
        blocks.add(SeparatorVerticalBlock(marginHoriz = 0))
    }

    private fun withHeader(filter: Filter, mode: ViewMode, blocks: MutableList<BlockItem<Fragment>>) {
        if (mode == ViewMode.EDIT) {
            blocks.add(
                ObjectNameEditBlock(
                    text = filter.name,
                    hint = filter.getEditHint(app),
                    maxLength = appConfig.maxLengthTag(),
                    onRename = this::onRename,
                    onCancelEdit = this::onCancelEdit
                )
            )
        } else {
            blocks.add(
                ObjectNameViewBlock(
                    name = StyleHelper.getFilterLabel(app, filter),
                    uid = filter.uid,
                    color = filter.color,
                    hideHint = filter.hideHint,
                    iconRes = filter.getIconRes(),
                    onEdit = this::onEdit,
                    onDelete = this::onDelete,
                    onShowHint = this::onShowHint
                )
            )
        }
        blocks.add(SeparatorVerticalBlock(marginHoriz = 0))
    }

    private fun withColors(filter: Filter, blocks: MutableList<BlockItem<Fragment>>) {
        if (colorsMatrix.isNotEmpty()) {
            withSpaceLg(blocks)
            blocks.add(
                ColorWheelBlock(
                    selectedColor = filter.color,
                    colorsMatrix = colorsMatrix,
                    onChangeColor = this::onChangeColor
                )
            )
        }
    }

    private fun withSpaceXxs(blocks: MutableList<BlockItem<Fragment>>) {
        blocks.add(SpaceBlock.xxs())
    }

    private fun withSpaceMd(blocks: MutableList<BlockItem<Fragment>>) {
        blocks.add(SpaceBlock.md())
    }

    private fun withSpaceLg(blocks: MutableList<BlockItem<Fragment>>) {
        blocks.add(SpaceBlock.lg())
    }

    private fun withListStyle(filter: Filter, blocks: MutableList<BlockItem<Fragment>>) {
        withSpaceMd(blocks)
        blocks.add(TitleBlock(R.string.list_style_title))
        blocks.add(
            ListStyleWheelBlock(
                listStyle = filter.listStyle,
                onChanged = this::onApplyListStyle
            )
        )
    }

    private fun withSortBy(filter: Filter, blocks: MutableList<BlockItem<Fragment>>) {
        withSpaceMd(blocks)
        blocks.add(TitleBlock(R.string.list_config_sort))
        blocks.add(
            SortByWheelBlock(
                uid = filter.uid,
                sortBy = filter.sortBy,
                sortByItems = SortByExt.getItems(filter),
                onChanged = this::onApplySortBy
            )
        )
    }

    private fun withAutoRule(filter: Filter, blocks: MutableList<BlockItem<Fragment>>) {
        if (appConfig.canCreateTagAutoRules()) {
            withSpaceMd(blocks)
            blocks.add(SwitchWithHintBlock(
                dialogState = dialogState,
                titleRes = R.string.filter_tag_rule_title,
                hintIconRes = R.drawable.ic_hint_auto_rule,
                hintTitle = R.string.filter_tag_rule_title,
                hintDescription = R.string.filter_tag_rule_description,
                checked = filter.autoRulesEnabled,
                onChecked = { checked ->
                    filter.autoRulesEnabled = checked
                    filterStore.setValue(filter, force = true)
                }
            ))
            if (filter.autoRulesEnabled) {
                blocks.add(TextInputLayoutBlock(
                    text = filter.autoRuleByTextIn,
                    hint = string(R.string.filter_tag_rule_hint),
                    filters = arrayOf(InputFilter.LengthFilter(appConfig.tagRuleMaxLength())),
                    onTextChanged = { text ->
                        filter.autoRuleByTextIn = text?.toString()
                        null
                    }
                ))
            }
        }
    }

    private fun withPinStarred(filter: Filter, blocks: MutableList<BlockItem<Fragment>>) {
        withSpaceMd(blocks)
        blocks.add(SwitchWithHintBlock(
            dialogState = dialogState,
            titleRes = R.string.settings_pin_starred_title,
            hintIconRes = R.drawable.filter_starred,
            hintTitle = R.string.settings_pin_starred_title,
            hintDescription = R.string.settings_pin_starred_description,
            checked = filter.pinStarredEnabled,
            onChecked = { checked ->
                filter.pinStarredEnabled = checked
                filterStore.setValue(filter, force = true)
            }
        ))
    }

    private fun withDeleteLimits(filter: Filter, blocks: MutableList<BlockItem<Fragment>>) {
        val limitRange = appConfig.limitDeletedNotesRange()
        val limitDefault = appConfig.limitDeletedNotesDefault()
        val currentLimit = filter.limit?.takeIf { it != 0 } ?: limitDefault
        val limits = limitRange
            .plus(limitDefault)
            .plus(currentLimit)
            .distinct()
            .sorted()
            .let {
                if (it.first() == ClientSession.UNLIMITED) {
                    it.minus(ClientSession.UNLIMITED).plus(ClientSession.UNLIMITED)
                } else {
                    it
                }
            }
        blocks.add(SpaceBlock(12))
        blocks.add(SeekBarBlock(
            titleRes = R.string.filter_limit_title,
            descriptionRes = R.string.filter_limit_description,
            enabled = true,
            boldHeader = true,
            progress = limits.indexOf(currentLimit),
            maxValue = limits.size - 1,
            changeProvider = {
                val newLimit = limits[it]
                if (filter.limit != newLimit) {
                    filter.limit = newLimit
                }
                newLimit
            }
        ))
    }

    private fun withSnippetKitPublicLink(filter: Filter, blocks: MutableList<BlockItem<Fragment>>) {
        if (filter.isNew()) return
        val kit = filter.snippetKit
        if (kit == null || isMy(filter)) {
            val sharable = kit?.isSharable() == true && kit.publicLink != null
            withSpaceMd(blocks)
            blocks.add(SwitchWithHintBlock(
                userState = userState,
                dialogState = dialogState,
                internetState = internetState,
                titleRes = R.string.snippet_kit_public_link,
                hintIconRes = R.drawable.ic_hint,
                hintTitle = R.string.snippet_kit_public_link,
                hintDescription = R.string.snippet_kit_public_link_description,
                uncheckConfirmTitle = R.string.snippet_kit_public_link_remove_title,
                uncheckConfirmDescription = R.string.snippet_kit_public_link_remove_description,
                enabled = kit?.isPublished() != true,
                checked = sharable,
                onChecked = { checked ->
                    if (checked) {
                        onCreateSnippetKitLink(filter)
                    } else {
                        onRemoveSnippetKitLink(filter)
                    }
                }
            ))
            if (sharable) {
                kit?.publicLink?.let { link ->
                    withSpaceXxs(blocks)
                    blocks.add(CopiedLinkBlock(
                        link = link,
                        internetState = internetState,
                        onOpened = { dismiss() }
                    ))
                }
            }
        } else {
            kit.publicLink?.let { link ->
                withSpaceMd(blocks)
                blocks.add(TitleBlock(titleRes = R.string.snippet_kit_public_link))
                withSpaceXxs(blocks)
                blocks.add(CopiedLinkBlock(
                    link = link,
                    internetState = internetState,
                    onOpened = { dismiss() }
                ))
            }
        }
    }

    private fun withSnippetKitPublicLibrary(filter: Filter, blocks: MutableList<BlockItem<Fragment>>) {
        if (filter.isNew()) return
        val kit = filter.snippetKit
        if (kit == null || isMy(filter)) {
            withSpaceMd(blocks)
            val description = string(
                R.string.snippet_kit_public_library_description,
                appConfig.getSnippetsKitMinimumSize(),
                appConfig.getSnippetsKitBonusProgramUrl()
            )
            val checked = kit?.isPublic() == true && !kit.publicStatus.actionRequired
            val enabled = checked || (filter.notesCount >= appConfig.getSnippetsKitMinimumSize() && kit?.publicStatus != PublicStatus.BLOCKED)
            blocks.add(SwitchWithHintBlock(
                userState = userState,
                dialogState = dialogState,
                internetState = internetState,
                titleRes = R.string.snippet_kit_public_library,
                hintIconRes = R.drawable.ic_hint,
                hintTitle = R.string.snippet_kit_public_library,
                hintDescriptionText = description,
                uncheckConfirmTitle = R.string.snippet_kit_public_library_remove_title,
                uncheckConfirmDescription = R.string.snippet_kit_public_library_remove_description,
                checked = checked,
                enabled = enabled,
                onChecked = { isChecked ->
                    if (isChecked) {
                        onPublishSnippetKit(filter)
                    } else {
                        onDiscardSnippetKit(filter)
                    }
                }
            ))
            if (kit != null) {
                if (kit.isActionRequired()) {
                    kit.updateReason?.takeIf { it.isNotBlank() }?.let { reason ->
                        blocks.add(SpaceBlock(heightInDp = 4))
                        blocks.add(WarningBlock(
                            title = reason,
                            actionIcon = 0,
                            clickListener = {
                                AppContext.get().onCopy(
                                    text = reason,
                                    saveCopied = false,
                                    clearSelection = false
                                )
                            }
                        ))
                    }
                } else if (kit.isInReview()) {
                    blocks.add(SpaceBlock(heightInDp = 4))
                    blocks.add(
                        WarningBlock(
                            titleRes = R.string.public_status_review_details,
                            backgroundColor = app.getColorHint(),
                            textColor = Color.BLACK,
                            actionIcon = 0
                        )
                    )
                }
                if (checked) {
                    blocks.add(SpaceBlock(heightInDp = 12))
                    blocks.add(OutlinedButtonBlock(
                        titleRes = R.string.snippet_kit_label_show_in_public_library,
                        clickListener = { onOpenSnippetKit(kit) }
                    ))
                }
            }
        } else {
            withSpaceMd(blocks)
            blocks.add(OutlinedButtonBlock(
                titleRes = R.string.snippet_kit_label_show_in_public_library,
                clickListener = { onOpenSnippetKit(kit) }
            ))
        }
    }

    private fun onPublishSnippetKit(filter: Filter) {
        snippetRepository.get()
            .publishKit(filter)
            .doOnError { dialogState.showError(it) }
            .subscribeBy("onPublishSnippetKit", appState) { kit ->
                filter.snippetKit = kit
                filterStore.setValue(filter, force = true)
                saveFilterAction.execute(filter)
            }
    }

    private fun onDiscardSnippetKit(filter: Filter) {
        snippetRepository.get()
            .discardKit(filter)
            .doOnError { dialogState.showError(it) }
            .subscribeBy("onDiscardSnippetKit", appState) { kit ->
                filter.snippetKit = kit
                filterStore.setValue(filter, force = true)
                saveFilterAction.execute(filter)
            }
    }

    private fun onCreateSnippetKitLink(filter: Filter) {
        snippetRepository.get()
            .createLink(filter)
            .doOnError { dialogState.showError(it) }
            .subscribeBy("onCreateSnippetKitLink", appState) { kit ->
                filter.snippetKit = kit
                filterStore.setValue(filter, force = true)
                saveFilterAction.execute(filter)
            }
    }

    private fun onRemoveSnippetKitLink(filter: Filter) {
        snippetRepository.get()
            .removeLink(filter)
            .doOnError { dialogState.showError(it) }
            .subscribeBy("onRemoveSnippetKitLink", appState) { kit ->
                filter.snippetKit = kit
                filterStore.setValue(filter, force = true)
                saveFilterAction.execute(filter)
            }
    }

    private fun onOpenSnippetKit(kit: SnippetKit) {
        val args = SnippetKitDetailsViewModel.buildArgs(kit.id)
        appState.requestNavigateTo(R.id.action_snippet_kit_details, args)
        dismiss()
    }

    private fun isMy(filter: Filter): Boolean = filter.objectType != ObjectType.EXTERNAL_SNIPPET_KIT

}