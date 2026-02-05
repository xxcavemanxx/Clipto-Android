package clipto.presentation.clip.fastactions

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import clipto.domain.FastAction
import clipto.common.extensions.showToast
import clipto.domain.Clip
import com.wb.clipboard.R

class FastActionAdapter(
    val clip: Clip,
    val context: Context,
    val clickHandler: (action: FastAction.ClipAction) -> Unit
) : ListAdapter<FastAction.ClipAction, FastActionAdapter.ViewHolder>(diffCallback) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bindTo(getItem(position))
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(parent)

    inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_fast_action, parent, false)
    ) {

        var clipAction: FastAction.ClipAction? = null

        init {
            itemView.setOnClickListener { clipAction?.let { clickHandler.invoke(it) } }
            itemView.setOnLongClickListener {
                clipAction?.let {
                    context.showToast(context.getString(it.action.titleRes))
                }
                true
            }
            itemView.isFocusable = false
        }

        fun bindTo(clipAction: FastAction.ClipAction?) {
            this.clipAction = clipAction
            val action = clipAction?.action
            action?.let {
                itemView as TextView
                itemView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, action.getIconRoundedRes(), 0, 0)
                itemView.contentDescription = context.getString(action.titleRes)
                itemView.text = clipAction.label
            }
        }
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<FastAction.ClipAction>() {
            override fun areItemsTheSame(oldItem: FastAction.ClipAction, newItem: FastAction.ClipAction): Boolean =
                oldItem.action == newItem.action

            override fun areContentsTheSame(oldItem: FastAction.ClipAction, newItem: FastAction.ClipAction): Boolean =
                oldItem == newItem

            override fun getChangePayload(oldItem: FastAction.ClipAction, newItem: FastAction.ClipAction): Any = newItem
        }
    }
}