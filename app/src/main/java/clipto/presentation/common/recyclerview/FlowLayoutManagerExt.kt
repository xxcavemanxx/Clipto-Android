package clipto.presentation.common.recyclerview

import androidx.recyclerview.widget.RecyclerView
import com.xiaofeng.flowlayoutmanager.FlowLayoutManager

class FlowLayoutManagerExt : FlowLayoutManager() {

    override fun canScrollVertically(direction: Int): Boolean {
        return true
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        return try {
            super.scrollVerticallyBy(dy, recycler, state)
        } catch (e: Exception) {
            0
        }
    }
}