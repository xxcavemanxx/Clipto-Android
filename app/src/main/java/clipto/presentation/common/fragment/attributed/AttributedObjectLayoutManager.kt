package clipto.presentation.common.fragment.attributed

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.widget.EditTextExt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clipto.domain.AttributedObject
import clipto.domain.AttributedObjectScreenState
import clipto.domain.isPreviewMode
import clipto.domain.isViewMode

class AttributedObjectLayoutManager<O : AttributedObject>(
    context: Context?,
    private val getState: () -> AttributedObjectScreenState<O>?
) : LinearLayoutManager(context, VERTICAL, false) {

    override fun requestChildRectangleOnScreen(
        parent: RecyclerView,
        child: View,
        rect: Rect,
        immediate: Boolean,
        focusedChildVisible: Boolean
    ): Boolean {
        if (child is EditTextExt) {
            if (getState().isViewMode()) {
                return false
            }
            val event = child.getLastTouchEvent()
            if (event != null && System.currentTimeMillis() - event.time <= 100) {
                return false
            }
        }
        if (getState().isPreviewMode()) {
            return false
        }
        return super.requestChildRectangleOnScreen(parent, child, rect, immediate, focusedChildVisible)
    }

}