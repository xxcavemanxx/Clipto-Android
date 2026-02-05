package clipto.presentation.clip.details.pages.general.blocks

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clipto.domain.FastAction
import clipto.domain.Clip
import clipto.extensions.toLinkifiedSpannable
import clipto.presentation.clip.fastactions.FastActionAdapter
import clipto.presentation.clip.fastactions.FastActionsFragment
import clipto.presentation.clip.details.pages.general.GeneralPageFragment
import clipto.presentation.clip.details.pages.general.GeneralPageViewModel
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class FastActionsBlock(
        private val viewModel: GeneralPageViewModel,
        private val clip: Clip
) : BlockItem<GeneralPageFragment>() {

    override val layoutRes: Int = R.layout.block_clip_details_general_fast_actions

    override fun onInit(fragment: GeneralPageFragment, block: View) {
        block as RecyclerView
        val ctx = block.context
        val adapter = FastActionAdapter(clip, ctx) {
            val action = it.action
            if (action == FastAction.MORE) {
                FastActionsFragment.show(fragment, R.attr.colorContext) {
                    viewModel.onFastAction(it)
                }
            } else {
                viewModel.onFastAction(action)
            }
        }
        block.adapter = adapter
        block.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
        viewModel.fastActionsUpdateLive.removeObservers(fragment)
        viewModel.fastActionsUpdateLive.observe(fragment) {
            viewModel.getVisibleClipActions(clip.toLinkifiedSpannable()).let { actions ->
                adapter.submitList(actions)
            }
        }
    }

    override fun onBind(fragment: GeneralPageFragment, block: View) = Unit

}