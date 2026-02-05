package clipto.presentation.main.list.data

import androidx.paging.PagedList
import clipto.domain.Filter
import clipto.domain.ListConfig
import clipto.presentation.common.recyclerview.BlockItem

open class ItemListData<V : BlockItem<*>>(
    val blocks: PagedList<V>? = null,
    val scrollToTop: Boolean = false,
    val snapshot: Filter.Snapshot,
    val listConfig: ListConfig,
    val stats:ItemListStats? = null
)