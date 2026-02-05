package clipto.presentation.clip.details.pages.attachments

import android.app.Application
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import clipto.common.extensions.*
import clipto.common.misc.AndroidUtils
import clipto.common.misc.Units
import clipto.common.presentation.mvvm.RxViewModel
import clipto.config.IAppConfig
import clipto.domain.FileRef
import clipto.domain.FileType
import clipto.domain.Filter
import clipto.extensions.getColorPositive
import clipto.extensions.getTextColorAccent
import clipto.presentation.blocks.TextInputLayoutBlock
import clipto.presentation.blocks.domain.MainActionBlock
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.clip.details.ClipDetailsState
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.file.FileScreenHelper
import clipto.presentation.file.blocks.SelectFileBlock
import clipto.presentation.usecases.FileUseCases
import clipto.repository.IFileRepository
import clipto.store.files.FilesState
import clipto.store.main.MainState
import clipto.store.user.UserState
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class AttachmentsPageViewModel @Inject constructor(
    app: Application,
    private val mainState: MainState,
    private val userState: UserState,
    private val appConfig: IAppConfig,
    private val dialogState: DialogState,
    private val filesState: FilesState,
    private val state: ClipDetailsState,
    private val clipDetailsState: ClipDetailsState,
    private val fileRepository: IFileRepository,
    private val fileScreenHelper: FileScreenHelper,
    private val fileUseCases: FileUseCases
) : RxViewModel(app) {

    private val hasCamera by lazy { app.canTakePhoto() }
    private val hasVideo by lazy { app.canRecordVideo() }

    private val fileChangeListeners = mutableListOf<(file: FileRef) -> Unit>()
    val selectedFilesBlocksLive = MutableLiveData<List<BlockItem<AttachmentsPageFragment>>>()
    val searchFilesBlocksLive = MutableLiveData<List<BlockItem<AttachmentsPageFragment>>>()
    val bottomSpaceBlocksLive = MutableLiveData<List<BlockItem<AttachmentsPageFragment>>>()
    val actionBlocksLive = MutableLiveData<List<BlockItem<AttachmentsPageFragment>>>()
    val fileBlocksLive = fileScreenHelper.getFilesLive()

    private var filesCountCallback: (count: Long) -> Unit = {}

    private val searchFilesBlockInited = AtomicBoolean(false)
    private val filter by lazy {
        val fileIds = state.files.requireValue()
        Filter.Snapshot(
            fileIds = fileIds,
            fileIdsWhereType = Filter.WhereType.NONE_OF,
            fileTypes = listOf(FileType.FOLDER),
            fileTypesWhereType = Filter.WhereType.NONE_OF
        )
    }

    private fun getListConfig() = mainState.getListConfig()

    override fun doClear() {
        fileScreenHelper.unbind()
        super.doClear()
    }

    override fun doCreate() {
        initActions()
        initSelectedFiles()
        initFiles()
    }

    private fun initActions() {
        val blocks = mutableListOf<BlockItem<AttachmentsPageFragment>>()

        val fileMaxSize = appConfig.attachmentUploadLimit()

        blocks.add(SpaceBlock(4))

        blocks.add(
            MainActionBlock(
                titleRes = R.string.main_action_files_file_title,
                description = string(R.string.main_action_files_file_description, fileMaxSize),
                iconRes = R.drawable.file_type_file,
                iconColor = app.getColorPositive().takeIf { userState.isAuthorized() },
                onClick = { takeFile(it) }
            )
        )
        if (hasCamera) {
            blocks.add(
                MainActionBlock(
                    titleRes = R.string.main_action_files_photo_title,
                    description = string(R.string.main_action_files_photo_description),
                    iconRes = R.drawable.file_type_photo,
                    iconColor = app.getTextColorAccent().takeIf { userState.isAuthorized() },
                    onClick = { takePhoto(it) }
                )
            )
        }
        if (hasVideo) {
            blocks.add(
                MainActionBlock(
                    titleRes = R.string.main_action_files_record_title,
                    description = string(R.string.main_action_files_record_description, fileMaxSize),
                    iconRes = R.drawable.file_type_record,
                    iconColor = app.getTextColorAccent().takeIf { userState.isAuthorized() },
                    onClick = { recordVideo(it) }
                )
            )
        }
        actionBlocksLive.postValue(blocks)
    }

    private fun initSearchFilters(files: List<FileRef>) {
        if (files.isEmpty()) return
        if (searchFilesBlockInited.compareAndSet(false, true)) {

            if (files.size < appConfig.getClipListSize()) {
                val blocks = mutableListOf<BlockItem<AttachmentsPageFragment>>()
                blocks.add(SpaceBlock(12))
                blocks.add(SeparatorVerticalBlock())
                blocks.add(SpaceBlock(12))
                searchFilesBlocksLive.postValue(blocks)
            } else {
                val blocks = mutableListOf<BlockItem<AttachmentsPageFragment>>()
                blocks.add(SpaceBlock(12))
                blocks.add(
                    TextInputLayoutBlock(
                        changedTextProvider = fileScreenHelper::getSearchByText,
                        text = fileScreenHelper.getSearchByText(),
                        hint = string(R.string.file_search_hint),
                        onTextChanged = { text ->
                            fileScreenHelper.onSearch(
                                filter = filter,
                                textToSearchBy = text?.toString(),
                                pageMapper = this::mapFilesBlock
                            )
                            null
                        },
                        getSuffixChanges = { callback ->
                            val filesCountCallbackRef: (count: Long) -> Unit = {
                                callback.invoke(it.toString())
                            }
                            fileScreenHelper.filesCountState.getValue()?.let(filesCountCallbackRef)
                            filesCountCallback = filesCountCallbackRef
                        }
                    )
                )
                blocks.add(SpaceBlock(heightInDp = 12))
                searchFilesBlocksLive.postValue(blocks)

                val height = AndroidUtils.getDisplaySize(app).y - Units.DP.toPx(248f)
                val heightInDp = Units.PX.toDp(height).toInt()
                bottomSpaceBlocksLive.postValue(listOf(SpaceBlock(heightInDp)))

                fileScreenHelper.filesCountState.getLiveChanges()
                    .filter { it.isNotNull() }
                    .map { it.requireValue() }
                    .observeOn(getViewScheduler())
                    .subscribeBy("filesCountState") { count ->
                        filesCountCallback.invoke(count)
                    }
            }

        }
    }

    private fun initSelectedFiles() {
        val selectedFileIds = state.files.requireValue()
        if (selectedFileIds.isNotEmpty()) {
            fileRepository.getFiles(selectedFileIds)
                .subscribeBy("getFiles") { files ->
                    if (files.isNotEmpty()) {
                        val blocks = mutableListOf<BlockItem<AttachmentsPageFragment>>()
                        blocks.add(SpaceBlock(12))
                        blocks.add(SeparatorVerticalBlock())
                        blocks.add(SpaceBlock(12))
                        blocks.addAll(createBlocks(files))
                        selectedFilesBlocksLive.postValue(blocks)
                    }
                }
        }
    }

    private fun initFiles() {
        fileScreenHelper.onSearch(
            filter = filter,
            pageMapper = this::mapFilesBlock
        )
        filesState.changes.getLiveChanges()
            .filter { fileChangeListeners.isNotEmpty() }
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .observeOn(getViewScheduler())
            .subscribeBy("getLiveChanges") { file ->
                log("getLiveChanges :: {} - {}", file.title, file.progress)
                fileChangeListeners.forEach { it.invoke(file) }
            }
    }

    private fun mapFilesBlock(files: List<FileRef>): List<BlockItem<AttachmentsPageFragment>> {
        initSearchFilters(files)
        return createBlocks(files)
    }

    private fun createBlocks(files: List<FileRef>): List<BlockItem<AttachmentsPageFragment>> {
        log("createBlocks :: files={}", files.size)
        return files.map { file ->
            SelectFileBlock(
                file,
                fileScreenHelper = fileScreenHelper,
                onFileIconClicked = this::onFileIconClicked,
                onFileClicked = this::onFileClicked,
                onFileChanged = this::onFileChanged,
                isSelectedGetter = this::isChecked,
                highlight = fileScreenHelper.getSearchByText(),
                listConfigGetter = this::getListConfig
            )
        }
    }

    private fun isChecked(file: FileRef): Boolean = state.files.requireValue().contains(file.getUid())

    private fun withAuth(callback: () -> Unit = {}) {
        userState.signIn(UserState.SignInRequest.newRequireAuthRequest())
            .observeOn(getViewScheduler())
            .subscribeBy("withAuth") { callback.invoke() }
    }

    private fun onAddFile(uri: Uri, fileType: FileType) {
        fileRepository.upload(uri, fileType)
            .doOnError { dialogState.showError(it) }
            .doOnSuccess { clipDetailsState.attachment.setValue(it, force = true) }
            .subscribeBy("onAddFile")
    }

    private fun takePhoto(fragment: AttachmentsPageFragment) {
        withAuth {
            fragment.activity?.let { act ->
                act.withPhoto {
                    onAddFile(it, FileType.PHOTO)
                }
            }
        }
    }

    private fun recordVideo(fragment: AttachmentsPageFragment) {
        withAuth {
            fragment.activity?.let { act ->
                act.withVideoRecord {
                    onAddFile(it, FileType.RECORD)
                }
            }
        }
    }

    private fun takeFile(fragment: AttachmentsPageFragment) {
        withAuth {
            fragment.activity?.let { act ->
                act.withFile {
                    onAddFile(it, FileType.FILE)
                }
            }
        }
    }

    private fun onFileChanged(callback: (file: FileRef) -> Unit) {
        fileChangeListeners.add(callback)
    }

    private fun onFileClicked(file: FileRef) {
        val fileId = file.getUid() ?: return
        var fileIds = state.files.requireValue()
        fileIds =
            if (fileIds.contains(fileId)) {
                fileIds.minus(fileId)
            } else {
                fileIds.plus(fileId)
            }
        state.files.setValue(fileIds)
    }

    private fun onFileIconClicked(file: FileRef) {
        fileUseCases.onPreview(file, fileScreenHelper.getPreview(file))
    }

}