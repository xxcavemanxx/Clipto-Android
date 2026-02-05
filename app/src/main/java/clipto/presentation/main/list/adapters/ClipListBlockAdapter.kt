package clipto.presentation.main.list.adapters

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import clipto.common.extensions.getSpanCount
import clipto.common.misc.Units
import clipto.domain.Clip
import clipto.domain.ListStyle
import clipto.extensions.getId
import clipto.presentation.common.recyclerview.BlockPagedListAdapter
import clipto.presentation.main.list.data.ClipItemListData
import clipto.presentation.main.list.blocks.ClipItemBlock

class ClipListBlockAdapter : BlockPagedListAdapter<Unit, ClipItemBlock<Unit>>(Unit) {

    fun submitList(recyclerView: RecyclerView, data: ClipItemListData, commitCallback: Runnable?) {
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

    fun updateActive(clip: Clip?) {
        val currentActiveIndex: Int = currentList?.indexOfFirst { it != null && it.clip.isActive } ?: -1
        val newActiveIndex: Int = clip
            ?.let { currentList }
            ?.indexOfFirst { it != null && (it.clip.getId() == clip.getId() || it.clip.text == clip.text) }
            ?: -1
        val changedIndex = currentList?.indexOfFirst { it != null && it.clip.isChanged } ?: -1
        if (currentActiveIndex != newActiveIndex) {
            if (currentActiveIndex >= 0) {
                currentList?.get(currentActiveIndex)?.clip?.isActive = false
                notifyItemChanged(currentActiveIndex)
            }
            if (newActiveIndex >= 0) {
                currentList?.get(newActiveIndex)?.clip?.isActive = true
                notifyItemChanged(newActiveIndex)
            }
        }
        if (changedIndex >= 0 && changedIndex != currentActiveIndex && changedIndex != newActiveIndex) {
            notifyItemChanged(changedIndex)
        }
    }

}