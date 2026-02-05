package clipto.presentation.clip.details

import androidx.lifecycle.LifecycleOwner
import clipto.AppContext
import clipto.config.IAppConfig
import clipto.domain.*
import clipto.store.StoreObject
import clipto.store.StoreState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipDetailsState @Inject constructor(appConfig: IAppConfig) : StoreState(appConfig) {

    val fastAction = StoreObject<FastAction>("fast_action", liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER)
    val dismiss = StoreObject<Boolean>("dismiss", liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER)

    val attributesCount = StoreObject<Int>(id = "attributes_count")

    var textType = StoreObject(
        id = "text_type",
        initialValue = TextType.TEXT_PLAIN,
        onChanged = { _, v -> clipDetails.updateValue { it?.copy(type = v!!) } })

    val tags = StoreObject(
        id = "tags",
        initialValue = emptyList<String>(),
        onChanged = { _, v -> clipDetails.updateValue { it?.copy(tagIds = v!!) } }
    )

    val snippetKits = StoreObject(
        id = "snippet_kits",
        initialValue = emptyList<String>(),
        onChanged = { _, v -> clipDetails.updateValue { it?.copy(snippetKitIds = v!!) } }
    )

    val files = StoreObject(
        id = "files",
        initialValue = emptyList<String>(),
        onChanged = { _, v -> clipDetails.updateValue { it?.copy(fileIds = v!!) } }
    )

    val publicLink = StoreObject<PublicLink>(
        id = "public_link",
        onChanged = { _, v -> clipDetails.updateValue { it?.copy(publicLink = v) } }
    )

    val fav = StoreObject(
        id = "fav",
        initialValue = false,
        onChanged = { _, v -> clipDetails.updateValue { it?.copy(fav = v!!) } }
    )

    val folderId = StoreObject<String?>(
        id = "folder_id",
        onChanged = { _, id -> clipDetails.updateValue { it?.copy(folderId = id) } }
    )

    val clipDetails = StoreObject<ClipDetails>(
        id = "clip_details",
        liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER,
        onChanged = { _, v ->
            val newDetails = v ?: return@StoreObject
            val clip = newDetails.clip
            clip.publicLink = newDetails.publicLink
            clip.snippetSetsIds = newDetails.snippetKitIds
            clip.folderId = newDetails.folderId
            clip.fileIds = newDetails.fileIds
            clip.textType = newDetails.type
            clip.tagIds = newDetails.tagIds
            clip.fav = newDetails.fav
            attributesCount.setValue(getAttributesCount())
        }
    )

    val openedClip = StoreObject(
        id = "opened_clip",
        initialValue = Clip.NULL,
        onChanged = { _, clip ->
            clip!!

            snippetKits.setValue(clip.getKits().mapNotNull { it.uid })
            tags.setValue(clip.getTags().mapNotNull { it.uid })
            publicLink.setValue(clip.publicLink)
            textType.setValue(clip.textType)
            folderId.setValue(clip.folderId)
            files.setValue(clip.fileIds)
            fav.setValue(clip.fav)

            clipDetails.setValue(
                ClipDetails(
                    publicLink = clip.publicLink,
                    snippetKitIds = clip.snippetSetsIds,
                    folderId = clip.folderId,
                    fileIds = clip.fileIds,
                    tagIds = clip.tagIds,
                    type = clip.textType,
                    fav = clip.fav,
                    clip = clip
                ),
                notifyChanged = false
            )

            attributesCount.setValue(getAttributesCount())
        }
    )

    val expand = StoreObject<Boolean>(
        id = "expand",
        liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
    )

    val selectedTab = StoreObject(
        id = "selected_tab",
        initialValue = AppContext.get().getSettings().clipDetailsTab
    )

    val searchBySnippet = StoreObject(
        id = "search_by_snippet",
        initialValue = AppContext.get().getSettings().dynamicValueSnippetSearchBy,
        liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
    )

    val dynamicValue = StoreObject<String>(
        id = "dynamic_value",
        liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER,
        onChanged = { _, newValue -> newValue?.let { dismiss.setValue(true, force = true) } }
    )

    val attachment = StoreObject<FileRef>(
        id = "attachment",
        liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER,
        onChanged = { _, newValue -> newValue?.let { dismiss.setValue(true, force = true) } }
    )

    fun unbind(owner: LifecycleOwner) {
        clipDetails.clearAndUnbind(owner)
        dynamicValue.clearAndUnbind(owner)
        attachment.clearAndUnbind(owner)
        fastAction.clearAndUnbind(owner)
        dismiss.clearAndUnbind(owner)
    }

    fun getSettingsIfChanged(): Settings? {
        val settings = AppContext.get().getSettings()
        return if (settings.dynamicValueSnippetSearchBy != searchBySnippet.getValue() || settings.clipDetailsTab != selectedTab.getValue()) {
            settings.dynamicValueSnippetSearchBy = searchBySnippet.getValue()
            settings.clipDetailsTab = selectedTab.requireValue()
            settings
        } else {
            null
        }
    }

    private fun getAttributesCount(): Int {
        var count = 0
        folderId.getValue()?.let {
            count++
        }
        tags.getValue()?.let {
            count += it.size
        }
        snippetKits.getValue()?.let {
            count += it.size
        }
        return count
    }

}