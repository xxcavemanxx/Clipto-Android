package clipto.domain

import com.google.gson.annotations.SerializedName

enum class ClipDetailsTab(val id: Int) {

    @SerializedName("0")
    GENERAL(0),

    @SerializedName("1")
    ATTACHMENTS(1),

    @SerializedName("2")
    DYNAMIC_VALUES(2),

    @SerializedName("3")
    SNIPPETS(3),

    @SerializedName("4")
    TAGS(4),

    @SerializedName("5")
    SNIPPET_KITS(5),

    @SerializedName("6")
    ATTRIBUTES(6),

    @SerializedName("7")
    VALUES(7),

    @SerializedName("8")
    FIELDS(8),

    @SerializedName("9")
    FOLDER(9),
    ;

    companion object {
        fun byId(id: Int?): ClipDetailsTab {
            return when (id) {
                ATTACHMENTS.id -> ATTACHMENTS
                DYNAMIC_VALUES.id -> DYNAMIC_VALUES
                SNIPPETS.id -> SNIPPETS
                SNIPPET_KITS.id -> SNIPPET_KITS
                TAGS.id -> TAGS
                ATTRIBUTES.id -> ATTRIBUTES
                VALUES.id -> VALUES
                FIELDS.id -> FIELDS
                FOLDER.id -> FOLDER
                else -> GENERAL
            }
        }
    }
}