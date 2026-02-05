package clipto.presentation.common.view

import android.content.Context
import android.view.MenuItem
import clipto.common.extensions.showToast
import clipto.common.logging.L
import clipto.common.presentation.state.MenuState
import com.wb.clipboard.R

class DoubleClickItemListenerWrapper<O>(
        val context: Context,
        private val isActivated: () -> Boolean,
        private val listener: MenuState.IStatefulMenuItemListener<O>
) : MenuState.IStatefulMenuItemListener<O> {

    private var lastClickTime = 0L
    private val threshold = 500L

    override fun onMenuItemClick(state: O, item: MenuItem) {
        if (isActivated.invoke()) {
            val prevClickTime = lastClickTime
            val currentClickTime = System.currentTimeMillis()
            if (currentClickTime - prevClickTime <= threshold) {
                L.log(this, "onDoubleClick: {}", this)
                lastClickTime = 0L
                listener.onMenuItemClick(state, item)
            } else {
                context.showToast(R.string.settings_double_click_toast)
                lastClickTime = currentClickTime
            }
        } else {
            listener.onMenuItemClick(state, item)
        }
    }

}