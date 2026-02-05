package clipto.presentation.clip.fastactions

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clipto.domain.FastAction
import clipto.common.misc.ThemeUtils
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.item_fast_action_settings.view.*

@SuppressLint("ClickableViewAccessibility")
class FastActionSettingsAdapter(
    val context: Context,
    val actions: List<FastAction>,
    val editMode: Boolean,
    val clickHandler: (action: FastAction) -> Unit
) : RecyclerView.Adapter<FastActionSettingsAdapter.ViewHolder>() {

    private var recyclerView: RecyclerView? = null
    private var touchHelper: ItemTouchHelper? = null

    override fun getItemCount(): Int = actions.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bindTo(actions[position])
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(parent)

    fun bind(recyclerView: RecyclerView, touchHelper: ItemTouchHelper) {
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        touchHelper.attachToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        this.touchHelper = touchHelper
        recyclerView.adapter = this
    }

    inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_fast_action_settings, parent, false)
    ) {

        var action: FastAction? = null

        init {
            itemView.setOnClickListener {
                if (editMode) {
                    itemView.checkbox.isChecked = !itemView.checkbox.isChecked
                } else {
                    action?.let { clickHandler.invoke(it) }
                }
            }
            itemView.handle.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    touchHelper?.startDrag(this)
                }
                false
            }
            itemView.handle.setImageResource(R.drawable.action_drag)
            itemView.checkbox.setOnCheckedChangeListener { _, isChecked ->
                action?.visible = isChecked
            }
        }

        fun bindTo(action: FastAction?) {
            this.action = action
            action?.let {
                val textView = itemView.textView
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(action.getIconRes(), 0, 0, 0)
                itemView.checkbox.isChecked = it.visible
                textView.setText(action.titleRes)
            }
        }

        fun onItemSelected() {
            itemView.setBackgroundColor(ThemeUtils.getColor(context, R.attr.listItemSelected))
        }

        fun onItemClear() {
            itemView.background = ThemeUtils.getDrawable(context, R.attr.selectableItemBackground)
        }
    }
}