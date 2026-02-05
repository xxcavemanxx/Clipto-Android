package clipto.store.main

import android.app.Application
import clipto.common.misc.AndroidUtils
import clipto.config.IAppConfig
import clipto.dao.objectbox.FilterBoxDao
import clipto.dao.objectbox.SettingsBoxDao
import clipto.dao.objectbox.model.ClipBox
import clipto.dao.objectbox.model.FileRefBox
import clipto.dao.objectbox.model.FilterBox
import clipto.domain.*
import clipto.domain.factory.FilterFactory
import clipto.extensions.normalize
import clipto.store.StoreObject
import clipto.store.StoreState
import clipto.store.user.UserState
import io.objectbox.query.Query
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainState @Inject constructor(
    appConfig: IAppConfig,
    private val app: Application,
    private val userState: UserState,
    private val filterBoxDao: FilterBoxDao,
    private val settingsBoxDao: SettingsBoxDao
) : StoreState(appConfig) {

    val listConfig by lazy {
        StoreObject(
            id = "list_config",
            initialValue = ListConfig.create(settingsBoxDao.get(), app).copy(activeFilter.requireValue()),
            onChanged = { _, value -> value?.let { settingsBoxDao.get().apply(it) } }
        )
    }
    val filterSnapshot by lazy {
        StoreObject(
            id = "filter_snapshot",
            initialValue = Filter.Snapshot().copy(activeFilter.requireValue()),
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }
    val activeFilter by lazy {
        StoreObject(
            id = "active_filter",
            initialValue = getFilters().findActive(),
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }
    val selectedObjects by lazy {
        StoreObject<Set<AttributedObject>>(
            id = "selected_objects",
            initialValue = linkedSetOf()
        )
    }
    val undoDeleteClips by lazy {
        StoreObject<Set<Clip>>(
            id = "undo_delete_clips", liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }
    val screen by lazy {
        StoreObject(
            id = "screen",
            initialValue = ScreenState.STATE_MAIN
        )
    }

    val requestCloseLeftNavigation by lazy {
        StoreObject<Boolean>(
            id = "request_close_left_navigation",
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }
    val requestApplyListConfig by lazy {
        StoreObject<ApplyListConfigRequest>(
            id = "request_apply_list_config",
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }
    val requestApplyFilter by lazy {
        StoreObject<ApplyFilterRequest>(
            id = "request_apply_filter",
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    val requestFontsUpdate by lazy {
        val settings = getSettings()
        if (settings.fontsMeta.isNotEmpty()) {
            Font.values().forEach { it.visible = false }
            settings.fontsMeta.forEach { meta ->
                Font.valueOf(meta.id)?.let {
                    it.visible = meta.visible
                    it.order = meta.order
                }
            }
        }
        StoreObject(id = "request_fonts_update", initialValue = FontsUpdateRequest())
    }

    val clipsQuery = StoreObject<Query<ClipBox>>(id = "clips_query")
    val filesQuery = StoreObject<Query<FileRefBox>>(id = "files_query")

    fun onClear() {
        undoDeleteClips.clearValue()
        selectedObjects.clearValue()
        filesQuery.clearValue()
        clipsQuery.clearValue()
    }

    fun clearSelection() = selectedObjects.clearValue()

    fun getActiveFilter() = activeFilter.requireValue()
    fun getActiveFilterSnapshot() = filterSnapshot.requireValue()
    fun getTextLike() = getFilters().last.textLike
    fun getSelectedObjects() = selectedObjects.requireValue()
    fun getListConfig() = listConfig.requireValue()
    fun getFilters() = filterBoxDao.getFilters()
    fun getSettings() = settingsBoxDao.get()
    fun getScreen() = screen.requireValue()

    fun getVisibleFonts(): List<Font> = requestFontsUpdate.let {
        mutableListOf<Font>().apply {
            add(Font.DEFAULT)
            Font.values().filter { it.isAvailable() && it.visible }.sortedBy { it.order }.forEach { add(it) }
            add(Font.MORE)
            val selected = Font.valueOf(getSettings())
            if (!contains(selected)) {
                add(1, selected)
            }
        }
    }

    fun isNotSynced(clip: Clip?): Boolean = userState.isNotSynced(clip)
    fun isSelected(obj: AttributedObject?) = obj != null && getSelectedObjects().contains(obj)

    fun getSelectedClips(): List<Clip> = getSelectedObjects().mapNotNull { it as? Clip }
    fun getSelectedFiles(): List<FileRef> = getSelectedObjects().filterIsInstance<FileRef>()
    fun getSelectedFolders(): List<FileRef> = getSelectedFiles().filter { it.isFolder }

    fun hasSelectedObjects(): Boolean {
        return getSelectedObjects().isNotEmpty()
    }

    fun hasClipsInSelection(): Boolean {
        return getSelectedObjects().find { it is Clip } != null
    }

    fun hasNotClipsInSelection(): Boolean {
        return getSelectedObjects().find { it !is Clip } != null
    }

    fun hasFilesInSelection(): Boolean {
        return getSelectedObjects().find { it is FileRef && !it.isFolder } != null
    }

    fun hasFoldersInSelection(): Boolean {
        return getSelectedObjects().find { it is FileRef && it.isFolder } != null
    }

    fun hasContextActions(): Boolean {
        val objects = getSelectedObjects()
        return objects.isNotEmpty() && objects.size <= appConfig.maxNotesForContextActions()
    }

    fun hasTooLongSize(): Boolean {
        val objects = getSelectedObjects()
        val clips = objects.mapNotNull { it as? Clip }
        return clips.sumOf { (it.text?.length ?: 0) + 2 } * 4 > 512_000
    }

    fun hasAnyThatRequiresConfirm(objects: Collection<AttributedObject> = getSelectedObjects()): Boolean {
        val clips = objects.mapNotNull { it as? Clip }
        if (clips.size != objects.size) return true
        return clips.find { it.filesCount > 0 || it.fav || it.snippet || it.publicLink != null || it.isDeleted() } != null
    }

    fun hasOnlyNotSyncedNotes(): Boolean {
        if (!userState.isAuthorized()) return false
        return getSelectedObjects().find { it.firestoreId != null } == null && !hasNotClipsInSelection()
    }

    fun hasNotDeletedInSelection(): Boolean {
        return getSelectedObjects().find { !it.isDeleted() } != null
    }

    fun requestFontsUpdate() = requestFontsUpdate.setValue(FontsUpdateRequest())
    fun requestCloseLeftNavigation(closeAll: Boolean) = requestCloseLeftNavigation.setValue(closeAll, force = true)

    fun requestReloadFilter(closeNavigation: Boolean = true) = requestApplyFilter(activeFilter.requireValue(), force = true, closeNavigation = closeNavigation)

    fun requestClearFilter(closeNavigation: Boolean = true) = requestApplyFilter(getFilters().all.normalize(), closeNavigation = closeNavigation)

    fun resetFilter() {
        requestApplyFilter(
            getFilters().all.normalize(),
            closeNavigation = false,
            force = true
        )
    }

    fun requestSearch(text: String?) {
        val snapshot = Filter.Snapshot().copy(activeFilter.requireValue())
        val filter = FilterBox().withSnapshot(snapshot)
        filter.activeFilterId = null
        filter.textLike = text
        requestApplyFilter(filter, closeNavigation = false)
    }

    fun requestApplyFilter(
        template: Filter,
        force: Boolean = false,
        withTextLike: Boolean = false,
        closeNavigation: Boolean = true,
        snapshotInterceptor: (snapshot: Filter.Snapshot) -> Unit = {}
    ): StoreObject<ApplyFilterRequest> {
        requestApplyFilter.setValue(
            ApplyFilterRequest(
                template = template,
                force = force,
                withTextLike = withTextLike,
                closeNavigation = closeNavigation,
                snapshotInterceptor = snapshotInterceptor
            ), force = force
        )
        return requestApplyFilter
    }

    fun requestApplyFilter(
        folder: FileRef,
        force: Boolean = false,
        withTextLike: Boolean = false,
        closeNavigation: Boolean = true,
        snapshotInterceptor: (snapshot: Filter.Snapshot) -> Unit = {}
    ): StoreObject<ApplyFilterRequest> {
        val filter = FilterFactory.createViewFolder(folder, getFilters().folders)
        return requestApplyFilter(
            template = filter,
            force = force,
            withTextLike = withTextLike,
            closeNavigation = closeNavigation,
            snapshotInterceptor = snapshotInterceptor
        )
    }

    fun requestApplyListConfig(
        specific: Filter? = null,
        callback: (state: ListConfig) -> ListConfig
    ) {
        requestApplyListConfig.setValue(
            ApplyListConfigRequest(
                specific = specific,
                callback = callback
            )
        )
    }

    fun requestUpdateSwipeActions(settings: Settings) {
        listConfig.updateValue { it?.copy(settings) }
    }

    data class ApplyFilterRequest(
        val id: Int = AndroidUtils.nextId(),
        val template: Filter,
        val force: Boolean = false,
        val withTextLike: Boolean = false,
        val closeNavigation: Boolean = true,
        val snapshotInterceptor: (snapshot: Filter.Snapshot) -> Unit = {}
    )

    data class ApplyListConfigRequest(
        val specific: Filter? = null,
        val callback: (state: ListConfig) -> ListConfig
    )

    data class FontsUpdateRequest(
        val id: Int = AndroidUtils.nextId()
    )
}