package clipto.common.presentation.state

import android.app.Activity
import android.content.Context
import androidx.appcompat.widget.Toolbar

import clipto.common.presentation.state.MenuState.StatefulMenuItem
import clipto.common.R

class ToolbarState<O> : State<O, Toolbar, ToolbarState<O>>() {

    private val menuConfigurator = MenuState<O>()

    private var titleRes: Int = 0
    private var withBackNavigation = false

    override fun withContext(context: Context?): ToolbarState<O> {
        menuConfigurator.withContext(context)
        return super.withContext(context)
    }

    fun withIgnoredMenuItem(itemId: Int): ToolbarState<O> {
        menuConfigurator.withIgnoredMenuItem(itemId)
        return this
    }

    fun withMenuItem(menuItem: StatefulMenuItem<O>): ToolbarState<O> {
        menuConfigurator.withMenuItem(menuItem)
        return this
    }

    fun withTitleRes(titleRes: Int): ToolbarState<O> {
        this.titleRes = titleRes
        return this
    }

    fun withBackNavigation(): ToolbarState<O> {
        this.withBackNavigation = true
        return this
    }

    override fun doApply(state: O, view: Toolbar) {
        if (withBackNavigation) {
            if (view.navigationIcon == null) {
                view.setNavigationIcon(R.drawable.ic_arrow_back)
            }
            val context = view.context
            if (context is Activity) {
                view.setNavigationOnClickListener { context.finish() }
            }
        }
        if (titleRes != 0) {
            view.setTitle(titleRes)
        }
        menuConfigurator.apply(state, view.menu)
    }

}
