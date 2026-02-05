package clipto.domain

import clipto.dao.objectbox.model.ClipBox
import java.util.*

open class Clip : AttributedObject() {

    // basic
    open var snippetId: String? = null
    open var text: String? = null
    open var changeTimestamp: Long = 0
    open var platform: String? = null
    open var createDate: Date? = null
    open var updateDate: Date? = null
    open var modifyDate: Date? = null
    open var textType: TextType = TextType.TEXT_PLAIN
    open var characters: Int = 0

    // clipboard
    open var tracked: Boolean = false
    open var usageCount: Int = 0

    // files
    open var files: List<ClipFile.Meta> = emptyList()
    open var fileIds: List<String> = emptyList()
    open var filesCount = 0
    open var size: Long = 0

    // public link
    open var publicLink: PublicLink? = null

    // others
    open var isActive: Boolean = false
    open var isChanged: Boolean = false
    open var sourceClips: List<Clip>? = null
    open var snippet: Boolean = false
    open var dynamic: Boolean = false
    open var forceSync: Boolean = false

    var newText: String? = null

    fun isDynamic(): Boolean = dynamic
    fun isClipboard(): Boolean = tracked
    fun hasFiles(): Boolean = fileIds.isNotEmpty()
    fun isInternal(): Boolean = objectType == ObjectType.INTERNAL
    fun isSynced(): Boolean = !firestoreId.isNullOrBlank()
    fun hasPublicLink(): Boolean = !publicLink?.link.isNullOrBlank()
    fun isSnippet(): Boolean = !snippetId.isNullOrBlank() || snippetSetsIds.isNotEmpty()
    fun canApplyAutoTags(): Boolean = objectType != ObjectType.EXTERNAL_SNIPPET
    fun canDefineTextType(): Boolean = objectType != ObjectType.EXTERNAL_SNIPPET && textType == TextType.TEXT_PLAIN

    fun clearTempState() {
        excludedTagIds = emptySet()
        newTagIds = null
        newText = null
    }

    fun areContentTheSame(second: Clip) =
        textType == second.textType &&
                fav == second.fav &&
                text == second.text &&
                color == second.color &&
                title == second.title &&
                folderId == second.folderId &&
                abbreviation == second.abbreviation &&
                description == second.description &&
                snippetSetsIds == second.snippetSetsIds &&
                fileIds == second.fileIds &&
                tagIds == second.tagIds

    open fun apply(snippet: Snippet): Clip {
        abbreviation = snippet.abbreviation
        description = snippet.description
        textType = snippet.textType
        title = snippet.title
        text = snippet.text
        return this
    }

    open fun apply(from: Clip): Clip {
        firestoreId = from.firestoreId
        snippetId = from.snippetId
        fav = from.fav
        text = from.text
        platform = from.platform
        changeTimestamp = from.changeTimestamp
        createDate = from.createDate
        updateDate = from.updateDate
        modifyDate = from.modifyDate
        textType = from.textType

        tracked = from.tracked
        usageCount = from.usageCount

        files = from.files
        fileIds = from.fileIds
        filesCount = from.filesCount
        size = from.size

        publicLink = from.publicLink

        sourceClips = from.sourceClips
        snippet = from.snippet
        dynamic = from.dynamic

        characters = from.characters

        newText = from.newText

        excludedTagIds = from.excludedTagIds
        newTagIds = from.newTagIds

        forceSync = from.forceSync

        super.apply(from)

        return this
    }

    companion object {
        val NULL = Clip()

        fun areTheSame(first: ClipBox, second: ClipBox) =
            first.usageCount == second.usageCount
                    && first.fav == second.fav
                    && first.snippetId == second.snippetId
                    && first.modifyDate == second.modifyDate
                    && first.deleteDate == second.deleteDate
                    && first.textType == second.textType
                    && first.tagIds == second.tagIds
                    && first.title == second.title
                    && first.text == second.text
                    && first.abbreviation == second.abbreviation
                    && first.description == second.description
                    && first.snippetSetsIds == second.snippetSetsIds
                    && first.folderId == second.folderId
                    && first.fileIds == second.fileIds
                    && first.publicLink == second.publicLink
                    && first.color == second.color
    }
}