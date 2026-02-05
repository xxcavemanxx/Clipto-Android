package clipto.domain

import clipto.AppContext
import clipto.cache.AppTextCache
import clipto.common.extensions.notNull
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.dao.objectbox.converter.IdsToStringConverter
import clipto.presentation.common.widget.ColorfulTagSpan
import com.wb.clipboard.R
import java.util.*

abstract class AttributedObject {

    open var firestoreId: String? = null
    open var fav: Boolean = false
    open var color: String? = null
    open var title: String? = null
    open var description: String? = null
    open var abbreviation: String? = null
    open var tagIds: List<String> = emptyList()
    open var snippetSetsIds: List<String> = emptyList()
    open var objectType: ObjectType = ObjectType.INTERNAL
    open var folderId: String? = null

    open var deleteDate: Date? = null

    var excludedTagIds: Set<String> = emptySet()
    var newTagIds: List<String>? = null

    open fun apply(from: AttributedObject): AttributedObject {
        this.firestoreId = from.firestoreId
        this.snippetSetsIds = from.snippetSetsIds
        this.abbreviation = from.abbreviation
        this.description = from.description
        this.objectType = from.objectType
        this.deleteDate = from.deleteDate
        this.folderId = from.folderId
        this.tagIds = from.tagIds
        this.title = from.title
        this.color = from.color
        this.fav = from.fav
        return this
    }

    fun isDeleted() = deleteDate != null

    fun isReadOnly() = objectType == ObjectType.READONLY

    fun hasDescription(): Boolean = !description.isNullOrBlank()

    fun hasAbbreviation(): Boolean = !abbreviation.isNullOrBlank()
}

private val tagIdsConverter = IdsToStringConverter()

fun AttributedObject?.getTagIds(): List<String> = this?.newTagIds ?: this?.tagIds ?: emptyList()

fun AttributedObject?.getKitIds(): List<String> = this?.snippetSetsIds ?: emptyList()

fun AttributedObject?.getKits(): List<Filter> {
    val appContext = AppContext.get()
    val filters = appContext.getFilters()
    return this
        ?.getKitIds()
        ?.takeIf { it.isNotEmpty() }
        ?.mapNotNull { filters.findFilterBySnippetKitId(it) }
        ?.let { Filters.sorted(filters.groupSnippets, it) }
        ?: emptyList()
}

fun AttributedObject?.getTags(noExcluded: Boolean = false): List<Filter> {
    val appContext = AppContext.get()
    val filters = appContext.getFilters()
    return this
        ?.getTagIds()
        ?.takeIf { it.isNotEmpty() }
        ?.let { if (noExcluded) it.minus(excludedTagIds) else it }
        ?.mapNotNull { filters.findFilterByTagId(it) }
        ?.filter { !it.isDeleted() }
        ?.let { Filters.sorted(filters.groupTags, it) }
        ?: emptyList()
}

fun AttributedObject?.getTagsAsStyledLine(): CharSequence? = this
    ?.getTags()
    ?.let { tags ->
        val ids = tags.mapNotNull { tag -> tag.uid }
        val tagIdsString = tagIdsConverter.convertToDatabaseValue(ids) ?: return@let null
        AppTextCache.getOrPut(tagIdsString, AppTextCache.TYPE_TAG) {
            val span = SimpleSpanBuilder()
            tags.forEachIndexed { index, tag ->
                span.append(tag.name.notNull(), ColorfulTagSpan(tag.uid.notNull()))
                if (index < tags.size - 1) {
                    span.append("       ")
                }
            }
            span.build()
        }
    }

fun AttributedObject.getFavIcon(): Int = if (fav) R.drawable.ic_fav_true else R.drawable.ic_fav_false_inverse