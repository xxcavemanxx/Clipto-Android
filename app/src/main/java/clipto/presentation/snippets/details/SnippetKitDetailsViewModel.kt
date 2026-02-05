package clipto.presentation.snippets.details

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.Transformations
import clipto.AppContext
import clipto.AppUtils
import clipto.action.KitInstallAction
import clipto.action.KitUninstallAction
import clipto.action.SaveFilterAction
import clipto.common.extensions.notNull
import clipto.common.extensions.toEmoji
import clipto.common.misc.FormatUtils
import clipto.common.misc.GsonUtils
import clipto.common.misc.IntentUtils
import clipto.common.presentation.mvvm.RxViewModel
import clipto.domain.*
import clipto.dynamic.DynamicTextHelper
import clipto.extensions.getLanguageLabel
import clipto.extensions.getTitleRes
import clipto.presentation.blocks.*
import clipto.presentation.blocks.layout.RowBlock
import clipto.presentation.blocks.ux.*
import clipto.presentation.common.StyleHelper
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.dialog.hint.HintDialogData
import clipto.presentation.common.dialog.select.value.SelectValueDialogRequest
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.snippets.details.blocks.SnippetBlock
import clipto.presentation.snippets.view.SnippetKitViewModel
import clipto.repository.ISnippetRepository
import clipto.repository.data.SnippetKitUpdateData
import clipto.store.app.AppState
import clipto.store.internet.InternetState
import clipto.store.main.MainState
import clipto.store.user.UserState
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Single
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SnippetKitDetailsViewModel @Inject constructor(
    app: Application,
    private val appState: AppState,
    private val userState: UserState,
    private val mainState: MainState,
    private val dialogState: DialogState,
    private val internetState: InternetState,
    private val savedStateHandle: SavedStateHandle,
    private val snippetRepository: ISnippetRepository,
    private val saveFilterAction: SaveFilterAction,
    private val uninstallKitAction: KitUninstallAction,
    private val installKitAction: KitInstallAction,
    val textHelper: DynamicTextHelper
) : RxViewModel(app) {

    private var updateData: SnippetKitUpdateData = SnippetKitUpdateData()
    private var categories = listOf<SnippetKitCategory>()
    private var accessRole: UserRole = UserRole.USER

    fun getTextSize() = mainState.getListConfig().textSize
    fun getTextFont() = mainState.getListConfig().textFont

    private var expandedDescription = true

    private val kit: SnippetKit? by lazy { savedStateHandle.get(ATTR_KIT) }

    private val kitId: String? by lazy { savedStateHandle.get(ATTR_KIT_ID) }

    private var fetchError: Throwable? = null

    val kitLive: MutableLiveData<SnippetKit> by lazy {
        val live = MutableLiveData<SnippetKit>(kit)
        if (kit == null) {
            onFetchById(kitId, live)
        }
        live
    }

    val blocksLive: LiveData<List<BlockItem<SnippetKitDetailsFragment>>> by lazy {
        Transformations.map(kitLive) { kit ->
            val blocks = mutableListOf<BlockItem<SnippetKitDetailsFragment>>()

            if (kit == null) {
                blocks.add(ZeroStateVerticalBlock())
            } else if (kit === SnippetKit.NOT_FOUND) {
                val errorRes =
                    if (internetState.isConnected()) {
                        R.string.error_unexpected
                    } else {
                        R.string.error_internet_required
                    }
                blocks.add(EmptyStateVerticalBlock(errorRes))
                blocks.add(PrimaryButtonBlock(
                    titleRes = R.string.button_try_again,
                    clickListener = { onFetchById(kitId, kitLive) }
                ))
            } else if (kit.publicStatus != PublicStatus.RESTRICTED) {
                // attrs
                val attrs = mutableListOf<BlockItem<SnippetKitDetailsFragment>>()
                // snippets
                attrs.add(AttrHorizontalBlock(title = string(R.string.snippet_kit_attr_snippets), value = kit.snippetsCount.toString()))
                attrs.add(SeparatorHorizontalBlock())
                // downloads
                attrs.add(AttrHorizontalBlock(title = string(R.string.snippet_kit_attr_downloads), value = kit.installs.toString()))
                attrs.add(SeparatorHorizontalBlock())
                if (kit.language != null && kit.country != null) {
                    // language
                    attrs.add(AttrHorizontalBlock(title = string(R.string.snippet_kit_attr_language), value = kit.getLanguageLabel()))
                    attrs.add(SeparatorHorizontalBlock())
                }
                // created
                attrs.add(AttrHorizontalBlock(title = string(R.string.snippet_kit_attr_created), value = FormatUtils.formatDate(kit.created)))
                // updated
                if (kit.updated != null) {
                    attrs.add(SeparatorHorizontalBlock())
                    attrs.add(AttrHorizontalBlock(title = string(R.string.snippet_kit_attr_updated), value = FormatUtils.formatDate(kit.updated)))
                }

                // ATTRS
                blocks.add(RowBlock(attrs, spacingInDp = 0, scrollToPosition = 0))

                // ERROR
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
                }

                // SPACE
                blocks.add(SpaceBlock(heightInDp = 24))

                // INSTALL / UNINSTALL / UPDATE
                val filters = appState.getFilters()
                val filter = filters.findFilterBySnippetKit(kit)
                log("check hash :: {} -> {}", kit.hash, filter?.snippetKit?.hash)
                when {
                    kit.userId == userState.getUserId() -> {
                        if (kit.publicStatus != PublicStatus.DELETED) {
                            blocks.add(PrimaryButtonBlock(
                                titleRes = R.string.menu_share_link,
                                clickListener = { onShareMyLink() }
                            ))
                            blocks.add(SpaceBlock(heightInDp = 8))
                        }
                    }
                    filter == null -> {
                        blocks.add(PrimaryButtonBlock(
                            titleRes = R.string.button_install,
                            clickListener = { onInstall() }
                        ))
                        blocks.add(TextButtonBlock(
                            titleRes = R.string.snippet_kit_install_hint_title,
                            clickListener = {
                                dialogState.showHint(
                                    HintDialogData(
                                        title = string(R.string.snippet_kit_install_hint_title),
                                        description = string(R.string.snippet_kit_install_hint_description),
                                        descriptionIsMarkdown = true
                                    )
                                )
                            }
                        ))
                    }
                    kit.hash == filter.snippetKit?.hash || kit === this.kit -> {
                        blocks.add(OutlinedButtonBlock(
                            titleRes = R.string.button_uninstall,
                            clickListener = { onUninstall() }
                        ))
                        blocks.add(SpaceBlock(heightInDp = 8))
                    }
                    else -> {
                        blocks.add(TwoButtonsBlock(
                            primaryTitleRes = R.string.button_update,
                            primaryClickListener = { onInstall() },
                            secondaryTitleRes = R.string.button_uninstall,
                            secondaryClickListener = { onUninstall() }
                        ))
                        blocks.add(SpaceBlock(heightInDp = 8))
                    }
                }

                // DESCRIPTION
                kit.description?.takeIf { it.isNotBlank() }?.let { description ->
                    blocks.add(
                        SeparateScreenBlock(
                            titleRes = R.string.common_label_description,
                            withBoldHeader = true,
                            withActionIcon = getExpandedIcon(),
                            withBadge = !expandedDescription,
                            clickListener = {
                                expandedDescription = !expandedDescription
                                kitLive.value?.let { kitLive.postValue(it) }
                            }
                        )
                    )
                    if (expandedDescription) {
                        blocks.add(DescriptionBlock(description, getTextFont(), getTextSize()))
                    }
                }

                val snippets = kit.getSortedSnippets().map { SnippetBlock(this, it) }
                if (snippets.isEmpty()) {
                    if (kit === this.kit) {
                        blocks.add(ZeroStateVerticalBlock())
                    } else {
                        blocks.add(EmptyStateVerticalBlock())
                    }
                } else {
                    blocks.add(SpaceBlock(heightInDp = 8))
                    blocks.addAll(snippets)
                }

                if (accessRole == UserRole.ADMIN) {
                    val updateReason = updateData.message ?: kit.updateReason
                    val publicStatus = updateData.status ?: kit.publicStatus
                    val categoryId = updateData.categoryId ?: kit.categoryId
                    val language = updateData.language ?: kit.language
                    val country = updateData.country ?: kit.country
                    blocks.add(SpaceBlock(heightInDp = 16))
                    blocks.add(SeparatorVerticalBlock(marginHoriz = 0))
                    blocks.add(SeparatorVerticalBlock(marginHoriz = 0))
                    blocks.add(SeparatorVerticalBlock(marginHoriz = 0))
                    blocks.add(SeparatorVerticalBlock(marginHoriz = 0))
                    blocks.add(SeparatorVerticalBlock(marginHoriz = 0))
                    blocks.add(SeparateScreenBlock(
                        titleRes = R.string.snippet_kit_attr_language,
                        value = language,
                        withBoldHeader = true,
                        clickListener = {
                            kitLive.value?.let { kit ->
                                snippetRepository.getLanguages(kit).subscribeBy("getLanguages", appState) { langs ->
                                    val data = SelectValueDialogRequest(
                                        title = string(R.string.snippet_kit_attr_language),
                                        options = langs.map { lang ->
                                            SelectValueDialogRequest.Option(
                                                title = "${lang.code} (${lang.confidence})",
                                                checked = lang.code == language,
                                                model = lang
                                            )
                                        },
                                        single = true,
                                        withClearAll = false,
                                        withImmediateNotify = true,
                                        onSelected = { selection ->
                                            updateData = updateData.copy(language = selection.firstOrNull()?.code)
                                            kitLive.postValue(kit)
                                        }
                                    )
                                    dialogState.requestSelectValueDialog(data)
                                }
                            }
                        }
                    ))
                    blocks.add(SeparateScreenBlock(
                        titleRes = R.string.snippet_kit_attr_country,
                        value = country,
                        withBoldHeader = true,
                        clickListener = {
                            kitLive.value?.let { kit ->
                                val data = SelectValueDialogRequest(
                                    title = string(R.string.snippet_kit_attr_country),
                                    options = Locale.getAvailableLocales()
                                        .distinctBy { it.country }
                                        .filter { it.toEmoji() != null }
                                        .filter { it.country.toIntOrNull() == null }
                                        .sortedBy { it.country }
                                        .map { locale ->
                                            val title = "${locale.toEmoji()}    ${locale.getDisplayCountry(Locale.getDefault())}"
                                            SelectValueDialogRequest.Option(
                                                checked = locale.country == country,
                                                title = title,
                                                model = locale
                                            )
                                        },
                                    single = true,
                                    withClearAll = false,
                                    withImmediateNotify = true,
                                    onSelected = { selection ->
                                        updateData = updateData.copy(country = selection.firstOrNull()?.country)
                                        kitLive.postValue(kit)
                                    }
                                )
                                dialogState.requestSelectValueDialog(data)
                            }
                        }
                    ))
                    blocks.add(SeparateScreenBlock(
                        titleRes = R.string.public_status,
                        value = string(publicStatus.getTitleRes()),
                        withBoldHeader = true,
                        clickListener = {
                            val data = SelectValueDialogRequest(
                                title = string(R.string.public_status),
                                options = PublicStatus.values().map { status ->
                                    SelectValueDialogRequest.Option(
                                        title = string(status.getTitleRes()),
                                        checked = status == publicStatus,
                                        model = status
                                    )
                                },
                                single = true,
                                withClearAll = false,
                                withImmediateNotify = true,
                                onSelected = { statuses ->
                                    updateData = updateData.copy(status = statuses.firstOrNull())
                                    kitLive.postValue(kit)
                                }
                            )
                            dialogState.requestSelectValueDialog(data)
                        }
                    ))
                    blocks.add(SeparateScreenBlock(
                        titleRes = R.string.snippet_kit_attr_category,
                        value = categories.find { it.id == categoryId }?.name,
                        withBoldHeader = true,
                        clickListener = {
                            val data = SelectValueDialogRequest(
                                title = string(R.string.snippet_kit_attr_category),
                                options = categories.map { category ->
                                    SelectValueDialogRequest.Option(
                                        title = category.name,
                                        checked = category.id == categoryId,
                                        model = category
                                    )
                                },
                                single = true,
                                withClearAll = false,
                                withImmediateNotify = true,
                                onSelected = { categories ->
                                    updateData = updateData.copy(categoryId = categories.firstOrNull()?.id)
                                    kitLive.postValue(kit)
                                }
                            )
                            dialogState.requestSelectValueDialog(data)
                        }
                    ))
                    blocks.add(TextInputLayoutBlock(
                        hint = string(R.string.public_status_update_reason),
                        maxLines = Integer.MAX_VALUE,
                        text = updateReason,
                        onTextChanged = {
                            updateData = updateData.copy(message = it?.toString())
                            null
                        }
                    ))
                    blocks.add(SpaceBlock(heightInDp = 16))
                    blocks.add(PrimaryButtonBlock(
                        titleRes = R.string.button_save,
                        clickListener = {
                            onUpdate(updateData)
                        }
                    ))
                }
            }

            blocks
        }
    }

    override fun doSubscribe() {
        onRefresh()
    }

    private fun onRefresh() {
        kitLive.value?.let { kit ->
            val id = kit.takeIf { it !== SnippetKit.NOT_FOUND }?.id ?: kitId
            onFetchById(id, kitLive)
        }
    }

    fun onShare() {
        kitLive.value?.let { kit ->
            if (kit === SnippetKit.NOT_FOUND) {
                AppUtils.sendRequest(
                    title = app.getString(R.string.error_unexpected),
                    info = StringBuilder()
                        .appendLine()
                        .append("kit_id: ")
                        .append(kitId.notNull())
                        .appendLine()
                        .appendLine()
                        .append("kit: ")
                        .append(GsonUtils.toStringSilent(this.kit))
                        .appendLine()
                        .toString(),
                    th = fetchError
                )
            } else {
                if (kit.userId == userState.getUserId()) {
                    onShareMyLink()
                } else {
                    kit.publicLink?.let { IntentUtils.share(app, it) }
                }
            }
        }
    }

    private fun onShareMyLink() {
        kitLive.value?.let { kit ->
            if (kit.sharable && kit.publicLink != null) {
                IntentUtils.share(app, kit.publicLink)
            } else {
                appState.getFilters().findFilterBySnippetKit(kit)?.let { filter ->
                    snippetRepository
                        .createLink(filter)
                        .doOnError { dialogState.showError(it) }
                        .observeOn(getViewScheduler())
                        .subscribeBy("onShareMyLink", appState) { kit ->
                            filter.snippetKit = kit
                            saveFilterAction.execute(filter)
                            IntentUtils.share(app, kit.publicLink)
                        }
                }
            }
        }
    }

    private fun onFetchById(id: String?, live: MutableLiveData<SnippetKit>) {
        if (id != null) {
            snippetRepository.getKit(id, force = fetchError != null)
                .flatMap { data ->
                    if (data.accessRole == UserRole.ADMIN) {
                        snippetRepository.getCategories()
                            .doOnSuccess { categories = it }
                            .map { data }
                    } else {
                        Single.just(data)
                    }
                }
                .doOnSubscribe { if (live.value == null || live.value == SnippetKit.NOT_FOUND) live.postValue(null) }
                .doOnSubscribe { fetchError = null }
                .doOnError { fetchError = it }
                .subscribeBy(
                    "onFetchById",
                    {
                        accessRole = it.accessRole
                        live.postValue(it.kit)
                    },
                    { live.postValue(SnippetKit.NOT_FOUND) }
                )
        }
    }

    fun onOpen(snippet: Snippet) {
        kitLive.value?.let { kit ->
            SnippetKitViewModel.buildArgs(kit, snippet)?.let { args ->
                appState.requestNavigateTo(R.id.action_snippet_kit, args)
            }
        }
    }

    fun onCopy(snippet: Snippet) {
        val kitId = kitLive.value?.id.notNull()
        AppContext.get().onCopy(
            clip = snippet.asClip(kitId),
            clearSelection = false
        )
    }

    private fun onInstall() {
        kitLive.value?.let { kit ->
            installKitAction.execute(kit) { filter ->
                filter.snippetKit?.let {
                    kitLive.postValue(
                        it.copy(
                            snippetsCount = kitLive.value?.snippetsCount ?: it.snippetsCount,
                            snippets = kitLive.value?.snippets ?: it.snippets
                        )
                    )
                }
            }
        }
    }

    private fun onUninstall() {
        kitLive.value?.let { kit ->
            uninstallKitAction.execute(kit) {
                kitLive.postValue(kit)
            }
        }
    }

    private fun onUpdate(data: SnippetKitUpdateData) {
        kitLive.value?.let { kit ->
            internetState.withInternet(
                success = {
                    snippetRepository.update(kit, data)
                        .doOnError { dialogState.showError(it) }
                        .subscribeBy("onUpdateStatus", appState) {
                            kitLive.postValue(it.copy(snippets = kit.getSortedSnippets()))
                        }
                }
            )
        }
    }

    private fun getExpandedIcon(): Int = StyleHelper.getExpandIcon(expandedDescription)

    companion object {
        private const val ATTR_KIT = "attr_kit"

        private const val ATTR_KIT_ID = "attr_kit_id"

        fun buildArgs(kit: SnippetKit): Bundle = Bundle().apply {
            putSerializable(ATTR_KIT, kit)
        }

        fun buildArgs(kitId: String): Bundle = Bundle().apply {
            putSerializable(ATTR_KIT_ID, kitId)
        }
    }

}