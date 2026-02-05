package clipto.presentation.blocks.ux

import android.view.View
import androidx.annotation.IdRes
import androidx.core.view.doOnAttach
import androidx.recyclerview.widget.RecyclerView
import clipto.extensions.restoreKeyboard
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class RequestFocusBlock<C>(
    @IdRes val viewId: Int
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_request_focus

    override fun areContentsTheSame(item: BlockItem<C>): Boolean {
        return item is RequestFocusBlock && viewId == item.viewId
    }

    override fun onBind(context: C, block: View) {
        block.doOnAttach {
            val parent = block.parent
            if (parent is RecyclerView) {
                val view = parent.findViewById<View?>(viewId)
                view?.post { view.restoreKeyboard() }
            }
        }
    }

}