package clipto.presentation.main.list.data

import androidx.paging.PagedList
import clipto.domain.Filter
import clipto.domain.ListConfig
import clipto.presentation.main.list.blocks.FileItemBlock

data class FileItemListData(
    private val _blocks: PagedList<FileItemBlock<Unit>>? = null,
    private val _scrollToTop: Boolean = false,
    private val _snapshot: Filter.Snapshot,
    private val _listConfig: ListConfig,
    private val _stats: ItemListStats? = null
) : ItemListData<FileItemBlock<Unit>>(
    _blocks,
    _scrollToTop,
    _snapshot,
    _listConfig,
    _stats
)