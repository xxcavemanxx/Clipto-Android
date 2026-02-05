package clipto.presentation.main.list

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import clipto.domain.Clip
import clipto.domain.SwipeAction
import clipto.extensions.toColor
import clipto.extensions.toIcon
import clipto.presentation.common.recyclerview.BlockItemViewHolder
import clipto.presentation.main.list.adapters.MainListAdapter
import clipto.presentation.main.list.blocks.ClipItemBlock
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator

class MainListTouchHelperBuilder(
    private val viewModel: MainListViewModel,
    private val mainAdapter: MainListAdapter
) {

    private val mainState = viewModel.mainState
    private val settings = viewModel.getSettings()

    fun build(): ItemTouchHelper = ItemTouchHelper(createCallback())

    private fun createCallback() = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
            return super.getSwipeEscapeVelocity(defaultValue) * 6
        }

        override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            if (viewHolder is BlockItemViewHolder<*, *>) {
                val clip = viewHolder.block?.let { it as? ClipItemBlock<*> }?.clip
                if (clip == null || clip.isReadOnly() || mainState.hasSelectedObjects()) {
                    return 0
                }
                var flags = 0
                if (settings.swipeActionRight != SwipeAction.NONE) {
                    flags = flags or ItemTouchHelper.RIGHT
                }
                if (settings.swipeActionLeft != SwipeAction.NONE) {
                    flags = flags or ItemTouchHelper.LEFT
                }
                return flags
            }
            return 0
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            if (viewHolder is BlockItemViewHolder<*, *>) {
                val clip = viewHolder.block?.let { it as? ClipItemBlock<*> }?.clip
                clip?.let {
                    if (it.isReadOnly()) {
                        if (direction == ItemTouchHelper.RIGHT
                            && settings.swipeActionRight == SwipeAction.COPY
                        ) {
                            doAction(it, settings.swipeActionRight, viewHolder)
                        } else if (direction == ItemTouchHelper.LEFT
                            && settings.swipeActionLeft == SwipeAction.COPY
                        ) {
                            doAction(it, settings.swipeActionLeft, viewHolder)
                        } else {
                            doClearView(viewHolder)
                        }
                    } else {
                        if (direction == ItemTouchHelper.RIGHT) {
                            doAction(it, settings.swipeActionRight, viewHolder)
                        } else {
                            doAction(it, settings.swipeActionLeft, viewHolder)
                        }
                    }
                }
            }
        }

        private fun doAction(clip: Clip, swipeAction: SwipeAction, item: BlockItemViewHolder<*, *>) {
            when (swipeAction) {
                SwipeAction.STAR -> {
                    doClearView(item, true) {
                        viewModel.onFav(listOf(clip), !clip.fav)
                    }
                }
                SwipeAction.COPY -> {
                    if (clip.isActive) {
                        doClearView(item, true) {
                            viewModel.onCopy(clip)
                            val block = item.block as? ClipItemBlock<*>
                            block?.onBind(item.itemView, block.listConfig)
                        }
                    } else {
                        doClearView(item, true) {
                            viewModel.onCopy(clip)
                        }
                    }
                }
                SwipeAction.DELETE -> {
                    viewModel.onDelete(listOf(clip)) {
                        doClearView(item)
                    }
                }
                SwipeAction.TAG -> {
                    doClearView(item, true) {
                        viewModel.onEditTags(clip)
                    }
                }
                else -> {
                    doClearView(item)
                }
            }
        }

        private fun doClearView(item: BlockItemViewHolder<*, *>, withCallback: Boolean = false, callback: () -> Unit = {}) {
            mainAdapter.notifyItemChanged(item.layoutPosition)
            if (withCallback) {
                item.itemView.postDelayed({ callback.invoke() }, viewModel.appConfig.getRuneSwipePowerDelay())
            } else {
                callback.invoke()
            }
        }

        override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
            if (viewHolder is BlockItemViewHolder<*, *>) {
                val clip = viewHolder.block?.let { it as? ClipItemBlock<*> }?.clip
                if (clip != null) {
                    val ctx = recyclerView.context
                    RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addSwipeLeftActionIcon(settings.swipeActionLeft.toIcon(clip))
                        .addSwipeLeftBackgroundColor(settings.swipeActionLeft.toColor(ctx))
                        .addSwipeRightActionIcon(settings.swipeActionRight.toIcon(clip))
                        .addSwipeRightBackgroundColor(settings.swipeActionRight.toColor(ctx))
                        .create()
                        .decorate()
                }
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }
}