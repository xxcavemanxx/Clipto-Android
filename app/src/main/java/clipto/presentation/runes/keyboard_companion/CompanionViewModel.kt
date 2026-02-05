package clipto.presentation.runes.keyboard_companion

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import clipto.AppContext
import clipto.action.SaveFilterAction
import clipto.action.SaveSettingsAction
import clipto.common.misc.ThemeUtils
import clipto.common.misc.Units
import clipto.common.presentation.mvvm.RxViewModel
import clipto.config.IAppConfig
import clipto.dao.objectbox.ClipBoxDao
import clipto.dao.objectbox.FilterBoxDao
import clipto.dao.objectbox.SettingsBoxDao
import clipto.domain.Clip
import clipto.domain.Filter
import clipto.domain.ListConfig
import clipto.extensions.getIconColor
import clipto.extensions.getIconRes
import clipto.presentation.runes.keyboard_companion.data.FilterData
import clipto.store.app.AppState
import clipto.store.clipboard.ClipboardState
import clipto.store.main.MainState
import com.wb.clipboard.R
import dagger.hilt.android.scopes.ServiceScoped
import io.objectbox.android.ObjectBoxDataSource
import io.reactivex.Single
import javax.inject.Inject

@ServiceScoped
class CompanionViewModel @Inject constructor(
    app: Application,
    val appState: AppState,
    val mainState: MainState,
    val appConfig: IAppConfig,
    filterBoxDao: FilterBoxDao,
    settingsBoxDao: SettingsBoxDao,
    val companionState: CompanionState,
    val clipboardState: ClipboardState,
    private val clipBoxDao: ClipBoxDao,
    private val saveFilterAction: SaveFilterAction,
    private val saveSettingsAction: SaveSettingsAction
) : RxViewModel(app) {

    private val appContext = AppContext.get()

    val themeLive = appState.theme.getLiveData()

    private val settings = settingsBoxDao.get()
    private val filters = filterBoxDao.getFilters()

    private val panelDefaultWidth = Units.DP.toPx(280f).toInt()
    private val panelDefaultHeight = (Units.displayMetrics.heightPixels / 1.8).toInt()
    val panelMaxHeight = (Units.displayMetrics.heightPixels - Units.DP.toPx(148f)).toInt()
    val panelMaxWidth = (Units.displayMetrics.widthPixels - Units.DP.toPx(24f)).toInt()
    val panelMinHeight = (panelDefaultHeight / 1.6).toInt()
    val panelMinWidth = (panelDefaultWidth / 1.6).toInt()

    fun getEventThreshold() = appConfig.texpanderEventThreshold()
    fun getPanelHeight(): Int = settings.texpanderHeight.takeIf { it in 1..panelMaxHeight } ?: panelDefaultHeight
    fun getPanelWidth(): Int = settings.texpanderWidth.takeIf { it in 1..panelMaxWidth } ?: panelDefaultWidth
    fun getPanelDestroyOnInactivityIntervalInSeconds(): Int = appConfig.texpanderPanelDestroyOnInactivityIntervalInSeconds()
    fun getDoubleClickToShowThreshold(): Int = appConfig.texpanderDoubleClickToShowThreshold()
    fun getLastEventStateCheckDelay(): Long = appConfig.texpanderLastEventStateCheckDelay()
    fun ignoreInternalEvents(): Boolean = appConfig.texpanderIgnoreInternalEvents()
    fun canDoubleClickToShow(): Boolean = appConfig.texpanderDoubleClickToShow()
    fun undoRedoInputMemory(): Int = appConfig.texpanderUndoRedoInputMemory()
    fun getLayoutDelay(): Long = appConfig.texpanderLayoutDelay()
    fun isEnabled(): Boolean = companionState.isEnabled()
    fun canAutoHide(): Boolean = appConfig.texpanderAutoHide()
    fun isUserHidden(): Boolean = companionState.isUserHidden()
    fun getX(): Int = settings.texpanderX
    fun getY(): Int = settings.texpanderY
    fun getTheme() = appState.getTheme()

    fun getFilterColor(): Int {
        return if (hasActiveFilter()) {
            ThemeUtils.getColor(app, R.attr.actionIconColorHighlight)
        } else {
            ThemeUtils.getColor(app, android.R.attr.textColorPrimary)
        }
    }

    fun withDelay(callback: () -> Unit) = companionState.onMain(appConfig.texpanderEventThreshold(), callback)
    fun hasActiveFilter(): Boolean = searchByFilter.hasActiveFilter()
    fun hasFilteredNotes(): Boolean = searchByFilter.hasNotes()
    fun getTextLike(): String? = searchByFilter.textLike
    fun getListConfig(): ListConfig = listConfig

    val filtersLive by lazy {
        appState.refreshFilters()
        Transformations.map(appState.filters.getLiveData()) { filters ->
            filters.getAll()
                .filter { it.type.orderBy > 0 }
                .sortedBy { it.type.orderBy }
                .map { filter ->
                    FilterData(
                        isActive = isActive(filter),
                        iconColor = filter.getIconColor(app),
                        iconRes = filter.getIconRes(),
                        count = filter.notesCount,
                        filter = filter
                    )
                }
        }
    }
    val clipsLive by lazy { MediatorLiveData<PagedList<Clip>>().also { onSearch(changed = false) } }

    private var searchByChanged = false
    private val searchDelay = appConfig.getUiTimeout()
    private var lastClips: LiveData<PagedList<Clip>>? = null
    private val searchByFilter: Filter = filters.texpander
    private var listConfig = ListConfig.create(settingsBoxDao.get(), app).copy(textSize = 14)
    private var filterSnapshot: Filter.Snapshot = Filter.Snapshot().copy(searchByFilter)
    private val searchHandler = Handler(Looper.getMainLooper())
    private val searchTask = Runnable {
        log("search by :: {} - {}", searchByFilter.textLike, searchByFilter.tagIds)
        val request = Single
            .fromCallable {
                val filter = filters.findActive(searchByFilter)
                val snapshot = Filter.Snapshot().copy(filter)
                snapshot.textLike = searchByFilter.textLike
                var isChanged = snapshot.sortBy != filterSnapshot.sortBy
                        || snapshot.textLike != filterSnapshot.textLike
                        || snapshot.pinStarred != filterSnapshot.pinStarred
                        || snapshot.pinSnippets != filterSnapshot.pinSnippets
                val query = clipBoxDao.getFiltered(snapshot)
                val dataSource = ObjectBoxDataSource.Factory(query)
                    .mapByPage {
                        var activeClip = clipboardState.clip.getValue()
                        var canBeActive = activeClip != null
                        it.forEach { clip ->
                            if (canBeActive) {
                                clip.isActive = canBeActive && (clip == activeClip || clip.text == activeClip?.text)
                                canBeActive = !clip.isActive
                                if (clip.isActive) {
                                    clipboardState.clip.setValue(clip)
                                    activeClip = clip
                                }
                            }
                            clip.isChanged = isChanged
                        }
                        isChanged = false
                        it as List<Clip>
                    }
                searchByFilter.notesCount = query.count()
                listConfig = listConfig.copy(filter)
                filterSnapshot = snapshot
                query to dataSource
            }
            .doOnSuccess { pair ->
                onMain {
                    lastClips?.let {
                        it.value?.dataSource?.invalidate()
                        clipsLive.removeSource(it)
                    }
                    val pageSize = appConfig.getClipListSize()
                    lastClips = LivePagedListBuilder(pair.second, pageSize).build().also { live ->
                        clipsLive.addSource(live) {
                            log("clips reloaded: {}", it.size)
                            clipsLive.postValue(it)
                        }
                    }
                }
            }
        request.subscribeBy("searchTask")
    }

    fun withText(clip: Clip, callback: (text: CharSequence) -> Unit) = appContext.onGetClipText(clip, callback)

    fun onSaveState(newX: Int, newY: Int, newWidth: Int?, newHeight: Int?, userHidden: Boolean?) {
        var changed = false
        settings.apply {
            if (texpanderX != newX) {
                texpanderX = newX
                changed = true
            }
            if (texpanderY != newY) {
                texpanderY = newY
                changed = true
            }
            if (newWidth != null && texpanderWidth != newWidth) {
                texpanderWidth = newWidth
                changed = true
            }
            if (newHeight != null && texpanderHeight != newHeight) {
                texpanderHeight = newHeight
                changed = true
            }
            if (userHidden != null && texpanderUserHidden != userHidden) {
                texpanderUserHidden = userHidden
                changed = true
            }
        }
        if (changed) {
            saveSettingsAction.execute()
        }
        if (searchByChanged) {
            searchByChanged = false
            log("save filter: {} -> {}", searchByFilter.uid, searchByFilter.tagIds)
            saveFilterAction.execute(searchByFilter)
        }
    }

    fun onShowNotification() {
        companionState.requestShowNotification()
    }

    fun onHideNotification() {
        companionState.requestHideNotification()
    }

    fun onCopy(clip: Clip, callback: () -> Unit = {}) {
        if (clip.isActive) {
            clipboardState.clearClipboard()
            clip.isActive = false
            clip.isChanged
        } else {
            appContext.onCopy(clip, clearSelection = false) {
                onMain { callback.invoke() }
            }
        }
    }

    fun onClearSearch() {
        log("onClearSearch")
        searchByFilter.textLike = null
        searchByFilter.starred = false
        searchByFilter.untagged = false
        searchByFilter.clipboard = false
        searchByFilter.snippets = false
        searchByFilter.recycled = false
        searchByFilter.tagIds = emptyList()
        filtersLive.value?.forEach { it.isActive = false }
        searchByChanged = true
        searchHandler.removeCallbacks(searchTask)
        searchHandler.postDelayed(searchTask, searchDelay)
    }

    fun onSearch(text: String? = searchByFilter.textLike, changed: Boolean = true) {
        searchByFilter.textLike = text
        filtersLive.value?.let { filters ->
            searchByFilter.starred = filters.find { it.filter.isStarred() }?.isActive == true
            searchByFilter.untagged = filters.find { it.filter.isUntagged() }?.isActive == true
            searchByFilter.clipboard = filters.find { it.filter.isClipboard() }?.isActive == true
            searchByFilter.snippets = filters.find { it.filter.isSnippets() }?.isActive == true
            searchByFilter.recycled = filters.find { it.filter.isDeleted() }?.isActive == true
            searchByFilter.tagIds = filters.filter { it.filter.isTag() && it.isActive }.mapNotNull { it.filter.uid }
        }
        log("onSearch: {}", searchByFilter.tagIds)
        searchByChanged = changed
        searchHandler.removeCallbacks(searchTask)
        searchHandler.postDelayed(searchTask, searchDelay)
    }

    private fun isActive(filter: Filter): Boolean = when {
        filter.isStarred() -> searchByFilter.starred
        filter.isUntagged() -> searchByFilter.untagged
        filter.isClipboard() -> searchByFilter.clipboard
        filter.isSnippets() -> searchByFilter.snippets
        filter.isDeleted() -> searchByFilter.recycled
        else -> filter.isTag() && searchByFilter.tagIds.contains(filter.uid)
    }
}