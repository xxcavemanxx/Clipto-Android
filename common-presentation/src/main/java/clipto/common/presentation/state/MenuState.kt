package clipto.common.presentation.state

import android.content.Context
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.MenuCompat
import clipto.common.extensions.tint
import clipto.common.presentation.text.SimpleSpanBuilder
import java.util.*

class MenuState<S> : State<S, Menu, MenuState<S>>() {

    private val statefulItems: MutableList<StatefulMenuItem<S>> = ArrayList()
    private val ignoredItems: MutableSet<Int> = HashSet(1)

    fun withIgnoredMenuItem(itemId: Int): MenuState<S> {
        ignoredItems.add(itemId)
        return this
    }

    fun withMenuItem(menuItem: StatefulMenuItem<S>): MenuState<S> {
        if (menuItem.id == 0) {
            menuItem.id = statefulItems.size
        }
        statefulItems.add(menuItem)
        return this
    }

    override fun doApply(state: S, menu: Menu) {
        MenuCompat.setGroupDividerEnabled(menu, true)
        val ids: MutableSet<Int> = HashSet()
        for (statefulItem in statefulItems) {
            statefulItem.state = state
            if (statefulItem.stateAcceptor == null
                    || statefulItem.stateAcceptor!!.test(state)) {
                ids.add(statefulItem.id)
                val menuItem = menu.findItem(statefulItem.id)
                if (menuItem == null) {
                    statefulItem.bind(state, menu, context)
                } else {
                    menuItem.setOnMenuItemClickListener(statefulItem.clickListener)
                    statefulItem.iconProvider?.provide(state)?.let {
                        menuItem.setIcon(it)
                    }
                    statefulItem.iconColorProvider?.provide(state)?.let {
                        menuItem.icon = menuItem.icon?.tint(it)
                    }
                    statefulItem.titleColorProvider?.provide(state)?.let { color ->
                        statefulItem.title?.let { title ->
                            menuItem.title = SimpleSpanBuilder()
                                    .append(title, ForegroundColorSpan(color))
                                    .build()
                        }
                    }
                    menuItem.isVisible = true
                }
            }
        }
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            if (!ids.contains(menuItem.itemId) && !ignoredItems.contains(menuItem.itemId)) {
                menuItem.setOnMenuItemClickListener(null)
                menuItem.isVisible = false
            }
        }
    }

    class StatefulMenuItem<S> {

        internal var id = 0
        internal var iconRes = 0
        internal var titleRes = 0
        internal var actionEnum = 0
        internal var actionViewRes = 0
        internal var order = Menu.FIRST
        internal var groupId = Menu.NONE
        internal var title: CharSequence? = null
        internal var actionView: View? = null

        internal var state: S? = null
        internal var stateAcceptor: Predicate<S>? = null
        internal var iconProvider: IMenuItemResourceProvider<S>? = null
        internal var titleColorProvider: IMenuItemResourceProvider<S>? = null
        internal var iconColorProvider: IMenuItemResourceProvider<S>? = null
        internal var clickListener: MenuItem.OnMenuItemClickListener? = null

        fun withId(id: Int): StatefulMenuItem<S> {
            this.id = id
            return this
        }

        fun withGroupId(groupId: Int): StatefulMenuItem<S> {
            this.groupId = groupId
            return this
        }

        fun withOrderInGroup(order: Int): StatefulMenuItem<S> {
            this.order = order
            return this
        }

        fun withIcon(@DrawableRes iconRes: Int): StatefulMenuItem<S> {
            this.iconRes = iconRes
            return this
        }

        fun withIcon(iconProvider: IMenuItemResourceProvider<S>): StatefulMenuItem<S> {
            this.iconProvider = iconProvider
            return this
        }

        fun withActionView(actionView: View): StatefulMenuItem<S> {
            this.actionView = actionView
            return this
        }

        fun withActionViewRes(@DrawableRes actionViewRes: Int): StatefulMenuItem<S> {
            this.actionViewRes = actionViewRes
            return this
        }

        fun withIconColor(@ColorInt iconColor: Int): StatefulMenuItem<S> {
            return withIconColor(object : IMenuItemResourceProvider<S> {
                override fun provide(state: S): Int {
                    return iconColor
                }
            })
        }

        fun withIconColor(colorProvider: IMenuItemResourceProvider<S>): StatefulMenuItem<S> {
            iconColorProvider = colorProvider
            return this
        }

        fun withTitle(@StringRes titleRes: Int): StatefulMenuItem<S> {
            this.titleRes = titleRes
            return this
        }

        fun withTitle(title: CharSequence?): StatefulMenuItem<S> {
            this.title = title
            return this
        }

        fun withTitleColor(@ColorInt titleColor: Int): StatefulMenuItem<S> {
            return withTitleColor(object : IMenuItemResourceProvider<S> {
                override fun provide(state: S): Int {
                    return titleColor
                }
            })
        }

        fun withTitleColor(colorProvider: IMenuItemResourceProvider<S>): StatefulMenuItem<S> {
            titleColorProvider = colorProvider
            return this
        }

        fun withShowAsActionNever(): StatefulMenuItem<S> {
            actionEnum = MenuItem.SHOW_AS_ACTION_NEVER
            return this
        }

        fun withShowAsActionAlways(): StatefulMenuItem<S> {
            actionEnum = MenuItem.SHOW_AS_ACTION_ALWAYS
            return this
        }

        fun withShowAsActionIfRoom(): StatefulMenuItem<S> {
            actionEnum = MenuItem.SHOW_AS_ACTION_IF_ROOM
            return this
        }

        fun withShowAsActionWithText(): StatefulMenuItem<S> {
            actionEnum = MenuItem.SHOW_AS_ACTION_WITH_TEXT
            return this
        }

        fun withListener(listener: IStatefulMenuItemListener<S>): StatefulMenuItem<S> {
            clickListener = object : MenuItem.OnMenuItemClickListener {
                private var lastClick = 0L
                override fun onMenuItemClick(item: MenuItem): Boolean {
                    val current = System.currentTimeMillis()
                    if (current - lastClick < 500) {
                    } else {
                        lastClick = current
                        listener.onMenuItemClick(state!!, item)
                    }
                    return true
                }
            }
            return this
        }

        fun withListenerExt(listener: IStatefulMenuItemListener<S>): StatefulMenuItem<S> {
            clickListener = MenuItem.OnMenuItemClickListener { item ->
                listener.onMenuItemClick(state!!, item)
                true
            }
            return this
        }

        fun withStateAcceptor(stateAcceptor: Predicate<S>): StatefulMenuItem<S> {
            this.stateAcceptor = stateAcceptor
            return this
        }

        fun bind(state: S, menu: Menu, context: Context?): MenuItem {
            titleColorProvider?.let {
                val titleColor = it.provide(state)
                (title ?: context?.resources?.getString(titleRes))?.let { t ->
                    title = SimpleSpanBuilder()
                            .append(t, ForegroundColorSpan(titleColor))
                            .build()
                }
            }
            val menuItem: MenuItem = if (title == null) {
                menu.add(groupId, id, order, titleRes)
            } else {
                menu.add(groupId, id, order, title)
            }
            if (actionViewRes != 0) {
                menuItem.setActionView(actionViewRes)
            } else if (actionView != null) {
                menuItem.actionView = actionView
            }
            menuItem.setShowAsAction(actionEnum)
            menuItem.setOnMenuItemClickListener(clickListener)
            iconProvider?.let { iconRes = it.provide(state) }
            if (iconRes != 0 && actionEnum != MenuItem.SHOW_AS_ACTION_NEVER) {
                menuItem.setIcon(iconRes)
                if (iconColorProvider != null) {
                    iconColorProvider?.provide(state)?.let { iconColor ->
                        menuItem.icon = menuItem.icon?.tint(iconColor)
                    }
                }
            }
            return menuItem
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as StatefulMenuItem<*>

            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int {
            return id
        }

    }

    fun interface IStatefulMenuItemListener<S> {
        fun onMenuItemClick(state: S, item: MenuItem)
    }

    fun interface IMenuItemResourceProvider<O> {
        fun provide(state: O): Int
    }
}