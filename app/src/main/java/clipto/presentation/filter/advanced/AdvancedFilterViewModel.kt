package clipto.presentation.filter.advanced

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import clipto.common.presentation.mvvm.RxViewModel
import clipto.common.presentation.mvvm.lifecycle.SingleLiveData
import clipto.config.IAppConfig
import clipto.dao.objectbox.model.FilterBox
import clipto.domain.Filter
import clipto.extensions.*
import clipto.presentation.blocks.*
import clipto.presentation.blocks.layout.ChipsFlowBlock
import clipto.presentation.blocks.layout.ChipsRowBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.common.StyleHelper
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.dialog.select.date.SelectDateDialogRequest
import clipto.presentation.common.dialog.select.value.SelectValueDialogRequest
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.filter.advanced.blocks.TagsBlock
import clipto.presentation.filter.advanced.blocks.WhereTitleBlock
import clipto.store.app.AppState
import clipto.store.filter.FilterDetailsState
import clipto.store.filter.FilterState
import clipto.store.main.MainState
import clipto.store.user.UserState
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class AdvancedFilterViewModel @Inject constructor(
    app: Application,
    val appConfig: IAppConfig,
    val appState: AppState,
    val userState: UserState,
    val mainState: MainState,
    val filterState: FilterState,
    val dialogState: DialogState,
    private val filterDetailsState: FilterDetailsState
) : RxViewModel(app) {

    private var lastSnapshot = Filter.Snapshot()

    val filters = appState.getFilters()
    val settings = appState.getSettings()

    val counterLive = appState.filters.getLiveData()

    private val filterLive: MutableLiveData<Filter> by lazy {
        val activeFilter = FilterBox().apply(filters.findActive())
        activeFilter.activeFilterId = activeFilter.takeIf { it.isNamedFilter() }?.uid
        lastSnapshot = lastSnapshot.copy(activeFilter.anonymize())
        val live = MutableLiveData(activeFilter.anonymize())
        mainState.activeFilter.getLiveChanges()
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .filter { it.isNamedFilter() }
            .subscribeBy("activeFilter") { namedFilter ->
                if (activeFilter.activeFilterId != namedFilter.uid) {
                    activeFilter.withFilter(namedFilter).anonymize()
                    activeFilter.activeFilterId = namedFilter.uid
                    live.postValue(activeFilter)
                }
            }
        filterState.requestUpdateFilter.getLiveChanges()
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .filter { it.isNamedFilter() }
            .filter { it.uid != null && it.uid == activeFilter.activeFilterId }
            .subscribeBy("requestUpdateFilter") {
                live.postValue(activeFilter)
            }
        live
    }

    val saveAsLive = SingleLiveData<Boolean>()

    val blocksLive: LiveData<List<BlockItem<AdvancedFilterFragment>>> by lazy {
        Transformations.map(filterLive) { filter ->
            val blocks = mutableListOf<BlockItem<AdvancedFilterFragment>>()
            if (filter != null) {
                withNamedFilters(filter, blocks)

                withTextContains(filter, blocks)

                if (filters.hasTags()) {
                    withSpaceLg(blocks)
                    withTags(filter, blocks)
                }

                withSpaceLg(blocks)
                withCategories(filter, blocks)

                withSpaceLg(blocks)
                withTextTypes(filter, blocks)

                withSpaceLg(blocks)
                withCreated(filter, blocks)

                withSpaceLg(blocks)
                withUpdated(filter, blocks)

                if (userState.isAuthorized()) {
                    withSpaceLg(blocks)
                    withAttachments(filter, blocks)

                    withSpaceMd(blocks)
                    withPublicLink(filter, blocks)

                    withSpaceMd(blocks)
                    withNotSynced(filter, blocks)
                }
            }
            blocks
        }
    }

    fun getTags(filter: Filter): List<Filter> {
        return filter.tagIds.mapNotNull { filters.findFilterByTagId(it) }
    }

    fun onChangeTags(filter: Filter, tagIds: List<String>) {
        val currentTags = filter.tagIds.filter { filters.findFilterByTagId(it) != null }
        if (currentTags.containsAll(tagIds) && currentTags.size == tagIds.size) {
            //
        } else {
            filter.tagIds = tagIds
            onApplyFilter(filter)
        }
    }

    fun onSaveAs() {
        filterLive.value?.let { filter ->
            val request = FilterDetailsState.OpenFilterRequest(
                filter = filter.asNew(),
                requestApply = true
            )
            filterDetailsState.requestOpenFilter(request)
        }
    }

    fun onClearFilter() {
        mainState.requestClearFilter()
        dismiss()
    }

    private fun withSpaceMd(blocks: MutableList<BlockItem<AdvancedFilterFragment>>) {
        blocks.add(SpaceBlock(16))
    }

    private fun withSpaceLg(blocks: MutableList<BlockItem<AdvancedFilterFragment>>) {
        blocks.add(SpaceBlock(24))
    }

    private fun withTextContains(filter: Filter, blocks: MutableList<BlockItem<AdvancedFilterFragment>>) {
        blocks.add(TitleBlock(R.string.advanced_filter_block_text))
        blocks.add(TextInputLayoutBlock(
            text = filter.textLike,
            hint = string(R.string.filter_tag_rule_hint),
            onTextChanged = { text ->
                filter.textLike = text?.toString()
                onApplyFilter(filter)
                null
            }
        ))
    }

    private fun withNamedFilters(filter: Filter, blocks: MutableList<BlockItem<AdvancedFilterFragment>>) {
        val activeFilterId = filter.activeFilterId
        var scrollToPosition = -1
        var hasActiveFilter = false
        val items = filters.getSortedNamedFilters()
            .mapIndexed { index, namedFilter ->
                val isActive = activeFilterId != null && activeFilterId == namedFilter.uid
                hasActiveFilter = hasActiveFilter || isActive
                if (hasActiveFilter && scrollToPosition == -1) {
                    scrollToPosition = index
                }
                ChipBlock<Filter, AdvancedFilterFragment>(
                    model = namedFilter,
                    title = namedFilter.getTitle(app),
                    iconRes = namedFilter.getIconRes(),
                    backgroundColor = namedFilter.getTagChipColor(app) ?: app.getTextColorAccent(),
                    checked = isActive,
                    onClicked = { checked ->
                        if (checked) {
                            val newFilter = filter.withFilter(namedFilter).anonymize()
                            onApplyFilter(newFilter, activeFilterId = namedFilter.uid)
                        } else {
                            val newFilter = filter.withFilter(filters.all).anonymize()
                            onApplyFilter(newFilter)
                        }
                    }
                )
            }
            .toMutableList()

        saveAsLive.postValue(!hasActiveFilter && filter.hasActiveFilter())

        if (items.isNotEmpty()) {
            blocks.add(SpaceBlock(12))
            blocks.add(ChipsRowBlock(items, scrollToPosition = scrollToPosition))
            blocks.add(SpaceBlock(12))
        }
    }

    private fun withTextTypes(filter: Filter, blocks: MutableList<BlockItem<AdvancedFilterFragment>>) {
        blocks.add(SeparateScreenBlock(
            withBoldHeader = true,
            titleRes = R.string.advanced_filter_block_text_type,
            clickListener = {
                val data = SelectValueDialogRequest(
                    title = string(R.string.advanced_filter_block_text_type),
                    options = TextTypeExt.types.map { type ->
                        SelectValueDialogRequest.Option(
                            title = string(type.titleRes),
                            checked = filter.textTypeIn.contains(type.type),
                            iconRes = type.iconRes,
                            model = type.type
                        )
                    },
                    onSelected = { types ->
                        filter.textTypeIn = types
                        onApplyFilter(filter)
                    },
                    withImmediateNotify = true
                )
                dialogState.requestSelectValueDialog(data)
            }
        ))
        val items = filter.textTypeIn
            .map { it.toExt() }
            .map { type ->
                ChipBlock<TextTypeExt, AdvancedFilterFragment>(
                    model = type,
                    title = string(type.titleRes),
                    iconRes = type.iconRes,
                    checked = filter.textTypeIn.contains(type.type),
                    onClicked = { checked ->
                        val newTypes = if (checked) filter.textTypeIn.plus(type.type) else filter.textTypeIn.minus(type.type)
                        filter.textTypeIn = newTypes
                        onApplyFilter(filter)
                    }
                )
            }
        blocks.add(ChipsFlowBlock(items, TextTypeExt::class.java))
    }

    private fun withCategories(filter: Filter, blocks: MutableList<BlockItem<AdvancedFilterFragment>>) {
        blocks.add(WhereTitleBlock(
            viewModel = this,
            titleRes = R.string.advanced_filter_block_category,
            whereType = filter.locatedInWhereType,
            whereOptions = arrayOf(Filter.WhereType.ANY_OF, Filter.WhereType.NONE_OF),
            onWhereTypeChanged = { whereType ->
                filter.locatedInWhereType = whereType
                onApplyFilter(filter)
            },
            onClickListener = {
                val options = listOf(
                    SelectValueDialogRequest.Option(
                        iconRes = filters.starred.getIconRes(),
                        title = filters.starred.getTitle(app),
                        model = filters.starred,
                        checked = filter.starred,
                    ),
                    SelectValueDialogRequest.Option(
                        iconRes = filters.untagged.getIconRes(),
                        title = filters.untagged.getTitle(app),
                        model = filters.untagged,
                        checked = filter.untagged,
                    ),
                    SelectValueDialogRequest.Option(
                        iconRes = filters.clipboard.getIconRes(),
                        title = filters.clipboard.getTitle(app),
                        model = filters.clipboard,
                        checked = filter.clipboard,
                    ),
                    SelectValueDialogRequest.Option(
                        iconRes = filters.snippets.getIconRes(),
                        title = filters.snippets.getTitle(app),
                        model = filters.snippets,
                        checked = filter.snippets,
                    ),
                    SelectValueDialogRequest.Option(
                        iconRes = filters.deleted.getIconRes(),
                        title = filters.deleted.getTitle(app),
                        model = filters.deleted,
                        checked = filter.recycled,
                    )
                )
                val data = SelectValueDialogRequest(
                    title = string(R.string.advanced_filter_block_category),
                    withImmediateNotify = true,
                    options = options,
                    onSelected = { selected ->
                        filter.starred = selected.contains(filters.starred)
                        filter.untagged = selected.contains(filters.untagged)
                        filter.clipboard = selected.contains(filters.clipboard)
                        filter.snippets = selected.contains(filters.snippets)
                        filter.recycled = selected.contains(filters.deleted)
                        onApplyFilter(filter)
                    }
                )
                dialogState.requestSelectValueDialog(data)
            }
        ))
        val items = mutableListOf<ChipBlock<Filter, AdvancedFilterFragment>>()
        if (filter.starred) {
            items.add(ChipBlock(
                model = filters.starred,
                title = filters.starred.getTitle(app),
                checked = filter.starred,
                onClicked = { checked ->
                    filter.starred = checked
                    onApplyFilter(filter)
                }
            ))
        }
        if (filter.untagged) {
            items.add(ChipBlock(
                model = filters.untagged,
                title = filters.untagged.getTitle(app),
                checked = filter.untagged,
                onClicked = { checked ->
                    filter.untagged = checked
                    onApplyFilter(filter)
                }
            ))
        }
        if (filter.clipboard) {
            items.add(ChipBlock(
                model = filters.clipboard,
                title = filters.clipboard.getTitle(app),
                checked = filter.clipboard,
                onClicked = { checked ->
                    filter.clipboard = checked
                    onApplyFilter(filter)
                }
            ))
        }
        if (filter.snippets) {
            items.add(ChipBlock(
                model = filters.snippets,
                title = filters.snippets.getTitle(app),
                checked = filter.snippets,
                onClicked = { checked ->
                    filter.snippets = checked
                    onApplyFilter(filter)
                }
            ))
        }
        if (filter.recycled) {
            items.add(ChipBlock(
                model = filters.deleted,
                title = filters.deleted.getTitle(app),
                checked = filter.recycled,
                onClicked = { checked ->
                    filter.recycled = checked
                    onApplyFilter(filter)
                }
            ))
        }
        blocks.add(ChipsFlowBlock(items, Filter::class.java))
    }

    private fun withTags(filter: Filter, blocks: MutableList<BlockItem<AdvancedFilterFragment>>) {
        blocks.add(WhereTitleBlock(
            viewModel = this,
            titleRes = R.string.advanced_filter_block_tags,
            whereType = filter.tagIdsWhereType,
            whereOptions = arrayOf(Filter.WhereType.ANY_OF, Filter.WhereType.ALL_OF),
            onWhereTypeChanged = { type ->
                filter.tagIdsWhereType = type
                onApplyFilter(filter)
            },
            onClickListener = {
                val data = SelectValueDialogRequest(
                    withImmediateNotify = true,
                    title = string(R.string.advanced_filter_block_tags),
                    options = filters.getSortedTags().map { tag ->
                        SelectValueDialogRequest.Option(
                            title = StyleHelper.getFilterLabel(app, tag),
                            checked = filter.tagIds.contains(tag.uid),
                            iconColor = tag.getIconColor(app),
                            iconRes = tag.getIconRes(),
                            model = tag
                        )
                    },
                    onSelected = { tags ->
                        onChangeTags(filter, tags.mapNotNull { it.uid })
                    }
                )
                dialogState.requestSelectValueDialog(data)
            }
        ))
        blocks.add(TagsBlock(
            viewModel = this,
            filter = filter,
            onClicked = { tag, checked ->
                val tagIds =
                    if (checked) {
                        filter.tagIds.plus(tag.uid).filterNotNull().distinct()
                    } else {
                        filter.tagIds.minus(tag.uid).filterNotNull()
                    }
                onChangeTags(filter, tagIds)
            }
        ))
    }

    private fun withCreated(filter: Filter, blocks: MutableList<BlockItem<AdvancedFilterFragment>>) {
        blocks.add(SeparateScreenBlock(
            withBoldHeader = true,
            titleRes = R.string.advanced_filter_block_created,
            value = filter.getCreateDateRangeLabel(app),
            clickListener = {
                val request = SelectDateDialogRequest(
                    withImmediateNotify = true,
                    title = string(R.string.advanced_filter_block_created),
                    selection = SelectDateDialogRequest.Selection(filter.createDatePeriod, filter.createDateFrom, filter.createDateTo),
                    onSelected = {
                        filter.createDateFrom = it?.dateFrom
                        filter.createDateTo = it?.dateTo
                        filter.createDatePeriod = it?.model
                        onApplyFilter(filter)
                    }
                )
                dialogState.requestSelectDateDialog(request)
            }
        ))
    }

    private fun withUpdated(filter: Filter, blocks: MutableList<BlockItem<AdvancedFilterFragment>>) {
        blocks.add(SeparateScreenBlock(
            withBoldHeader = true,
            titleRes = R.string.advanced_filter_block_updated,
            value = filter.getUpdateDateRangeLabel(app),
            clickListener = {
                val request = SelectDateDialogRequest(
                    withImmediateNotify = true,
                    title = string(R.string.advanced_filter_block_updated),
                    selection = SelectDateDialogRequest.Selection(filter.updateDatePeriod, filter.updateDateFrom, filter.updateDateTo),
                    onSelected = {
                        filter.updateDateFrom = it?.dateFrom
                        filter.updateDateTo = it?.dateTo
                        filter.updateDatePeriod = it?.model
                        onApplyFilter(filter)
                    }
                )
                dialogState.requestSelectDateDialog(request)
            }
        ))
    }

    private fun withAttachments(filter: Filter, blocks: MutableList<BlockItem<AdvancedFilterFragment>>) {
        blocks.add(SwitchBlock(
            titleRes = R.string.advanced_filter_block_only_attachments,
            checked = filter.showOnlyWithAttachments,
            textSize = 14,
            clickListener = { _, isChecked ->
                filter.showOnlyWithAttachments = isChecked
                onApplyFilter(filter)
            }
        ))
    }

    private fun withPublicLink(filter: Filter, blocks: MutableList<BlockItem<AdvancedFilterFragment>>) {
        blocks.add(SwitchBlock(
            titleRes = R.string.advanced_filter_block_only_public_links,
            checked = filter.showOnlyWithPublicLink,
            textSize = 14,
            clickListener = { _, isChecked ->
                filter.showOnlyWithPublicLink = isChecked
                onApplyFilter(filter)
            }
        ))
    }

    private fun withNotSynced(filter: Filter, blocks: MutableList<BlockItem<AdvancedFilterFragment>>) {
        blocks.add(SwitchBlock(
            titleRes = R.string.advanced_filter_block_only_not_synced,
            checked = filter.showOnlyNotSynced,
            textSize = 14,
            clickListener = { _, isChecked ->
                filter.showOnlyNotSynced = isChecked
                onApplyFilter(filter)
            }
        ))
    }

    private fun onApplyFilter(filter: Filter, activeFilterId: String? = null) {
        val nextSnapshot = lastSnapshot.copy(filter)
        if (activeFilterId != null || lastSnapshot != nextSnapshot) {
            lastSnapshot = nextSnapshot
            filter.activeFilterId = activeFilterId
            Single
                .fromCallable { mainState.requestApplyFilter(filter, closeNavigation = false) }
                .delaySubscription(appConfig.getUiTimeout(), TimeUnit.MILLISECONDS)
                .subscribeBy("onApplyFilter") { }
            filterLive.postValue(filter)
        }
    }

}