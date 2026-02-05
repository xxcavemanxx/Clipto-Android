package clipto.presentation.clip

import android.app.Application
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import clipto.common.extensions.disposeSilently
import clipto.common.extensions.length
import clipto.common.misc.FormatUtils
import clipto.common.presentation.mvvm.RxViewModel
import clipto.config.IAppConfig
import clipto.dao.objectbox.ClipBoxDao
import clipto.domain.Clip
import clipto.domain.FileRef
import clipto.domain.Filter
import clipto.domain.factory.FileRefFactory
import clipto.domain.getFavIcon
import clipto.extensions.*
import clipto.presentation.blocks.AttrHorizontalBlock
import clipto.presentation.blocks.AttrIconBlock
import clipto.presentation.blocks.layout.RowBlock
import clipto.presentation.blocks.ux.SeparatorHorizontalBlock
import clipto.presentation.clip.details.ClipDetailsState
import clipto.presentation.clip.view.blocks.AttachmentsBlock
import clipto.presentation.common.StyleHelper
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.dialog.select.value.SelectValueDialogRequest
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.file.FileScreenHelper
import clipto.presentation.preview.link.LinkPreview
import clipto.repository.IFileRepository
import clipto.repository.IFilterRepository
import clipto.repository.IPreviewRepository
import clipto.store.app.AppState
import clipto.store.clip.ClipScreenState
import clipto.store.files.FilesState
import clipto.store.filter.FilterDetailsState
import com.wb.clipboard.R
import dagger.hilt.android.scopes.ViewModelScoped
import io.objectbox.android.ObjectBoxDataSource
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ViewModelScoped
class ClipScreenHelper @Inject constructor(
    app: Application,
    private val appState: AppState,
    private val appConfig: IAppConfig,
    private val clipBoxDao: ClipBoxDao,
    private val filesState: FilesState,
    private val dialogState: DialogState,
    private val clipDetailsState: ClipDetailsState,
    private val filterDetailsState: FilterDetailsState,
    private val fileRepository: IFileRepository,
    private val filterRepository: IFilterRepository,
    private val fileScreenHelper: FileScreenHelper,
    private val previewRepository: IPreviewRepository
) : RxViewModel(app) {

    private val previewScheduler by lazy { Schedulers.newThread() }
    private val clipsLive: MediatorLiveData<PagedList<Any>> = MediatorLiveData()
    private var lastClipsLive: LiveData<PagedList<Any>>? = null
    private var searchDisposable: Disposable? = null

    @Suppress("UNCHECKED_CAST")
    fun <T> getClipsLive(): LiveData<PagedList<T>> = clipsLive as LiveData<PagedList<T>>

    fun getSearchByText() = clipDetailsState.searchBySnippet.getValue()

    fun unbind() {
        fileScreenHelper.unbind()
        doClear()
    }

    fun onSearch(
        filter: Filter.Snapshot,
        textToSearchBy: String? = getSearchByText(),
        pageMapper: (page: List<Clip>) -> List<Any> = { it }
    ) {
        searchDisposable.disposeSilently()
        searchDisposable = getViewScheduler().scheduleDirect(
            {
                log("onSearch :: text={}, filter={}", textToSearchBy, filter)
                var textChanged = clipDetailsState.searchBySnippet.setValue(textToSearchBy)
                val filterSnapshot = filter.copy(textLike = textToSearchBy)
                val query = clipBoxDao.getFiltered(filterSnapshot)
                val dataSource = ObjectBoxDataSource.Factory(query)
                    .mapByPage {
                        val list = it as List<Clip>
                        if (textChanged) {
                            list.forEach { clip -> clip.isChanged = true }
                            textChanged = false
                        }
                        pageMapper.invoke(list)
                    }
                onClearSearch()
                val pageSize = appConfig.getClipListSize()
                lastClipsLive = LivePagedListBuilder(dataSource, pageSize).build()
                    .also { live ->
                        clipsLive.addSource(live) {
                            clipsLive.postValue(it)
                        }
                    }
            },
            appConfig.getUiTimeout(),
            TimeUnit.MILLISECONDS
        )
    }

    fun onClearSearch(postEmptyState: Boolean = false) {
        if (isMainThread()) {
            log("onClearSearch :: postEmptyState={}", postEmptyState)
            lastClipsLive?.let {
                it.value?.dataSource?.invalidate()
                clipsLive.removeSource(it)
                lastClipsLive = null
            }
            if (postEmptyState) {
                clipsLive.postValue(null)
                searchDisposable.disposeSilently()
            }
        }
    }

    private fun getOptions(clip: Clip, checkedIds: List<String?> = emptyList()): List<SelectValueDialogRequest.Option<Filter>> {
        return appState.getFilters().getSortedTags()
            .map { tag ->
                SelectValueDialogRequest.Option(
                    title = StyleHelper.getFilterLabel(app, tag),
                    checked = clip.tagIds.contains(tag.uid) || checkedIds.contains(tag.uid),
                    iconColor = tag.getIconColor(app),
                    iconRes = tag.getIconRes(),
                    model = tag,
                    uid = tag.name
                )
            }
            .sortedByDescending { it.checked }
    }

    fun onEditTags(
        clip: Clip,
        titleRes: Int = R.string.clip_details_tab_tags,
        clipToChange: () -> Clip? = { clip },
        onChanged: (clip: Clip) -> Unit
    ) {
        val data = SelectValueDialogRequest(
            title = string(titleRes),
            withClearAllAlternativeLogic = true,
            withClearAllCustomTitleRes = R.string.tag_new_action,
            withClearAllCustomListener = {
                filterDetailsState.requestNewTag {
                    it.uid?.let { uid ->
                        clipToChange.invoke()?.let { changed ->
                            changed.tagIds = changed.tagIds.plus(uid).distinct()
                            onChanged(changed)
                        }
                    }
                }
                true
            },
            withManualInput = true,
            onManualInput = { request ->
                val tagName = request.filteredByText
                if (tagName == null) {
                    request.requestRefresh()
                } else {
                    onAddTag(tagName) { filter ->
                        val checkedIds = request.options
                            .filter { opt -> opt.checked }
                            .mapNotNull { opt -> opt.model.uid }
                            .plus(filter.uid)
                        request.options = getOptions(clip, checkedIds)
                        request.requestRefresh()
                    }
                }
            },
            options = getOptions(clip),
            onSelected = { tags ->
                clipToChange.invoke()?.let { changed ->
                    val newTagIds = tags.mapNotNull { it.uid }
                    if (newTagIds != changed.tagIds) {
                        val excludedTagIds = changed.tagIds.minus(newTagIds)
                        changed.excludedTagIds = changed.excludedTagIds.plus(excludedTagIds).minus(newTagIds)
                        changed.tagIds = newTagIds
                        onChanged(changed)
                    }
                }
            }
        )
        dialogState.requestSelectValueDialog(data)
    }

    fun createAttrsBlock(
        clip: Clip,
        withAttachments: Boolean = false,
        clipToChange: () -> Clip? = { clip },
        onChanged: (clip: Clip) -> Unit
    ): BlockItem<Fragment> {
        val attrs = mutableListOf<BlockItem<Fragment>>()

        // starred
        attrs.add(
            AttrIconBlock(
                id = "clip",
                title = string(R.string.filter_label_fav),
                iconRes = clip.getFavIcon(),
                onClicked = {
                    clipToChange.invoke()?.let { changed ->
                        changed.fav = !changed.fav
                        onChanged(changed)
                    }
                }
            )
        )

        // text type
        attrs.add(SeparatorHorizontalBlock())
        attrs.add(
            AttrIconBlock(
                title = string(R.string.text_config_type),
                iconRes = clip.textType.toExt().iconRes,
                onClicked = {
                    val data = SelectValueDialogRequest(
                        title = string(R.string.text_config_type),
                        options = TextTypeExt.types.map { type ->
                            SelectValueDialogRequest.Option(
                                title = string(type.titleRes),
                                checked = type.type == clip.textType,
                                iconRes = type.iconRes,
                                model = type.type
                            )
                        },
                        onSelected = { types ->
                            clipToChange.invoke()?.let { changed ->
                                val newType = types.firstOrNull() ?: changed.textType
                                if (changed.textType != newType) {
                                    changed.textType = newType
                                    onChanged(changed)
                                }
                            }
                        },
                        withImmediateNotify = true,
                        withClearAll = false,
                        single = true
                    )
                    dialogState.requestSelectValueDialog(data)
                }
            )
        )

        // tags
        attrs.add(SeparatorHorizontalBlock())
        attrs.add(
            AttrHorizontalBlock(
                title = string(R.string.clip_details_tab_tags),
                value = clip.tagIds.size.toString(),
                onClicked = {
                    onEditTags(
                        clip = clip,
                        clipToChange = clipToChange,
                        onChanged = onChanged
                    )
                }
            )
        )

        // path
        attrs.add(SeparatorHorizontalBlock())
        attrs.add(
            AttrHorizontalBlock(
                id = "clip",
                title = string(R.string.clip_details_tab_folder),
                value = FormatUtils.DASH,
                onClicked = {
                    fileScreenHelper.onSelectFolder(
                        attributedObject = clip,
                        withNewFolder = true,
                        onSelected = { folderId ->
                            clipToChange.invoke()?.let { changed ->
                                if (folderId != changed.folderId) {
                                    changed.folderId = folderId
                                    onChanged(changed)
                                }
                            }
                        }
                    )
                },
                valueKey = clip.folderId,
                valueProvider = fileScreenHelper::onGetFolderName
            )
        )

        // attachments
        if (withAttachments) {
            attrs.add(SeparatorHorizontalBlock())
            attrs.add(
                AttrHorizontalBlock(
                    title = string(R.string.attachments_title),
                    value = clip.fileIds.size.toString(),
                    onClicked = {
                        fileScreenHelper.onSelectFiles(
                            FileRefFactory.root(),
                            canAddExternal = true,
                            iconRes = R.drawable.ic_attach_file,
                            title = string(R.string.attachments_title),
                            actionIconRes = R.drawable.action_save,
                            excludedIds = clip.fileIds
                        ) { files ->
                            clipToChange.invoke()?.let { changed ->
                                changed.fileIds = files.mapNotNull { it.getUid() }.distinct()
                                onChanged(changed)
                            }
                        }
                    }
                )
            )
        }

        // characters
        attrs.add(SeparatorHorizontalBlock())
        attrs.add(
            AttrHorizontalBlock(
                title = string(R.string.clip_attr_charsCount),
                value = clip.text.length().toString()
            )
        )

        if (!clip.isNew()) {
            attrs.add(SeparatorHorizontalBlock())

            // created
            attrs.add(
                AttrHorizontalBlock(
                    title = string(R.string.clip_attr_created),
                    value = StyleHelper.getCreateDateValue(clip)
                )
            )
            attrs.add(SeparatorHorizontalBlock())

            // edited or deleted
            if (clip.isDeleted()) {
                attrs.add(
                    AttrHorizontalBlock(
                        title = string(R.string.clip_attr_deleted),
                        value = StyleHelper.getDeleteDateValue(clip)
                    )
                )
            } else {
                attrs.add(
                    AttrHorizontalBlock(
                        title = string(R.string.clip_attr_edited),
                        value = StyleHelper.getModifyDateValue(clip)
                    )
                )
            }
            attrs.add(SeparatorHorizontalBlock())

            // updated
            attrs.add(
                AttrHorizontalBlock(
                    title = string(R.string.clip_attr_updated),
                    value = StyleHelper.getUpdateDateValue(clip)
                )
            )
            attrs.add(SeparatorHorizontalBlock())

            // usage count
            attrs.add(
                AttrHorizontalBlock(
                    title = string(R.string.clip_attr_usageCount),
                    value = StyleHelper.getUsageCountValue(clip)
                )
            )
        }


        return RowBlock(attrs, spacingInDp = 0, scrollToPosition = 0)
    }

    fun createAttachmentsBlock(
        screenState: ClipScreenState,
        backgroundColor: Int? = null,
        onEdit: () -> Unit = {},
        onShow: (fileRef: FileRef) -> Unit = {},
        onRemove: (fileRef: FileRef) -> Unit = {}
    ) =
        AttachmentsBlock(
            backgroundColor = backgroundColor,
            screenState = screenState,
            onEdit = onEdit,
            onShowAttachment = onShow,
            onRemoveAttachment = onRemove,
            onGetFiles = { callback -> onGetFiles(screenState.value, callback) },
            onGetFilesChanges = { callback -> onGetFilesChanges({ screenState.value }, callback) }
        )

    fun onFetchPreview(id: String, url: String, callback: (preview: LinkPreview) -> Unit) {
        previewRepository.getPreview(url)
            .timeout(appConfig.getRxTimeout(), TimeUnit.MILLISECONDS)
            .observeOn(getViewScheduler())
            .subscribeBy("onFetchPreview_$id", previewScheduler) { preview ->
                callback.invoke(preview.copy(withSquarePreview = true, cornerRadiusInDp = 16f))
            }
    }

    private fun onGetFilesChanges(clipProvider: () -> Clip?, changesCallback: (fileRef: FileRef) -> Unit) {
        filesState.changes.getLiveChanges()
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .filter {
                val clip = clipProvider.invoke()
                clip != null && clip.fileIds.contains(it.getUid())
            }
            .observeOn(getViewScheduler())
            .subscribeBy("onGetFilesChanges", changesCallback)
    }

    private fun onGetFiles(clip: Clip, changesCallback: (files: List<FileRef>) -> Unit) {
        fileRepository.getFiles(clip.fileIds)
            .observeOn(getViewScheduler())
            .subscribeBy("onGetFiles") { changesCallback.invoke(it) }
    }

    private fun onAddTag(tagName: String, callback: (filter: Filter) -> Unit) {
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
}