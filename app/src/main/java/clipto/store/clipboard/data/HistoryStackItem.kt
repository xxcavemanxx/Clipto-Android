package clipto.store.clipboard.data

import clipto.common.extensions.notNull
import clipto.domain.Clip
import clipto.extensions.getId

data class HistoryStackItem(
    val text: String,
    var id: Long = 0
) {
    fun isNew(): Boolean = id == 0L
}

fun Clip.toStackItem(): HistoryStackItem = HistoryStackItem(
    text = text.notNull(),
    id = getId()
)

fun String.toStackItem(): HistoryStackItem = HistoryStackItem(
    text = this
)
