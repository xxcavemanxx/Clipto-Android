package clipto.presentation.main.list.adapters

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import clipto.common.extensions.getSpanCount
import clipto.common.misc.Units
import clipto.domain.ListStyle
import clipto.presentation.common.recyclerview.BlockPagedListAdapter
import clipto.presentation.main.list.data.FileItemListData
import clipto.presentation.main.list.blocks.FileItemBlock

class FileListBlockAdapter : BlockPagedListAdapter<Unit, FileItemBlock<Unit>>(Unit) {

    fun submitList(recyclerView: RecyclerView, data: FileItemListData, commitCallback: Runnable?) {
        val listStyle = data.listConfig.listStyle
        val context = recyclerView.context
        submitList(data.blocks) {
            val layoutManager = recyclerView.layoutManager
            when {
                listStyle == ListStyle.GRID && !data.blocks.isNullOrEmpty() -> {
                    val spanCount = context.getSpanCount()
                    if (layoutManager !is StaggeredGridLayoutManager || layoutManager.spanCount != spanCount) {
                        recyclerView.layoutManager = StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)
                        val padding = Units.DP.toPx(8f).toInt()
                        recyclerView.setPadding(padding, 0, padding, recyclerView.paddingBottom)
                    }
                }
                else -> {
                    if (layoutManager !is LinearLayoutManager) {
                        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                        recyclerView.setPadding(0, 0, 0, recyclerView.paddingBottom)
                    }
                }
            }
            commitCallback?.run()
        }
    }

}