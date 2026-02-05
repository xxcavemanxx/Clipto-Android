package clipto.domain

import java.io.Serializable

enum class SortBy(val id: Int, val desc: Boolean = false) : Serializable {

    USAGE_DATE_DESC(0, desc = true),

    CREATE_DATE_DESC(1, desc = true),

    USAGE_COUNT_DESC(2, desc = true),

    TITLE_DESC(3, desc = true),

    TEXT_DESC(4, desc = true),

    USAGE_DATE_ASC(5),

    CREATE_DATE_ASC(6),

    USAGE_COUNT_ASC(7),

    TITLE_ASC(8),

    TEXT_ASC(9),

    TAGS_DESC(10, desc = true),

    TAGS_ASC(11),

    SIZE_ASC(12),

    SIZE_DESC(13, desc = true),

    MODIFY_DATE_ASC(14),

    MODIFY_DATE_DESC(15, desc = true),

    DELETE_DATE_ASC(16),

    DELETE_DATE_DESC(17, desc = true),

    CHARACTERS_ASC(18),

    CHARACTERS_DESC(19, desc = true),

    NAME_ASC(20),

    NAME_DESC(21, desc = true),

    NOTES_COUNT_ASC(22),

    NOTES_COUNT_DESC(23, desc = true),

    COLOR_ASC(24),

    COLOR_DESC(25, desc = true),

    MANUAL_ASC(26),

    MANUAL_DESC(27, desc = true),

    ;

    companion object {

        private val items = arrayOf(
            USAGE_DATE_DESC,
            CREATE_DATE_DESC,
            USAGE_COUNT_DESC,
            TITLE_DESC,
            TEXT_DESC,
            USAGE_DATE_ASC,
            CREATE_DATE_ASC,
            USAGE_COUNT_ASC,
            TITLE_ASC,
            TEXT_ASC,
            TAGS_DESC,
            TAGS_ASC,
            SIZE_ASC,
            SIZE_DESC,
            MODIFY_DATE_ASC,
            MODIFY_DATE_DESC,
            DELETE_DATE_ASC,
            DELETE_DATE_DESC,
            CHARACTERS_ASC,
            CHARACTERS_DESC,
            NAME_ASC,
            NAME_DESC,
            NOTES_COUNT_ASC,
            NOTES_COUNT_DESC,
            COLOR_ASC,
            COLOR_DESC,
            MANUAL_ASC,
            MANUAL_DESC
        )

        fun byId(typeId: Int?): SortBy = items.getOrElse(typeId ?: 0) { USAGE_DATE_DESC }
    }

}