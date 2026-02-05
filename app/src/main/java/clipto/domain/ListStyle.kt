package clipto.domain

import java.io.Serializable

enum class ListStyle(
    val id: Int,
    val hasTitle: Boolean = false
) : Serializable {

    DEFAULT(0),
    GRID(4, hasTitle = true),
    COMFORTABLE(1),
    COMPACT(2),
    PREVIEW(3, hasTitle = true),
    FOLDERS(5),
    ;

    companion object {
        val NOTE_STYLES = listOf(
            DEFAULT,
            GRID,
            COMFORTABLE,
            COMPACT,
            PREVIEW
        )

        val FOLDER_STYLES = listOf(
            FOLDERS
        )

        fun getStyle(filter: Filter): ListStyle {
            val style = filter.listStyle
            if (filter.isFolder()) {
                return style.takeIf { FOLDER_STYLES.contains(it) } ?: FOLDERS
            }
            return style.takeIf { NOTE_STYLES.contains(it) } ?: DEFAULT
        }

        fun byId(id: Int?): ListStyle = when (id) {
            DEFAULT.id -> DEFAULT
            COMPACT.id -> COMPACT
            COMFORTABLE.id -> COMFORTABLE
            PREVIEW.id -> PREVIEW
            GRID.id -> GRID
            FOLDERS.id -> FOLDERS
            else -> DEFAULT
        }
    }
}