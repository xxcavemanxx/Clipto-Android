package clipto.presentation.clip.fastactions

import androidx.annotation.AttrRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import clipto.analytics.Analytics
import clipto.common.extensions.setBottomSheetHeight
import clipto.common.extensions.withSafeFragmentManager
import clipto.common.presentation.mvvm.MvvmBottomSheetDialogFragment
import clipto.common.presentation.mvvm.lifecycle.SingleLiveData
import clipto.domain.FastAction
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_clip_fast_actions.*
import java.util.*

@AndroidEntryPoint
class FastActionsFragment : MvvmBottomSheetDialogFragment<FastActionsViewModel>() {

    override val layoutResId: Int = R.layout.fragment_clip_fast_actions
    override val viewModel: FastActionsViewModel by activityViewModels()

    override fun bind(viewModel: FastActionsViewModel) {
        val ctx = viewModel.app
        val titleRes = viewModel.titleRes
        val editMode = viewModel.editMode
        contentView.setBottomSheetHeight(noBackground = true)

        tvTitle.setText(titleRes)

        val actions = ArrayList(FastAction.getMoreActions())
        val adapter = FastActionSettingsAdapter(ctx, actions, editMode) { action ->
            viewModel.onClicked(action)
            dismissAllowingStateLoss()
        }

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

            override fun onMove(
                recyclerView: RecyclerView,
                dragged: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val draggedPosition = dragged.adapterPosition
                val targetPosition = target.adapterPosition
                actions[draggedPosition].order = targetPosition
                actions[targetPosition].order = draggedPosition
                Collections.swap(actions, draggedPosition, targetPosition)
                adapter.notifyItemMoved(draggedPosition, targetPosition)
                return true
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                    if (viewHolder is FastActionSettingsAdapter.ViewHolder) {
                        viewHolder.onItemSelected()
                    }
                }
                super.onSelectedChanged(viewHolder, actionState)
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                if (viewHolder is FastActionSettingsAdapter.ViewHolder) {
                    viewHolder.onItemClear()
                }
            }

            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int = 0
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
        })

        adapter.bind(recyclerView, touchHelper)

        Analytics.screenFastActions()
    }

    override fun onDestroyView() {
        viewModel.onSave()
        super.onDestroyView()
    }

    companion object {
        fun show(
            fragment: Fragment,
            @AttrRes backgroundAttr: Int,
            titleRes: Int = R.string.fast_actions_title,
            editMode: Boolean = false,
            callback: (action: FastAction) -> Unit = {}
        ) {
            fragment.withSafeFragmentManager()?.let { fm ->
                val viewModel = fragment.activityViewModels<FastActionsViewModel>().value
                val newFastActionLive = SingleLiveData<FastAction>().also { live ->
                    live.observe(fragment) { action ->
                        live.removeObservers(fragment)
                        action?.let(callback)
                    }
                }
                viewModel.apply {
                    this.backgroundAttr = backgroundAttr
                    this.titleRes = titleRes
                    this.editMode = editMode
                    this.fastActionLive.removeObservers(fragment)
                    this.fastActionLive = newFastActionLive
                }
                FastActionsFragment().show(fm, "FastActionsFragment")
            }
        }
    }

}