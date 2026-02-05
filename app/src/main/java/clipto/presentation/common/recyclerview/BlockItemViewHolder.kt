package clipto.presentation.common.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import clipto.extensions.log

class BlockItemViewHolder<C, I : BlockItem<C>>(parent: ViewGroup, type: Int) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(type, parent, false)
) {
    var block: I? = null

    private var inited = false

    fun bind(context: C, block: I?) {
        val canInit = !this.inited
        this.block = block
        if (canInit && block != null) {
            block.onInit(context, this)
            this.inited = true
        }
        block?.onBind(context, this)
    }
}