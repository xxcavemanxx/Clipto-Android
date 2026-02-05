package clipto.presentation.common.recyclerview

import android.view.View
import androidx.annotation.CallSuper
import clipto.extensions.log

abstract class BlockItem<C> {

    internal abstract val layoutRes: Int

    private var viewHolder: BlockItemViewHolder<C, *>? = null

    open fun onInit(context: C, holder: BlockItemViewHolder<C, *>) {
        log("BlockItem :: onInit: id={}, {}", javaClass.simpleName, holder.hashCode())
        this.viewHolder = holder
        runCatching { onInit(context, holder.itemView) }
    }

    fun onBind(context: C, holder: BlockItemViewHolder<C, *>) {
        log("BlockItem :: onBind: id={}, {}", javaClass.simpleName, holder.hashCode())
        this.viewHolder = holder
        runCatching { onBind(context, holder.itemView) }
    }

    open fun onInit(context: C, block: View) = Unit
    abstract fun onBind(context: C, block: View)

    open fun areItemsTheSame(item: BlockItem<C>): Boolean = layoutRes == item.layoutRes
    open fun areContentsTheSame(item: BlockItem<C>): Boolean = true

    fun getContentView(): View? = viewHolder?.itemView

}