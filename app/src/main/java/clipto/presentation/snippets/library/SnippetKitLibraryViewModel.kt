package clipto.presentation.snippets.library

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.Transformations
import clipto.analytics.Analytics
import clipto.common.presentation.mvvm.RxViewModel
import clipto.domain.SnippetKit
import clipto.domain.SnippetKitCategory
import clipto.presentation.blocks.*
import clipto.presentation.blocks.layout.ChipsRowBlock
import clipto.presentation.blocks.ux.EmptyStateVerticalBlock
import clipto.presentation.blocks.ux.ZeroStateHorizontalBlock
import clipto.presentation.blocks.ux.ZeroStateVerticalBlock
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.snippets.details.SnippetKitDetailsViewModel
import clipto.presentation.snippets.library.blocks.SnippetKitBlock
import clipto.repository.ISnippetRepository
import clipto.store.StoreObject
import clipto.store.app.AppState
import clipto.store.internet.InternetState
import clipto.store.user.UserState
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SnippetKitLibraryViewModel @Inject constructor(
    app: Application,
    private val appState: AppState,
    private val userState: UserState,
    private val internetState: InternetState,
    private val savedStateHandle: SavedStateHandle,
    private val snippetRepository: ISnippetRepository,
) : RxViewModel(app) {

    private val categoryStore: StoreObject<SnippetKitCategory> by lazy {
        StoreObject(
            id = "category",
            initialValue = savedStateHandle.get(ATTR_CATEGORY)
        )
    }
    private val categoriesLive = MutableLiveData<List<SnippetKitCategory>?>()

    private val emptyStateBlocks = listOf(EmptyStateVerticalBlock<SnippetKitLibraryFragment>())

    val blocksLive = MutableLiveData<List<BlockItem<SnippetKitLibraryFragment>>>()
    val refreshLive: MutableLiveData<Boolean> = MutableLiveData()

    val categoriesBlocksLive = Transformations.map(categoriesLive) { categories ->
        when {
            categories == null -> {
                listOf<BlockItem<SnippetKitLibraryFragment>>(ZeroStateHorizontalBlock())
            }
            categories.isEmpty() -> {
                emptyList()
            }
            else -> {
                val chips = categories.map { category ->
                    ChipBlock<SnippetKitCategory, SnippetKitLibraryFragment>(
                        model = category,
                        title = category.name,
                        checkable = false,
                        cornerRadius = 16f,
                        checked = category == categoryStore.getValue(),
                        onClicked = {
                            if (categoryStore.getValue() != category) {
                                onOpenCategory(category)
                            }
                        }
                    )
                }
                val selected = categories.indexOfFirst { it == categoryStore.getValue() }
                val row = ChipsRowBlock(chips, scrollToPosition = selected)
                listOf<BlockItem<SnippetKitLibraryFragment>>(row)
            }
        }
    }

    override fun doCreate() {
        onRefresh()
    }

    fun onRefresh() {
        onOpenCategory(categoryStore.getValue())
    }

    fun isMy(kit: SnippetKit): Boolean = kit.userId == userState.getUserId()

    private fun onOpenCategory(category: SnippetKitCategory?) {
        if (category == null) {
            snippetRepository.getCategories()
                .doOnSubscribe { categoriesLive.postValue(null) }
                .doOnError { categoriesLive.postValue(emptyList()) }
                .doOnSuccess { categories -> categoryStore.setValue(categories.firstOrNull()) }
                .doOnSuccess { categories -> categoriesLive.postValue(categories) }
                .flatMap { snippetRepository.getKits(categoryStore.getValue()) }
                .doOnSubscribe { blocksLive.postValue(listOf(ZeroStateVerticalBlock())) }
                .doFinally { refreshLive.postValue(false) }
                .map { it.map { kit -> SnippetKitBlock(kit = kit, viewModel = this) } }
                .map { it.ifEmpty { emptyStateBlocks } }
                .subscribeBy(
                    "onOpenCategory",
                    { blocksLive.postValue(it) },
                    { postEmptyState(it, category) }
                )
        } else {
            snippetRepository.getKits(category)
                .doOnSubscribe { blocksLive.postValue(listOf(ZeroStateVerticalBlock())) }
                .doOnSubscribe { categoryStore.setValue(category) }
                .doOnSubscribe { categoriesLive.value?.let { categoriesLive.postValue(it) } }
                .doFinally { refreshLive.postValue(false) }
                .map { it.map { kit -> SnippetKitBlock(kit = kit, viewModel = this) } }
                .map { it.ifEmpty { emptyStateBlocks } }
                .subscribeBy(
                    "onOpenCategory",
                    { blocksLive.postValue(it) },
                    { postEmptyState(it, category) }
                )
        }
    }

    private fun postEmptyState(th: Throwable, category: SnippetKitCategory?) {
        Analytics.onError("snippet_kits_onOpenCategory", th)
        val blocks = mutableListOf<BlockItem<SnippetKitLibraryFragment>>()
        val errorRes =
            if (internetState.isConnected()) {
                R.string.error_unexpected
            } else {
                R.string.error_internet_required
            }
        blocks.add(EmptyStateVerticalBlock(errorRes))
        blocks.add(PrimaryButtonBlock(
            titleRes = R.string.button_try_again,
            clickListener = { onOpenCategory(category) }
        ))
        blocksLive.postValue(blocks)
    }

    fun onOpenSnippetKit(kit: SnippetKit) {
        val args = SnippetKitDetailsViewModel.buildArgs(kit)
        appState.requestNavigateTo(R.id.action_snippet_kit_details, args)
    }

    companion object {
        private const val ATTR_CATEGORY = "attr_category"

        fun buildArgs(categoryId: String): Bundle = Bundle().apply {
            putString(ATTR_CATEGORY, categoryId)
        }
    }

}