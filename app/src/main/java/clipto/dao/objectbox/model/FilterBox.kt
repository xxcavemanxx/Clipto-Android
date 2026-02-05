package clipto.dao.objectbox.model

import clipto.dao.objectbox.converter.*
import clipto.domain.*
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import java.util.*

@Entity
class FilterBox : Filter() {

    @Id
    var localId: Long = 0

    @Convert(converter = ObjectTypeConverter::class, dbType = Int::class)
    override var objectType: ObjectType = ObjectType.INTERNAL

    @Index
    override var uid: String? = null

    @Index
    override var name: String? = null
    override var createDate: Date? = null
    override var updateDate: Date? = null
    override var syncDate: Date? = null
    override var hideHint: Boolean = false

    @Index
    @Convert(converter = FilterTypeConverter::class, dbType = Int::class)
    override var type: Type = Type.CUSTOM
    override var limit: Int? = null
    override var color: String? = null
    override var notesCount: Long = 0L

    @Convert(converter = SortByConverter::class, dbType = Int::class)
    override var sortBy: SortBy = SortBy.CREATE_DATE_DESC

    @Convert(converter = ListStyleConverter::class, dbType = Int::class)
    override var listStyle: ListStyle = ListStyle.DEFAULT

    override var description: String? = null
    override var autoRuleByTextIn: String? = null
    override var autoRulesEnabled: Boolean = false
    override var pinStarredEnabled: Boolean = false
    override var excludeWithCustomAttributes: Boolean = true

    @Convert(converter = IdsToStringConverter::class, dbType = String::class)
    override var tagIds: List<String> = emptyList()
        get() {
            val uidRef = uid
            if (field.isEmpty() && isTag() && uidRef != null) {
                return listOf(uidRef)
            }
            return field
        }

    @Convert(converter = WhereTypeConverter::class, dbType = Int::class)
    override var tagIdsWhereType: WhereType = WhereType.ANY_OF

    @Convert(converter = IdsToStringConverter::class, dbType = String::class)
    override var snippetSetIds: List<String> = emptyList()

    @Convert(converter = WhereTypeConverter::class, dbType = Int::class)
    override var snippetSetIdsWhereType: WhereType = WhereType.ANY_OF

    override var textLike: String? = null
    override var starred: Boolean = false
    override var untagged: Boolean = false
    override var clipboard: Boolean = false
    override var recycled: Boolean = false
    override var snippets: Boolean = false

    @Convert(converter = WhereTypeConverter::class, dbType = Int::class)
    override var locatedInWhereType: WhereType = WhereType.ANY_OF

    @Convert(converter = TextTypeInConverter::class, dbType = String::class)
    override var textTypeIn: List<TextType> = emptyList()

    override var showOnlyNotSynced: Boolean = false
    override var showOnlyWithPublicLink: Boolean = false
    override var showOnlyWithAttachments: Boolean = false

    @Convert(converter = TimePeriodConverter::class, dbType = Int::class)
    override var createDatePeriod: TimePeriod? = null
    override var createDateFrom: Date? = null
    override var createDateTo: Date? = null

    @Convert(converter = TimePeriodConverter::class, dbType = Int::class)
    override var updateDatePeriod: TimePeriod? = null
    override var updateDateFrom: Date? = null
    override var updateDateTo: Date? = null

    override var activeFilterId: String? = null

    @Convert(converter = SnippetKitConverter::class, dbType = String::class)
    override var snippetKit: SnippetKit? = null

    override var folderId: String? = null

    @Convert(converter = FileTypesConverter::class, dbType = String::class)
    override var fileTypes: List<FileType> = emptyList()

    @Convert(converter = IdsToStringConverter::class, dbType = String::class)
    override var fileIds: List<String> = emptyList()

    @Convert(converter = WhereTypeConverter::class, dbType = Int::class)
    override var fileIdsWhereType: WhereType = WhereType.NONE_OF

    override fun apply(from: Filter): FilterBox {
        super.apply(from)
        if (from is FilterBox) {
            this.localId = from.localId
        }
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as FilterBox

        if (localId != other.localId) return false
        if (uid != other.uid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + localId.hashCode()
        result = 31 * result + (uid?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "FilterBox(localId=$localId, objectType=$objectType, uid=$uid, name=$name, createDate=$createDate, updateDate=$updateDate, syncDate=$syncDate, hideHint=$hideHint, type=$type, limit=$limit, color=$color, notesCount=$notesCount, sortBy=$sortBy, listStyle=$listStyle, description=$description, autoRuleByTextIn=$autoRuleByTextIn, autoRulesEnabled=$autoRulesEnabled, pinStarredEnabled=$pinStarredEnabled, excludeWithCustomAttributes=$excludeWithCustomAttributes, tagIdsWhereType=$tagIdsWhereType, snippetSetIds=$snippetSetIds, snippetSetIdsWhereType=$snippetSetIdsWhereType, textLike=$textLike, starred=$starred, untagged=$untagged, clipboard=$clipboard, recycled=$recycled, snippets=$snippets, locatedInWhereType=$locatedInWhereType, textTypeIn=$textTypeIn, showOnlyNotSynced=$showOnlyNotSynced, showOnlyWithPublicLink=$showOnlyWithPublicLink, showOnlyWithAttachments=$showOnlyWithAttachments, createDatePeriod=$createDatePeriod, createDateFrom=$createDateFrom, createDateTo=$createDateTo, updateDatePeriod=$updateDatePeriod, updateDateFrom=$updateDateFrom, updateDateTo=$updateDateTo, activeFilterId=$activeFilterId, snippetKit=$snippetKit, folderId=$folderId, fileTypes=$fileTypes, fileIds=$fileIds, fileIdsWhereType=$fileIdsWhereType)"
    }

}

fun Filter.toBox(): FilterBox =
    if (this is FilterBox) {
        this
    } else {
        FilterBox().apply(this)
    }