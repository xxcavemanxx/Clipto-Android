package clipto.action

import android.app.Application
import androidx.fragment.app.Fragment
import clipto.domain.FileRef
import clipto.domain.factory.FileRefFactory
import clipto.presentation.blocks.SeparateScreenBlock
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.recyclerview.BlockItem
import clipto.repository.IClipRepository
import clipto.repository.IFileRepository
import clipto.store.main.MainState
import com.wb.clipboard.R
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject

@ActivityRetainedScoped
class DeleteFolderAction @Inject constructor(
    private val app: Application,
    private val mainState: MainState,
    private val dialogState: DialogState,
    private val fileRepository: IFileRepository,
    private val clipRepository: IClipRepository
) : CompletableAction<DeleteFolderAction.Context>() {

    override val name: String = "delete_folder"

    fun execute(folder: FileRef, callback: () -> Unit = {}) {
        dialogState.requestBlocksDialog { vm ->
            val blocks = mutableListOf<BlockItem<Fragment>>()
            blocks.add(SpaceBlock.sm())
            blocks.add(
                SeparateScreenBlock(
                    titleRes = R.string.folder_delete_deep,
                    clickListener = {
                        dialogState.showConfirmAction(app.getString(R.string.folder_delete_deep)) {
                            vm.dismiss()
                            execute(Context(folder, deep = true), callback)
                        }
                    }
                )
            )
            blocks.add(SeparatorVerticalBlock())
            blocks.add(
                SeparateScreenBlock(
                    titleRes = R.string.folder_delete_weak,
                    clickListener = {
                        dialogState.showConfirmAction(app.getString(R.string.folder_delete_weak)) {
                            vm.dismiss()
                            execute(Context(folder, deep = false), callback)
                        }
                    }
                )
            )
            vm.postBlocks(blocks)
        }
    }

    override fun create(context: Context): Completable = Single.just(context)
        .flatMap {
            val folderId = context.folder.getUid()!!
            fileRepository.getChildren(folderId, true)
        }
        .flatMap { childrenFiles ->
            val deep = context.deep
            val files = childrenFiles.filter { !it.isFolder }
            val folders = childrenFiles.filter { it.isFolder }.plus(context.folder)
            val folderIds = folders.mapNotNull { it.getUid() }.distinct()
            val clipsFlow = clipRepository.getChildren(folderIds)
                .flatMap { clips ->
                    log("DELETE FOLDER :: clips={}, deep={}", clips.size, deep)
                    clips.forEach { log("DELETE FOLDER :: clip={}", it.text) }
                    if (deep) {
                        clipRepository.deleteAll(clips, permanently = false)
                    } else {
                        clipRepository.changeFolder(clips, null)
                    }
                }

            val filesFlow =
                if (deep) {
                    fileRepository.deleteAll(files, permanently = true).flatMap { clipRepository.unlink(it) }
                } else {
                    fileRepository.changeFolder(files, null)
                }
            log("DELETE FOLDER :: files={}, deep={}", files.size, deep)

            val foldersFlow = fileRepository.deleteAll(folders, permanently = true)
            log("DELETE FOLDER :: folders={}, deep={}", folders.size, deep)

            clipsFlow
                .flatMap { filesFlow }
                .flatMap { foldersFlow }
        }
        .flatMap { fileRepository.getByUid(context.folder.folderId).onErrorReturn { FileRefFactory.root() } }
        .doOnSuccess { newFolderToNavigate -> mainState.requestApplyFilter(newFolderToNavigate, force = true) }
        .doOnError { dialogState.showError(it) }
        .ignoreElement()

    data class Context(val folder: FileRef, val deep: Boolean) : ActionContext(showLoadingIndicator = true)

}