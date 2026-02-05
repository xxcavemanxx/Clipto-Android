package clipto.store.files

import clipto.config.IAppConfig
import clipto.domain.FileRef
import clipto.domain.FocusMode
import clipto.domain.ViewMode
import clipto.store.StoreObject
import clipto.store.StoreState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilesState @Inject constructor(
    appConfig: IAppConfig
) : StoreState(appConfig) {

    val changes by lazy {
        StoreObject<FileRef>(
            id = "files_changes"
        )
    }

    val files by lazy {
        StoreObject<List<FileRef>>(
            id = "files"
        )
    }

    val screenState by lazy {
        StoreObject<FileScreenState>(
            id = "screen_state",
            onChanged = { _, next ->
                changes.clearValue()
                val fileRef = next?.value
                changedFav.setValue(fileRef?.fav)
                changedName.setValue(fileRef?.title)
                changedTags.setValue(fileRef?.tagIds)
                changedFolderId.setValue(fileRef?.folderId)
                changedDescription.setValue(fileRef?.description)
                changedAbbreviation.setValue(fileRef?.abbreviation)
                changedSnippetKitIds.setValue(fileRef?.snippetSetsIds)
                selectedFileIndex.setValue(files.getValue()?.indexOf(fileRef))
                log("onView file :: onChanged={}", fileRef)
            },
            liveDataChangeStrategy = StoreObject.LiveDataChangeStrategy.AUTO
        )
    }

    val changedSnippetKitIds = StoreObject<List<String>>(id = "changed_snippet_kit_ids")
    val changedAbbreviation = StoreObject<CharSequence>(id = "changed_abbreviation")
    val changedDescription = StoreObject<CharSequence>(id = "changed_description")
    val changedFolderId = StoreObject<String>(id = "changed_folder_id")
    val changedName = StoreObject<CharSequence>(id = "changed_name")
    val changedTags = StoreObject<List<String>>(id = "changed_tags")
    val changedFav = StoreObject<Boolean>(id = "changed_fav")

    val selectedFileIndex = StoreObject(id = "selected_file_index", initialValue = -1)

    fun setFiles(files: List<FileRef>) = this.files.setValue(files)
    fun updateState() = screenState.updateValue(force = true) { it }
    fun setViewState(fileRef: FileRef, title: CharSequence? = null) = setState(fileRef, ViewMode.VIEW, FocusMode.NONE, title)
    fun updateState(fileRef: FileRef) = screenState.updateValue(force = true) { it?.copy(value = fileRef) ?: FileScreenState(fileRef, ViewMode.VIEW, FocusMode.NONE) }
    fun setState(fileRef: FileRef, viewMode: ViewMode, focusMode: FocusMode, title: CharSequence? = null): FileScreenState {
        val newState = FileScreenState(value = fileRef, viewMode = viewMode, focusMode = focusMode, title = title)
        screenState.setValue(newState, force = true)
        return newState
    }

    fun clearState() {
        screenState.clearValue()
        files.clearValue()
    }

}