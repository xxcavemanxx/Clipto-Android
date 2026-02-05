package clipto.domain

import clipto.common.extensions.ComparatorNullCheck
import clipto.common.extensions.notNull
import clipto.common.extensions.reversed
import clipto.common.extensions.toNullIfEmpty
import clipto.common.misc.IdUtils
import clipto.extensions.createSnippetKit
import clipto.store.user.UserState
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*

data class SnippetKit(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("created") val created: Date,
    @SerializedName("hash") val hash: String? = null,
    @SerializedName("updated") val updated: Date? = null,
    @SerializedName("installs") val installs: Int = 0,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("userName") val userName: String? = null,
    @SerializedName("color") val color: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("filterId") val filterId: String,
    @SerializedName("snippetsCount") val snippetsCount: Int = 0,
    @SerializedName("publicLink") val publicLink: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("publicStatus") val publicStatus: PublicStatus,
    @SerializedName("updateReason") val updateReason: String? = null,
    @SerializedName("categoryId") val categoryId: String? = null,
    @SerializedName("sharable") val sharable: Boolean = false,
    @SerializedName("sortBy") val sortBy: SortBy = SortBy.CREATE_DATE_DESC,
    @SerializedName("listStyle") val listStyle: ListStyle = ListStyle.DEFAULT,
    @Transient var snippets: List<Snippet> = emptyList()
) : Serializable {

    fun isSharable(): Boolean = sharable

    fun isPublic(): Boolean = publicStatus != PublicStatus.PRIVATE

    fun isPublished(): Boolean = publicStatus == PublicStatus.PUBLISHED

    fun isInReview(): Boolean = publicStatus == PublicStatus.IN_REVIEW

    fun isEmpty(): Boolean = snippets.isEmpty()

    fun isMy(userId: String?): Boolean = this.userId == userId

    fun isActionRequired(): Boolean = publicStatus.actionRequired && updateReason != null

    fun getSortedSnippets(): List<Snippet> {
        val comparator =
            when (sortBy) {
                SortBy.CREATE_DATE_ASC -> CREATED_COMPARATOR
                SortBy.CREATE_DATE_DESC -> reversed(CREATED_COMPARATOR)

                SortBy.MODIFY_DATE_ASC -> UPDATED_COMPARATOR
                SortBy.MODIFY_DATE_DESC -> reversed(UPDATED_COMPARATOR, nullsLast = true)

                SortBy.TITLE_ASC -> TITLE_COMPARATOR
                SortBy.TITLE_DESC -> reversed(TITLE_COMPARATOR, nullsLast = true)

                SortBy.TEXT_ASC -> TEXT_COMPARATOR
                SortBy.TEXT_DESC -> reversed(TEXT_COMPARATOR)

                else -> reversed(CREATED_COMPARATOR)
            }.then(reversed(CREATED_COMPARATOR))
        return runCatching { snippets.sortedWith(comparator) }.getOrElse { snippets }
    }

    fun asFilter(): Filter {
        val filter = Filter.createSnippetKit()
        filter.objectType = ObjectType.EXTERNAL_SNIPPET_KIT
        filter.withKit(this)
        return filter
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SnippetKit

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        private val TEXT_COMPARATOR = Comparator<Snippet> { f1, f2 -> f1.text.notNull().compareTo(f2.text.notNull()) }
        private val TITLE_COMPARATOR = Comparator<Snippet> { f1, f2 ->
            val t1 = f1.title.toNullIfEmpty()
            val t2 = f2.title.toNullIfEmpty()
            when {
                t1 == null && t2 == null -> 0
                t1 == null && t2 != null -> ComparatorNullCheck
                t1 != null && t2 == null -> -ComparatorNullCheck
                t1 != null && t2 != null -> t1.compareTo(t2)
                else -> 0
            }
        }
        private val CREATED_COMPARATOR = Comparator<Snippet> { f1, f2 -> f1.created.compareTo(f2.created) }
        private val UPDATED_COMPARATOR = Comparator<Snippet> { f1, f2 ->
            val d1 = f1.updated
            val d2 = f2.updated
            when {
                d1 == null && d2 == null -> 0
                d1 == null && d2 != null -> ComparatorNullCheck
                d1 != null && d2 == null -> -ComparatorNullCheck
                d1 != null && d2 != null -> d1.compareTo(d2)
                else -> 0
            }
        }

        fun from(filter: Filter, userState: UserState): SnippetKit = filter.snippetKit ?: SnippetKit(
            id = IdUtils.autoId(),
            created = Date(),
            name = filter.name!!,
            color = filter.color,
            filterId = filter.uid!!,
            sortBy = filter.sortBy,
            listStyle = filter.listStyle,
            description = filter.description,
            country = Locale.getDefault().country,
            language = Locale.getDefault().language,
            publicStatus = PublicStatus.PRIVATE,
            userName = userState.getUserName(),
            userId = userState.getUserId()
        )

        val NOT_FOUND = SnippetKit(
            id = "not_found",
            created = Date(),
            name = "???",
            userName = "???",
            filterId = "not_found",
            publicStatus = PublicStatus.NOT_FOUND
        )
    }
}

fun SnippetKit?.isSame(second: SnippetKit?): Boolean {
    if (this == null && second == null) return true
    if (this == null || second == null) return false
    return id == second.id
            && name == second.name
            && created == second.created
            && updated == second.updated
            && installs == second.installs
            && userId == second.userId
            && userName == second.userName
            && color == second.color
            && country == second.country
            && language == second.language
            && filterId == second.filterId
            && snippetsCount == second.snippetsCount
            && publicLink == second.publicLink
            && description == second.description
            && publicStatus == second.publicStatus
            && updateReason == second.updateReason
            && categoryId == second.categoryId
            && sharable == second.sharable
            && sortBy == second.sortBy
            && listStyle == second.listStyle
}