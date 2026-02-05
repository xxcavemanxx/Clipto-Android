package clipto.presentation.main.list

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View.OnClickListener
import android.view.inputmethod.EditorInfo
import androidx.core.view.GravityCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import clipto.AppContext
import clipto.action.intent.provider.*
import clipto.common.extensions.*
import clipto.common.logging.L
import clipto.common.misc.AndroidUtils
import clipto.common.misc.ThemeUtils
import clipto.common.misc.Units
import clipto.common.presentation.mvvm.ActivityBackPressConsumer
import clipto.common.presentation.mvvm.MvvmFragment
import clipto.common.presentation.mvvm.base.StatefulFragment
import clipto.common.presentation.state.MenuState
import clipto.common.presentation.state.ToolbarState
import clipto.domain.Clip
import clipto.domain.Settings
import clipto.extensions.*
import clipto.presentation.common.fragment.attributed.config.ConfigAttributedObjectFragment
import clipto.presentation.common.view.DoubleClickItemListenerWrapper
import clipto.presentation.config.list.ClipListConfigFragment
import clipto.presentation.main.list.adapters.MainListAdapter
import clipto.store.main.ScreenState
import com.google.android.material.snackbar.Snackbar
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_main_list.*
import kotlin.collections.set
import kotlin.math.min

@AndroidEntryPoint
class MainListFragment : MvvmFragment<MainListViewModel>(), StatefulFragment, ActivityBackPressConsumer {

    override val layoutResId: Int = R.layout.fragment_main_list
    override val viewModel: MainListViewModel by viewModels()
    override fun bindOnFirstLayout(): Boolean = true

    private var hasPendingUpdates: Boolean = false
    private val mainAdapter = MainListAdapter()

