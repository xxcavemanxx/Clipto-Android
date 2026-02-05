package clipto.presentation.main.list.data

import clipto.domain.Filter

data class ItemListStats(
    val filter: Filter,
    var notesCount: Long = -1,
    var filesCount: Long = -1
) {

    fun isInitialized(): Boolean = notesCount >= 0 && filesCount >= 0

    fun hasData(): Boolean = (notesCount + filesCount) > 0

}