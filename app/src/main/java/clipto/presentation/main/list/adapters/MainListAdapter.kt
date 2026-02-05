package clipto.presentation.main.list.adapters

import android.os.Parcelable
import androidx.paging.PagedList
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clipto.common.extensions.notNull
import clipto.domain.Clip
import clipto.domain.ListStyle
import clipto.extensions.log
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.recyclerview.BlockListAdapter
import clipto.presentation.main.list.blocks.ClipItemBlock
import clipto.presentation.main.list.blocks.FileItemBlock
import clipto.presentation.main.list.data.ClipItemListData
import clipto.presentation.main.list.data.FileItemListData
import clipto.presentation.main.list.data.ItemListData
import clipto.presentation.main.list.data.ItemListStats

class MainListAdapter {

    private val folderScrollStates = mutableMapOf<String, Parcelable?>()
    private val scrollStates = mutableMapOf<String, Parcelable?>()
    private var lastFolderId: String? = null

    private val headerAdapter = BlockListAdapter(Unit)
    private val clipsAdapter = ClipListBlockAdapter()
    private val filesAdapter = FileListBlockAdapter()
    private val emptyAdapter = BlockListAdapter(Unit)

    private val concatAdapter = ConcatAdapter(headerAdapter, filesAdapter, clipsAdapter, emptyAdapter)


    fun saveScrollState(key: String, recyclerView: RecyclerView?) {
        scrollStates[key] = recyclerView?.layoutManager?.onSaveInstanceState()
    }

    fun restoreScrollState(key: String, recyclerView: RecyclerView?): Boolean {
        val state = scrollStates[key]
        if (state != null) {
            recyclerView?.layoutManager?.onRestoreInstanceState(state)
        }
        scrollStates.clear()
        return state != null
    }

    fun notifyItemChanged(position: Int) {
        concatAdapter.notifyItemChanged(position)
    }

    fun requestLayout() {
        concatAdapter.notifyItemRangeChanged(0, concatAdapter.itemCount, true)
    }

    fun onScreenChanged(recyclerView: RecyclerView) {
        clipsAdapter.onScreenChanged(recyclerView)
    }

    fun updateActive(clip: Clip?) {
        clipsAdapter.updateActive(clip)
    }

    fun submitHeaderList(blocks: List<BlockItem<Unit>>) {
        headerAdapter.submitList(blocks)
    }

    fun submitEmptyList(recyclerView: RecyclerView, blocks: List<BlockItem<Unit>>) {
        emptyAdapter.submitList(blocks) {
            if (blocks.isNotEmpty()) {
                val layoutManager = recyclerView.layoutManager
                if (layoutManager !is LinearLayoutManager) {
                    recyclerView.layoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.VERTICAL, false)
                    recyclerView.setPadding(0, 0, 0, recyclerView.paddingBottom)
                }
            }
        }
    }

    fun submitList(recyclerView: RecyclerView, data: ClipItemListData, commitCallback: (currentList: PagedList<ClipItemBlock<Unit>>?) -> Unit) {
        val isFolders = data.listConfig.listStyle == ListStyle.FOLDERS
        val currentList = clipsAdapter.currentList
        clipsAdapter.submitList(recyclerView, data) {
            if (!isFolders) {
                commitCallback(currentList)
            }
            if (recyclerView.adapter !== concatAdapter) {
                recyclerView.adapter = concatAdapter
            }
        }
    }

    fun submitList(recyclerView: RecyclerView, data: FileItemListData, commitCallback: (currentList: PagedList<FileItemBlock<Unit>>?) -> Unit) {
        val isFolders = data.listConfig.listStyle == ListStyle.FOLDERS
        val canRestore = folderScrollStates.isNotEmpty() && data.stats != null && data.stats.filter.folderId != lastFolderId
        val currentList = filesAdapter.currentList
        storeFolderPosition(recyclerView, data)
        data.stats
            ?.takeIf { canRestore }
            ?.let { restoreFolderPosition(recyclerView, it) }
        filesAdapter.submitList(recyclerView, data) {
            if (!isFolders) {
                commitCallback(currentList)
            }
            if (recyclerView.adapter !== concatAdapter) {
                recyclerView.adapter = concatAdapter
            }
        }
    }

    private fun storeFolderPosition(recyclerView: RecyclerView, data: ItemListData<*>) {
        if (data.listConfig.listStyle == ListStyle.FOLDERS) {
            val folderId = lastFolderId
            if (folderId != null) {
                folderScrollStates.getOrPut(folderId) {
                    recyclerView.layoutManager?.onSaveInstanceState()
                }
            }
            if (data.stats != null) {
                lastFolderId = data.stats.filter.folderId.notNull()
            }
        }
    }

    private fun restoreFolderPosition(recyclerView: RecyclerView, stats: ItemListStats) {
        if (stats.filter.listStyle == ListStyle.FOLDERS) {
            val folderId = stats.filter.folderId.notNull()
            folderScrollStates.remove(folderId)?.let { state ->
                log("restore state :: {}", stats.filter.name)
                recyclerView.layoutManager?.onRestoreInstanceState(state)
            }
        }
    }

}