    override fun onConfigurationChanged(newConfig: Configuration) {
        recyclerView?.let { mainAdapter.onScreenChanged(it) }
        super.onConfigurationChanged(newConfig)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden && hasPendingUpdates) {
            viewModel.refresh()
        }
    }

    override fun onBackPressConsumed(): Boolean {
        if (drawerLayout?.isDrawerVisible(GravityCompat.START) == true) {
            drawerLayout?.closeDrawer(GravityCompat.START)
            return true
        }
        try {
            if (searchView?.hasFocus() == true) {
                searchView?.clearFocus()
                return true
            }
        } catch (e: Exception) {
            // ignore
        }
        if (viewModel.hasSelection()) {
            viewModel.onClearSelection()
            return true
        }
        if (viewModel.onNavigateHierarchyBack()) {
            return true
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.appConfig.canUpdateNotificationOnResume()) {
            drawerLayout?.doOnFirstLayout {
                viewModel.clipboardState.refreshClipboard()
            }
        }
    }

    override fun bind(viewModel: MainListViewModel) {
        val ctx = requireContext()

        var newFav: Boolean? = null

        val mainState = viewModel.mainState
        val settings = viewModel.getSettings()
        val contextCloseListener = OnClickListener { viewModel.onClearSelection() }.debounce()

        // LEFT NAVIGATION
        toolbar.setNavigationIcon(R.drawable.action_menu)
        toolbar.setNavigationOnClickListener {
            searchView.hideKeyboard()
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val menuFlags = MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_ALWAYS

        // COUNTER MENU
        val counterMenu = toolbar.menu.add(1, 1, 1, "")
        counterMenu.isVisible = viewModel.appConfig.mainListDisplayCounter()
        counterMenu.setShowAsAction(menuFlags)
        counterMenu.isEnabled = false

        // FILTER ACTION
        val filterMenu = toolbar.menu.add(2, 1, 1, R.string.filter_toolbar_title)
        filterMenu.setIcon(R.drawable.action_filter)
        filterMenu.setShowAsAction(menuFlags)
        filterMenu.isEnabled = false
        filterMenu.setOnMenuItemClickListener {
            searchView.hideKeyboard()
            navigateTo(R.id.action_advanced_filter)
            true
        }

        // SEARCH ACTION

        var searchByText: String? = null
        val recyclerViewRef = recyclerView
        searchView.apply {
            val searchHandler = Handler(Looper.getMainLooper())
            val searchTask = Runnable {
                val newSearchText = searchView?.text.toNullIfEmpty()
                if (searchByText != newSearchText) {
                    searchByText = newSearchText
                    viewModel.onSearch(newSearchText)
                }
            }
            doOnTextChanged { text, _, _, _ ->
                val textStr = text.toNullIfEmpty()
                searchHandler.removeCallbacks(searchTask)
                if (consumeFromUser()) {
                    searchHandler.postDelayed(searchTask, viewModel.appConfig.getUiTimeout())
                } else {
                    searchByText = textStr
                }
            }
            setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                    clearFocus()
                    true
                } else {
                    false
                }
            }

            setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    v.hideKeyboard()
                } else {
                    val key = searchView?.text?.toNullIfEmpty().notNull()
                    mainAdapter.saveScrollState(key, recyclerViewRef)
                    v.showKeyboard { setSelection(key.length) }
                }
            }
        }

        // LEFT NAVIGATION
        navigationView.layoutParams?.width = getDrawerWidth()

        // MAIN LIST
        val touchHelper = MainListTouchHelperBuilder(viewModel, mainAdapter).build()
        touchHelper.attachToRecyclerView(recyclerView)

        // LISTENERS
        viewModel.clipboardState.clip.getLiveData().observe(viewLifecycleOwner) {
            mainAdapter.updateActive(it)
        }
        viewModel.getForceLayoutLive().observe(viewLifecycleOwner) {
            mainAdapter.requestLayout()
        }
        viewModel.getHeaderBlocksLive().observe(viewLifecycleOwner) {
            mainAdapter.submitHeaderList(it)
        }
        viewModel.getEmptyBlocksLive().observe(viewLifecycleOwner) {
            mainAdapter.submitEmptyList(recyclerViewRef, it)
        }
        viewModel.getFilesLive().observe(viewLifecycleOwner) { data ->
            hasPendingUpdates = isHidden
            if (hasPendingUpdates) return@observe
            mainAdapter.submitList(recyclerViewRef, data) {}
        }
        viewModel.getClipsLive().observe(viewLifecycleOwner) { data ->
            val newList = data.blocks
            hasPendingUpdates = isHidden
            if (hasPendingUpdates) return@observe
            mainAdapter.submitList(recyclerViewRef, data) { currentList ->
                val last = viewModel.getLastFilter()
                val scrollRestored = mainAdapter.restoreScrollState(last.textLike.notNull(), recyclerView)
                when {
                    !scrollRestored && data.scrollToTop -> {
                        log("MainList :: scroll to top force :: {}", true)
                        recyclerViewRef.scrollToPosition(0)
                    }
                    !scrollRestored && newList != null -> {
                        val newBlock = newList.maxByOrNull { it?.clip?.changeTimestamp ?: 0 }
                        val prevBlock = newBlock?.let { currentList?.find { it?.clip?.getId() == newBlock.clip.getId() } }
                        if (prevBlock == null) {
                            val index = newList.indexOf(newBlock)
                            log("MainList :: scroll to top prev changed :: {}", index)
                            recyclerViewRef.scrollToPosition(index)
                        }
                    }
                }
            }
        }

        // BOTTOM NAVIGATION
        selectionAllAction.setOnClickListener {
            viewModel.onSelectAll { mainAdapter.requestLayout() }
        }
        val bottomBarUpdater = ToolbarState<Unit>()
            .withContext(bottomBar.context)
            .withMenuItem(
                MenuState.StatefulMenuItem<Unit>()
                    .withShowAsActionAlways()
                    .withTitle(R.string.menu_settings)
                    .withIcon(R.drawable.ic_more_vert)
                    .withOrderInGroup(3)
                    .withGroupId(2)
                    .withStateAcceptor {
                        mainState.getScreen().hasMoreSettings()
                    }
                    .withListener { _, _ ->
                        viewModel.onOpenLastFilter()
                    }
            )
            // STATE_MAIN_CONTEXT
            .withMenuItem(
                MenuState.StatefulMenuItem<Unit>()
                    .withShowAsActionAlways()
                    .withOrderInGroup(1)
                    .withGroupId(11)
                    .withTitle(R.string.main_actions_sync_note)
                    .withIcon(R.drawable.ic_clip_not_synced_action)
                    .withStateAcceptor {
                        mainState.getScreen().isContextScreen()
                                && mainState.hasOnlyNotSyncedNotes()
                    }
                    .withListener { _, _ -> viewModel.onSyncSelected() }
            )
            .withMenuItem(
                MenuState.StatefulMenuItem<Unit>()
                    .withShowAsActionAlways()
                    .withTitle(R.string.menu_fav)
                    .withOrderInGroup(2)
                    .withGroupId(11)
                    .withIcon {
                        if (newFav == true) {
                            R.drawable.ic_fav_true
                        } else {
                            R.drawable.ic_fav_false
                        }
                    }
                    .withStateAcceptor {
                        mainState.getScreen() == ScreenState.STATE_MAIN_CONTEXT
                                && mainState.hasContextActions()
                    }
                    .withListener { _, _ -> newFav?.let { viewModel.onFavSelection(!it) } }
            )
            .withMenuItem(
                MenuState.StatefulMenuItem<Unit>()
                    .withShowAsActionAlways()
                    .withTitle(R.string.menu_delete)
                    .withOrderInGroup(3)
                    .withGroupId(11)
                    .withIcon(R.drawable.ic_delete)
                    .withStateAcceptor {
                        mainState.getScreen() == ScreenState.STATE_MAIN_CONTEXT
                                && mainState.hasContextActions()
                                && !mainState.hasNotClipsInSelection()
                    }
                    .withListenerExt(DoubleClickItemListenerWrapper(
                        ctx,
                        { settings.doubleClickToDelete && viewModel.getSelectedClips().size == 1 },
                        { _, _ -> viewModel.onDelete(viewModel.getSelectedClips().toList()) }
                    ))
            )
            .withMenuItem(
                MenuState.StatefulMenuItem<Unit>()
                    .withShowAsActionAlways()
                    .withTitle(R.string.menu_copy)
                    .withOrderInGroup(4)
                    .withGroupId(11)
                    .withIcon(R.drawable.ic_copy)
                    .withStateAcceptor {
                        mainState.getScreen() != ScreenState.STATE_MAIN_CONTEXT_DELETED
                                && mainState.getScreen().isContextScreen()
                                && mainState.hasContextActions()
                                && !mainState.hasNotClipsInSelection()
                                && !mainState.hasTooLongSize()
                    }
                    .withListener { _, _ ->
                        val clips = mainState.getSelectedClips().toTypedArray()
                        AppContext.get().onCopy(*clips)
                    }
            )
            .withMenuItem(
                MenuState.StatefulMenuItem<Unit>()
                    .withShowAsActionAlways()
                    .withTitle(R.string.menu_delete)
                    .withOrderInGroup(5)
                    .withGroupId(11)
                    .withIcon(R.drawable.ic_delete_forever)
                    .withStateAcceptor {
                        mainState.getScreen() == ScreenState.STATE_MAIN_CONTEXT_DELETED
                                && !mainState.hasNotDeletedInSelection()
                                && !mainState.hasNotClipsInSelection()
                                && mainState.hasContextActions()
                    }
                    .withListener { _, _ ->
                        val clips = viewModel.getSelectedClips()
                        viewModel.onDelete(clips.toList())
                    }
            )
            .withMenuItem(
                MenuState.StatefulMenuItem<Unit>()
                    .withShowAsActionAlways()
                    .withTitle(R.string.menu_restore)
                    .withOrderInGroup(6)
                    .withGroupId(11)
                    .withIcon(R.drawable.ic_restore)
                    .withStateAcceptor {
                        mainState.getScreen() == ScreenState.STATE_MAIN_CONTEXT_DELETED
                                && !mainState.hasNotDeletedInSelection()
                                && !mainState.hasNotClipsInSelection()
                                && mainState.hasContextActions()
                    }
                    .withListener { _, _ ->
                        val clips = viewModel.getSelectedClips()
                        viewModel.onUndoDelete(clips.toList())
                    }
            )
            .withMenuItem(
                MenuState.StatefulMenuItem<Unit>()
                    .withGroupId(10)
                    .withOrderInGroup(1)
                    .withShowAsActionNever()
                    .withTitle(R.string.context_action_move_to_folder)
                    .withIcon(R.drawable.file_type_folder)
                    .withStateAcceptor {
                        mainState.getScreen() == ScreenState.STATE_MAIN_CONTEXT
                                && mainState.hasContextActions()

                    }
                    .withListener { _, _ -> viewModel.onMoveSelectionToFolder() }
            )
            .withMenuItem(
                MenuState.StatefulMenuItem<Unit>()
                    .withGroupId(10)
                    .withOrderInGroup(2)
                    .withShowAsActionNever()
                    .withTitle(R.string.clip_multiple_edit_attributes_tags)
                    .withIcon(R.drawable.ic_tags_any)
                    .withStateAcceptor {
                        mainState.getScreen() == ScreenState.STATE_MAIN_CONTEXT
                                && mainState.getSelectedObjects().size > 1
                                && !mainState.hasNotClipsInSelection()
                                && mainState.hasContextActions()
                    }
                    .withListener { _, _ -> navigateTo(R.id.action_common_tags) }
            )
            .withMenuItem(
                MenuState.StatefulMenuItem<Unit>()
                    .withGroupId(10)
                    .withOrderInGroup(2)
                    .withShowAsActionNever()
                    .withTitle(R.string.clip_info_label_tags_edit)
                    .withIcon(R.drawable.ic_tags_any)
                    .withStateAcceptor {
                        mainState.getScreen() == ScreenState.STATE_MAIN_CONTEXT
                                && mainState.getSelectedObjects().size == 1
                                && !mainState.hasNotClipsInSelection()
                                && mainState.hasContextActions()
                    }
                    .withListener { _, _ -> navigateTo(R.id.action_common_tags) }
            )
            .withMenuItem(
                MenuState.StatefulMenuItem<Unit>()
                    .withGroupId(10)
                    .withOrderInGroup(2)
                    .withShowAsActionNever()
                    .withTitle(R.string.menu_merge)
                    .withIcon(R.drawable.ic_merge)
                    .withStateAcceptor {
                        mainState.getScreen() == ScreenState.STATE_MAIN_CONTEXT
                                && mainState.getSelectedObjects().size > 1
                                && !mainState.hasNotClipsInSelection()
                                && mainState.hasContextActions()
                    }
                    .withListener { _, _ -> viewModel.onMergeSelectedNotes() }
            )
            .withMenuItem(
                MenuState.StatefulMenuItem<Unit>()
                    .withGroupId(10)
                    .withOrderInGroup(3)
                    .withShowAsActionNever()
                    .withTitle(R.string.menu_share)
                    .withIcon(R.drawable.ic_share)
                    .withStateAcceptor {
                        mainState.getScreen() != ScreenState.STATE_MAIN_CONTEXT_DELETED
                                && mainState.getScreen().isContextScreen()
                                && mainState.hasContextActions()
                                && !mainState.hasNotClipsInSelection()
                                && !mainState.hasTooLongSize()
                    }
                    .withListener { _, _ ->
                        val clips = viewModel.getSelectedClips()
                        AppContext.get().onShare(clips)
                    }
            )
            .withMenuItem(
                MenuState.StatefulMenuItem<Unit>()
                    .withGroupId(10)
                    .withOrderInGroup(4)
                    .withShowAsActionNever()
                    .withTitle(R.string.settings_backup_title)
                    .withIcon(R.drawable.ic_backup)
                    .withStateAcceptor {
                        mainState.getScreen() == ScreenState.STATE_MAIN_CONTEXT
                                && !mainState.hasNotClipsInSelection()
                    }
                    .withListener { _, _ ->
                        activity?.let { act ->
                            val clips = viewModel.getSelectedClips()
                            viewModel.backupManager.backupNotes(act, clips)
                            viewModel.onClearSelection()
                        }
                    }
            )

        viewModel.getMainListDataLive().observe(viewLifecycleOwner) { data ->
            if (data == null) return@observe
            if (data.rememberLastAction && data.lastAction != null) {
                actionButton.setDebounceClickListener {
                    activity?.let { act ->
                        viewModel.mainActionUseCases.onAction(data.lastAction, act)
                    }
                }
                actionButton.setOnLongClickListener {
                    navigateTo(R.id.action_main_actions)
                    true
                }
                actionButton.setImageResource(data.lastAction.iconRes)
            } else {
                actionButton.setDebounceClickListener {
                    navigateTo(R.id.action_main_actions)
                }
                actionButton.setOnLongClickListener {
                    viewModel.onNewNote()
                    true
                }
                actionButton.setImageResource(R.drawable.ic_add_black)
            }
        }

        viewModel.screenLive.observe(viewLifecycleOwner) {
            showHideFab(false)
            when (it) {
                ScreenState.STATE_MAIN -> {
                    bottomBar.setNavigationIcon(R.drawable.ic_tune)
                    bottomBar.setNavigationOnClickListener {
                        searchView.hideKeyboard()
                        if (viewModel.getLastFilter().isFolder()) {
                            ConfigAttributedObjectFragment.show(ctx)
                        } else {
                            ClipListConfigFragment.show(ctx)
                        }
                    }
                    bottomBar.setNavigationContentDescription(R.string.main_actions_more)
                    showHideFab(true)
                }
                ScreenState.STATE_MAIN_CONTEXT,
                ScreenState.STATE_MAIN_CONTEXT_DELETED,
                ScreenState.STATE_MAIN_CONTEXT_READONLY -> {
                    bottomBar.setNavigationIcon(R.drawable.ic_close)
                    bottomBar.setNavigationOnClickListener(contextCloseListener)
                    bottomBar.setNavigationContentDescription(R.string.desktop_shortcuts_contextModeCancel)
                    bottomBar.performShow()
                    showHideFab(false)
                }
                else -> Unit
            }
            bottomBarUpdater.apply(Unit, bottomBar)
        }

        val delay = viewModel.appConfig.externalActionDelay()
        viewModel.lastActionLive.observe(viewLifecycleOwner) {
            it?.action?.let { action ->
                when (action) {
                    is AppNewNoteProvider.Action -> {
                        viewModel.noteUseCases.onNewNote(this)
                    }
                    is AppSearchNotesProvider.Action -> {
                        handleExternalAction()
                        viewModel.onClearSelection()
                        searchView?.postDelayed({ searchView?.requestFocus() }, delay)
                    }
                    is AppEditNoteProvider.Action -> {
                        viewModel.noteUseCases.onEditNote(this, action.clip)
                    }
                    is AppViewNoteProvider.Action -> {
                        val text = action.text
                        if (text.isNotBlank()) {
                            AppContext.get().onGetClipByText(
                                text = text,
                                id = action.id,
                                success = { clip ->
                                    viewModel.noteUseCases.onViewNote(clip)
                                },
                                fail = {
                                    viewModel.noteUseCases.onEditNote(this, Clip.from(text))
                                })
                        }
                    }
                    is AppPreviewNoteProvider.Action -> {
                        viewModel.noteUseCases.onEditNote(this, action.clip)
                    }
                    is AppAuthProvider.Action -> {
                        handleExternalAction()
                        viewModel.userState.requestSignIn()
                    }
                    else -> Unit
                }
            }
        }

        viewModel.filtersLive.observe(viewLifecycleOwner) {
            val textLike = it.last.textLike
            if (textLike != searchByText) {
                searchView?.setText(textLike, fromUser = false)
            }
            val hintRes =
                if (it.last.isFolder()) {
                    R.string.folder_search
                } else {
                    R.string.main_search
                }
            searchView?.setHint(hintRes)
            searchView?.setImeActionLabel(ctx.getString(hintRes), EditorInfo.IME_ACTION_SEARCH)
            updateCounterMenu(counterMenu)
            updateFilterMenu(filterMenu)
            updateEmptyState()
        }

        var selectedClipsLiveFirstRequest = true
        viewModel.selectedClipsLive.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                newFav = null
                selectionAllContainer?.setVisibleOrGone(false)
                selectionCounter?.text = null
                viewModel.onNavigate(ScreenState.STATE_MAIN)
                val color = ThemeUtils.getColorPrimary(ctx)
                bottomBar?.backgroundTint = ColorStateList.valueOf(color)
                bottomBar?.performShow()
                if (!selectedClipsLiveFirstRequest) {
                    viewModel.requestLayout()
                }
                selectedClipsLiveFirstRequest = false
            } else {
                val color = ThemeUtils.getColor(ctx, R.attr.colorPrimaryLight)
                bottomBar?.backgroundTint = ColorStateList.valueOf(color)
                selectionCounter?.text =
                    if (mainState.hasContextActions()) {
                        "${it.size}"
                    } else {
                        val maxNotesNumber = viewModel.appConfig.maxNotesForContextActions()
                        "${it.size} / $maxNotesNumber"
                    }
                val state =
                    if (it.find { it.isReadOnly() } != null) {
                        if (it.find { it.isDeleted() } != null) {
                            ScreenState.STATE_MAIN_CONTEXT_DELETED
                        } else {
                            ScreenState.STATE_MAIN_CONTEXT_READONLY
                        }
                    } else {
                        ScreenState.STATE_MAIN_CONTEXT
                    }

                viewModel.onNavigate(state)
                newFav = it.find { !it.fav } == null
                selectionAllContainer?.setVisibleOrGone(true)
                bottomBar?.let { bottomBarUpdater.apply(Unit, it) }
                bottomBar?.performShow()
            }
        }

        viewModel.undoDeleteClipsLive.observe(viewLifecycleOwner) {
            it?.let { deletedClips ->
                val count = deletedClips.size
                val text = resources.getQuantityString(R.plurals.clip_snackbar_text_deleted, count, count)
                Snackbar.make(mainView, text, Snackbar.LENGTH_LONG).let { snackbar ->
                    snackbar.setAction(R.string.button_undo) {
                        viewModel.onUndoDelete(deletedClips.toList())
                    }
                    snackbar.show()
                }
            }
        }

        viewModel.requestUpdateFilterLive.observe(viewLifecycleOwner) { filter ->
            if (filter == null) return@observe
            L.log(this, "main_list: filter updated")
            if (!filter.isNew()) {
                mainAdapter.requestLayout()
            }
            updateFilterMenu(filterMenu)
        }

        viewModel.requestCloseLeftNavigationLive.observe(viewLifecycleOwner) {
            closeNavigation()
        }
    }

    private fun updateCounterMenu(menuItem: MenuItem) {
        if (menuItem.isVisible) {
            menuItem.title = "(${viewModel.getFilters().getFilteredNotesCount()})"
        }
    }

    private fun updateFilterMenu(menuItem: MenuItem) {
        val ctx = requireContext()
        val filters = viewModel.getFilters()
        val filterColor =
            if (filters.hasActiveFilter()) {
                filters.findActive().getTagChipColor(ctx) ?: ctx.getActionIconColorHighlight()
            } else {
                ctx.getTextColorPrimary()
            }
        menuItem.icon?.tint(filterColor)
        menuItem.isEnabled = true
    }

    private fun showHideFab(show: Boolean) {
        if (show) {
            actionButton.show()
        } else {
            actionButton.hide()
        }
    }

    private fun handleExternalAction() {
        getNavController().popBackStack(R.id.fragment_main, false)
        closeNavigation()
    }

    private fun dismissZeroState() {
        activity?.window?.setBackgroundDrawable(null)
    }

    private fun updateEmptyState() {
        dismissZeroState()
    }

    private fun getDrawerWidth(): Int {
        val displayWidth = AndroidUtils.getPreferredDisplaySize(viewModel.app).x
        val maxWidth = Units.DP.toPx(360f).toInt()
        val offset = Units.DP.toPx(56f).toInt()
        return min(maxWidth, displayWidth - offset)
    }

    private fun closeNavigation() {
        drawerLayout?.postDelayed({ drawerLayout?.closeDrawer(GravityCompat.START) }, 100)
        bottomBar?.performShow()
    }

}
