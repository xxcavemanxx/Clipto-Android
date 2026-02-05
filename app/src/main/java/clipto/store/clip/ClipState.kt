package clipto.store.clip

import clipto.config.IAppConfig
import clipto.domain.*
import clipto.dynamic.FormField
import clipto.extensions.getText
import clipto.store.StoreObject
import clipto.store.StoreState
import clipto.store.app.AppState
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipState @Inject constructor(
    appConfig: IAppConfig,
    private val appState: AppState
) : StoreState(appConfig) {

    val close = StoreObject<Boolean>("close", liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER)

    val screenState by lazy {
        StoreObject<ClipScreenState>(
            id = "screen_state",
            onChanged = { _, next ->
                val clip = next?.value
                formFields.clearValue()
                changedTitle.setValue(clip?.title)
                changedFileIds.setValue(clip?.fileIds)
                changedFolderId.setValue(clip?.folderId)
                changedText.setValue(clip?.getText())
                changedTags.setValue(clip?.getTagIds())
                changedDescription.setValue(clip?.description)
                changedAbbreviation.setValue(clip?.abbreviation)
                changedSnippetKitIds.setValue(clip?.snippetSetsIds)
            },
            liveDataChangeStrategy = StoreObject.LiveDataChangeStrategy.AUTO
        )
    }

    val activeFocus = StoreObject<FocusMode>(id = "active_focus")

    val changedSnippetKitIds = StoreObject<List<String>>(id = "changed_snippet_kit_ids")
    val changedAbbreviation = StoreObject<CharSequence>(id = "changed_abbreviation")
    val changedDescription = StoreObject<CharSequence>(id = "changed_description")
    val changedFileIds = StoreObject<List<String>>(id = "changed_file_ids")
    val changedTitle = StoreObject<CharSequence>(id = "changed_title")
    val changedFolderId = StoreObject<String>(id = "changed_folder_id")
    val changedText = StoreObject<CharSequence>(id = "changed_text")
    val changedTags = StoreObject<List<String>>(id = "changed_tags")
    val formFields = StoreObject<List<FormField>>(id = "form_fields")

    fun getSettings() = appState.getSettings()
    fun close() = close.setValue(true, force = true)
    fun updateState() = screenState.updateValue(force = true) { it }
    fun setViewState(clip: Clip) = setState(clip, ViewMode.VIEW, FocusMode.NONE)
    fun setNewState(clip: Clip = getDefaultNewClip()) = setEditState(clip, appState.getFocusMode())
    fun setEditState(clip: Clip, focusMode: FocusMode, title: CharSequence? = null) = setState(clip, ViewMode.EDIT, focusMode, title)
    fun updateState(clip: Clip) = screenState.updateValue(force = true) { it?.copy(value = clip) ?: ClipScreenState(clip, ViewMode.VIEW, FocusMode.NONE) }

    fun setState(
        clip: Clip,
        viewMode: ViewMode,
        focusMode: FocusMode,
        title: CharSequence? = null
    ): ClipScreenState {
        val newState = ClipScreenState(value = clip, viewMode = viewMode, focusMode = focusMode, title = title)
        screenState.setValue(newState, force = true)
        return newState
    }

    fun getDefaultNewClip(files: List<FileRef>): Clip = getDefaultNewClip()
        .apply {
            fileIds = files.mapNotNull { it.getUid() }
        }

    fun getDefaultNewClip(): Clip = Clip()
        .apply {
            val filter = appState.getFilters().findActive()
            textType = appState.getSettings().textType
            folderId = appState.getActiveFolderId()
            snippetSetsIds = filter.snippetSetIds
            snippet = filter.snippets
            tagIds = filter.tagIds
            fav = filter.starred
            createDate = Date()
        }

}