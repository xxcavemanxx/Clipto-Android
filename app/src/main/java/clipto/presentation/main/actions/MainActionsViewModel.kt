package clipto.presentation.main.actions

import android.app.Application
import android.graphics.Color
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import androidx.recyclerview.widget.ConcatAdapter
import clipto.common.extensions.canRecordVideo
import clipto.common.extensions.canTakePhoto
import clipto.common.extensions.notNull
import clipto.dao.sharedprefs.SharedPrefsDao
import clipto.dao.sharedprefs.SharedPrefsState
import clipto.dao.sharedprefs.data.MainListData
import clipto.domain.*
import clipto.domain.factory.FileRefFactory
import clipto.extensions.getColorHint
import clipto.extensions.getColorPositive
import clipto.extensions.getTextColorAccent
import clipto.extensions.getTextColorSecondary
import clipto.presentation.blocks.SwitchBlock
import clipto.presentation.blocks.TextInputLayoutBlock
import clipto.presentation.blocks.TitleBlock
import clipto.presentation.blocks.bottomsheet.ObjectNameReadOnlyBlock
import clipto.presentation.blocks.domain.MainActionBlock
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.blocks.ux.WarningBlock
import clipto.presentation.blocks.ux.ZeroStateVerticalBlock
import clipto.presentation.clip.ClipScreenHelper
import clipto.presentation.common.fragment.blocks.BlocksViewModel
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.recyclerview.BlockListAdapter
import clipto.presentation.common.recyclerview.BlockPagedListAdapter
import clipto.presentation.file.FileScreenHelper
import clipto.presentation.file.blocks.SelectFileBlock
import clipto.presentation.main.list.blocks.ClipItemFolderBlock
import clipto.presentation.usecases.FileUseCases
import clipto.presentation.usecases.MainActionUseCases
import clipto.repository.IClipRepository
import clipto.repository.IFileRepository
import clipto.store.files.FilesState
import clipto.store.user.UserState
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainActionsViewModel @Inject constructor(
    app: Application,
    private val userState: UserState,
    private val sharedPrefsDao: SharedPrefsDao,
    private val fileRepository: IFileRepository,
    private val clipRepository: IClipRepository,
    private val sharedPrefsState: SharedPrefsState,
    private val mainActionUseCases: MainActionUseCases,
    private val fileScreenHelper: FileScreenHelper,
    private val clipScreenHelper: ClipScreenHelper
) : BlocksViewModel(app) {

    override fun doCreate() {
        val activeFilter = mainState.getActiveFilter()
        if (activeFilter.folderId == null) {
            initActions(FileRefFactory.root())
        } else {
            fileRepository.getByUid(activeFilter.folderId)
                .onErrorReturn { FileRefFactory.root() }
                .observeOn(getViewScheduler())
                .subscribeBy("doCreate") { initActions(it) }
        }
    }

    override fun doClear() {
        super.doClear()
        fileScreenHelper.unbind()
        clipScreenHelper.unbind()
    }

    private fun initActions(folder: FileRef) {
        val blocks = mutableListOf<BlockItem<Fragment>>()
        val fileMaxSize = appConfig.attachmentUploadLimit()
        val noteMaxSize = appConfig.noteMaxSizeInKb()
        val authorized = userState.isAuthorized()
        val hasVideo = app.canRecordVideo()
        val hasCamera = app.canTakePhoto()

        blocks.add(SpaceBlock(heightInDp = 12))

        // FOLDER
        if (!folder.isRootFolder()) {
            blocks.add(TitleBlock(R.string.context_action_move_to_folder, textColor = app.getTextColorSecondary()))

            val color = folder.getIconColor(app)

            blocks.add(
                MainActionBlock(
                    titleRes = MainAction.FOLDER_MOVE_NOTE.titleRes,
                    description = string(MainAction.FOLDER_MOVE_NOTE.descriptionRes),
                    iconRes = MainAction.FOLDER_MOVE_NOTE.iconRes,
                    iconColor = color,
                    onClick = { onMoveNotes(folder) }
                )
            )

            blocks.add(
                MainActionBlock(
                    titleRes = MainAction.FOLDER_MOVE_FILE.titleRes,
                    description = string(MainAction.FOLDER_MOVE_FILE.descriptionRes),
                    iconRes = MainAction.FOLDER_MOVE_FILE.iconRes,
                    iconColor = color,
                    onClick = { onMoveFiles(folder) }
                )
            )

            blocks.add(SeparatorVerticalBlock(marginHoriz = 0, marginVert = 12))
        }

        // NOTES
        blocks.add(TitleBlock(R.string.main_action_notes, textColor = app.getTextColorSecondary()))
        blocks.add(
            MainActionBlock(
                titleRes = MainAction.NOTE_NEW.titleRes,
                description = string(MainAction.NOTE_NEW.descriptionRes),
                iconRes = MainAction.NOTE_NEW.iconRes,
                iconColor = app.getColorPositive(),
                onClick = this::onNewNote
            )
        )
        if (hasCamera) {
            blocks.add(
                MainActionBlock(
                    titleRes = MainAction.NOTE_BARCODE.titleRes,
                    description = string(MainAction.NOTE_BARCODE.descriptionRes),
                    iconRes = MainAction.NOTE_BARCODE.iconRes,
                    iconColor = app.getTextColorAccent(),
                    onClick = this::scanBarcode
                )
            )
        }
        blocks.add(
            MainActionBlock(
                titleRes = MainAction.NOTE_CLIPBOARD.titleRes,
                description = string(MainAction.NOTE_CLIPBOARD.descriptionRes),
                iconRes = MainAction.NOTE_CLIPBOARD.iconRes,
                iconColor = app.getTextColorAccent(),
                onClick = this::onReadClipboard
            )
        )
        blocks.add(
            MainActionBlock(
                titleRes = MainAction.NOTE_FILE.titleRes,
                description = string(MainAction.NOTE_FILE.descriptionRes, noteMaxSize),
                iconRes = MainAction.NOTE_FILE.iconRes,
                iconColor = app.getTextColorAccent(),
                onClick = this::importFromFile
            )
        )

        // SPACING
        blocks.add(SeparatorVerticalBlock(marginHoriz = 0, marginVert = 12))

        // FILES
        blocks.add(TitleBlock(R.string.main_action_files, textColor = app.getTextColorSecondary()))
        blocks.add(
            MainActionBlock(
                titleRes = MainAction.FILE_SELECT.titleRes,
                description = string(MainAction.FILE_SELECT.descriptionRes, fileMaxSize),
                iconRes = MainAction.FILE_SELECT.iconRes,
                iconColor = app.getColorPositive().takeIf { authorized },
                onClick = this::takeFile
            )
        )
        if (hasCamera) {
            blocks.add(
                MainActionBlock(
                    titleRes = MainAction.FILE_PHOTO.titleRes,
                    description = string(MainAction.FILE_PHOTO.descriptionRes),
                    iconRes = MainAction.FILE_PHOTO.iconRes,
                    iconColor = app.getTextColorAccent().takeIf { authorized },
                    onClick = this::takePhoto
                )
            )
        }
        if (hasVideo) {
            blocks.add(
                MainActionBlock(
                    titleRes = MainAction.FILE_VIDEO.titleRes,
                    description = string(MainAction.FILE_VIDEO.descriptionRes, fileMaxSize),
                    iconRes = MainAction.FILE_VIDEO.iconRes,
                    iconColor = app.getTextColorAccent().takeIf { authorized },
                    onClick = this::recordVideo
                )
            )
        }

        // SPACING
        blocks.add(SeparatorVerticalBlock(marginHoriz = 0, marginVert = 12))

        // ORGANIZE
        blocks.add(TitleBlock(R.string.main_action_organize, textColor = app.getTextColorSecondary()))
        blocks.add(
            MainActionBlock(
                titleRes = MainAction.TAG_NEW.titleRes,
                description = string(MainAction.TAG_NEW.descriptionRes),
                iconRes = MainAction.TAG_NEW.iconRes,
                iconColor = app.getColorPositive(),
                onClick = this::onNewTag
            )
        )
        blocks.add(
            MainActionBlock(
                titleRes = MainAction.FOLDER_NEW.titleRes,
                description = string(MainAction.FOLDER_NEW.descriptionRes),
                iconRes = MainAction.FOLDER_NEW.iconRes,
                iconColor = app.getTextColorAccent().takeIf { authorized },
                onClick = this::onNewFolder
            )
        )
        blocks.add(
            MainActionBlock(
                titleRes = MainAction.FILTER_NEW.titleRes,
                description = string(MainAction.FILTER_NEW.descriptionRes),
                iconRes = MainAction.FILTER_NEW.iconRes,
                iconColor = app.getTextColorAccent(),
                onClick = this::onNewFilter
            )
        )

        // SPACING
        blocks.add(SeparatorVerticalBlock(marginHoriz = 0, marginVert = 12))

        // SNIPPETS
        blocks.add(TitleBlock(R.string.main_action_snippets, textColor = app.getTextColorSecondary()))
        blocks.add(
            MainActionBlock(
                titleRes = MainAction.SNIPPET_NEW.titleRes,
                description = string(MainAction.SNIPPET_NEW.descriptionRes),
                iconRes = MainAction.SNIPPET_NEW.iconRes,
                iconColor = app.getColorPositive(),
                onClick = this::onNewSnippet
            )
        )
        blocks.add(
            MainActionBlock(
                titleRes = MainAction.SNIPPET_KIT_NEW.titleRes,
                description = string(MainAction.SNIPPET_KIT_NEW.descriptionRes),
                iconRes = MainAction.SNIPPET_KIT_NEW.iconRes,
                iconColor = app.getTextColorAccent(),
                onClick = this::onNewSnippetSet
            )
        )

        blocks.add(SpaceBlock(heightInDp = 12))

        postBlocks(blocks)
    }

    fun onSettings() {
        dialogState.requestBlocksDialog {
            val data = sharedPrefsState.mainListData.getValue() ?: MainListData()
            val blocks = mutableListOf<BlockItem<Fragment>>()
            blocks.add(SpaceBlock(16))
            blocks.add(SwitchBlock(
                titleRes = R.string.main_action_settings_remember_title,
                checked = data.rememberLastAction,
                clickListener = { _, checked ->
                    val newData = data.copy(rememberLastAction = checked)
                    sharedPrefsDao.saveMainListData(newData)
                        .subscribeBy("saveMainListData")
                }
            ))
            blocks.add(
                WarningBlock(
                    actionIcon = 0,
                    textColor = Color.BLACK,
                    backgroundColor = app.getColorHint(),
                    titleRes = R.string.main_action_settings_remember_description
                )
            )
            blocks.add(SpaceBlock(24))
            it.postBlocks(blocks)
        }
    }

    private fun withDismiss(fragment: Fragment, callback: (activity: FragmentActivity) -> Unit) {
        fragment.activity?.let { act ->
            dismissLive.value = true
            callback.invoke(act)
        }
    }

    private fun onMoveNotes(folder: FileRef) {
        onSelectClips(folder) { clips ->
            clipRepository.changeFolder(clips, folder.getUid())
                .subscribeBy("onMoveNotes", appState) {
                    dismiss()
                }
        }
    }

    private fun onMoveFiles(folder: FileRef) {
        fileScreenHelper.onSelectFiles(folder) { files ->
            fileRepository.changeFolder(files, folder.getUid())
                .subscribeBy("onMoveFiles", appState) {
                    dismiss()
                }
        }
    }

    private fun onNewNote(fragment: Fragment) {
        fragment.activity?.let { act ->
            mainActionUseCases.onAction(MainAction.NOTE_NEW, act)
        }
    }

    private fun onReadClipboard(fragment: Fragment) {
        fragment.activity?.let { act ->
            mainActionUseCases.onAction(MainAction.NOTE_CLIPBOARD, act) { callback ->
                withDismiss(fragment, callback)
            }
        }
    }

    private fun onNewTag(fragment: Fragment) {
        withDismiss(fragment) { act ->
            mainActionUseCases.onAction(MainAction.TAG_NEW, act)
        }
    }

    private fun onNewFolder(fragment: Fragment) {
        withDismiss(fragment) { act ->
            mainActionUseCases.onAction(MainAction.FOLDER_NEW, act)
        }
    }

    private fun onNewSnippet(fragment: Fragment) {
        fragment.activity?.let { act ->
            mainActionUseCases.onAction(MainAction.SNIPPET_NEW, act)
        }
    }

    private fun onNewSnippetSet(fragment: Fragment) {
        withDismiss(fragment) { act ->
            mainActionUseCases.onAction(MainAction.SNIPPET_KIT_NEW, act)
        }
    }

    private fun onNewFilter(fragment: Fragment) {
        withDismiss(fragment) { act ->
            mainActionUseCases.onAction(MainAction.FILTER_NEW, act)
        }
    }

    private fun importFromFile(fragment: Fragment) {
        withDismiss(fragment) { act ->
            mainActionUseCases.onAction(MainAction.NOTE_FILE, act)
        }
    }

    private fun scanBarcode(fragment: Fragment) {
        withDismiss(fragment) { act ->
            mainActionUseCases.onAction(MainAction.NOTE_BARCODE, act)
        }
    }

    private fun takePhoto(fragment: Fragment) {
        fragment.activity?.let { act ->
            mainActionUseCases.onAction(MainAction.FILE_PHOTO, act) { callback ->
                withDismiss(fragment, callback)
            }
        }
    }

    private fun recordVideo(fragment: Fragment) {
        fragment.activity?.let { act ->
            mainActionUseCases.onAction(MainAction.FILE_VIDEO, act) { callback ->
                withDismiss(fragment, callback)
            }
        }
    }

    private fun takeFile(fragment: Fragment) {
        fragment.activity?.let { act ->
            mainActionUseCases.onAction(MainAction.FILE_SELECT, act) { callback ->
                withDismiss(fragment, callback)
            }
        }
    }

    private fun onSelectClips(folder: FileRef, callback: (clips: List<Clip>) -> Unit) {
        clipScreenHelper.onClearSearch(postEmptyState = true)
        clipRepository.getChildren(listOf(folder.getUid().notNull()))
            .onErrorReturn { emptyList() }
            .map { it.mapNotNull { it.firestoreId } }
            .map { ignoreIds ->
                Filter.Snapshot(
                    listStyle = ListStyle.FOLDERS,
                    clipIdsWhereType = Filter.WhereType.NONE_OF,
                    sortBy = appState.getFilterByFolders().sortBy,
                    clipIds = ignoreIds
                )
            }
            .subscribeBy("onSelectClips", appState) { filter ->
                dialogState.requestBlocksDialog(
                    onDestroy = { clipScreenHelper.onClearSearch(postEmptyState = true) },
                    onCreateAdapter = { vm, fragment, adapter ->
                        val clipsAdapter = BlockPagedListAdapter(Unit)

                        // DATA
                        val selectedClips = mutableSetOf<Clip>()
                        val onLongClicked: (clip: Clip) -> Boolean = { false }
                        val isSelectedGetter: (clip: Clip) -> Boolean = { selectedClips.contains(it) }
                        val onClicked: (clip: Clip) -> Boolean = {
                            if (selectedClips.contains(it)) {
                                selectedClips.remove(it)
                            } else {
                                selectedClips.add(it)
                            }
                            true
                        }
                        val onIconClicked: (clip: Clip, previewUrl: String?) -> Boolean = { clip, _ -> onClicked(clip) }

                        val pageMapper: (clips: List<Clip>) -> List<BlockItem<Unit>> = { clips ->
                            clips.map { clip ->
                                ClipItemFolderBlock(
                                    clip = clip,
                                    checkable = true,
                                    synced = !userState.isNotSynced(clip),
                                    isSelectedGetter = isSelectedGetter,
                                    textLike = clipScreenHelper.getSearchByText(),
                                    listConfigGetter = mainState::getListConfig,
                                    onLongClick = onLongClicked,
                                    onClick = onClicked,
                                    onFetchPreview = clipScreenHelper::onFetchPreview,
                                    onClipIconClicked = onIconClicked
                                )
                            }
                        }

                        // HEADER
                        val blocks = mutableListOf<BlockItem<Fragment>>()
                        blocks.add(
                            ObjectNameReadOnlyBlock(
                                name = folder.title,
                                uid = folder.getUid(),
                                color = folder.color,
                                iconRes = R.drawable.file_type_folder,
                                actionIconColor = app.getTextColorAccent(),
                                actionIconRes = R.drawable.main_action_folder_move_file,
                                onActionClick = {
                                    callback.invoke(selectedClips.toList())
                                    vm.dismiss()
                                }
                            )
                        )
                        blocks.add(SpaceBlock(heightInDp = 8))
                        blocks.add(TextInputLayoutBlock(
                            text = clipScreenHelper.getSearchByText(),
                            changedTextProvider = clipScreenHelper::getSearchByText,
                            hint = string(R.string.main_search),
                            onTextChanged = { text ->
                                clipScreenHelper.onSearch(
                                    filter = filter,
                                    pageMapper = pageMapper,
                                    textToSearchBy = text?.toString()
                                )
                                null
                            }
                        ))
                        blocks.add(SpaceBlock(heightInDp = 12))
                        vm.setBlocks(blocks, scrollToTop = true)

                        // FOOTER
                        val footerAdapter = BlockListAdapter(Unit)
                        footerAdapter.submitList(
                            listOf(
                                ZeroStateVerticalBlock(),
                                SpaceBlock.screenSize(app, 0.8f)
                            )
                        )

                        // CLIPS
                        val clipsLive: LiveData<PagedList<BlockItem<Unit>>> = clipScreenHelper.getClipsLive()
                        clipsLive.observe(fragment) {
                            if (it != null) {
                                footerAdapter.submitList(listOf(SpaceBlock.screenSize(app, 0.8f)))
                            }
                            clipsAdapter.submitList(it)
                        }

                        clipScreenHelper.onSearch(filter = filter, pageMapper = pageMapper)

                        ConcatAdapter(adapter, clipsAdapter, footerAdapter)
                    }
                )
            }
    }

}