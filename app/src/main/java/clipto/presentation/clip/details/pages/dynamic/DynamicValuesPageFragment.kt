package clipto.presentation.clip.details.pages.dynamic

import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clipto.presentation.clip.details.pages.PageFragment
import clipto.presentation.clip.list.ClipListAdapter
import clipto.presentation.common.recyclerview.BlockListAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DynamicValuesPageFragment : PageFragment<DynamicValuesPageViewModel>() {

    override val viewModel: DynamicValuesPageViewModel by viewModels()

    override fun bind(recyclerView: RecyclerView, viewModel: DynamicValuesPageViewModel) {
        val ctx = requireContext()

        // blocks
        val blocksAdapter = BlockListAdapter(this)

        // actions
        val actionsAdapter = BlockListAdapter(this)

        // clips
        val clipAdapter = ClipListAdapter(
                context = ctx,
                withMainState = viewModel.mainState,
                withClickHandler = { it, _ -> viewModel.onSnippet(it) },
                withTextConstraint = { viewModel.getSearchByText() }
        ).withListConfig(viewModel.listConfig)

        recyclerView.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = ConcatAdapter(blocksAdapter, actionsAdapter, clipAdapter)

        viewModel.blocksLive.observe(viewLifecycleOwner) {
            blocksAdapter.submitList(it)
        }

        viewModel.actionsLive.observe(viewLifecycleOwner) {
            actionsAdapter.submitList(it)
        }

        viewModel.clipsLive.observe(viewLifecycleOwner) {
            clipAdapter.submitList(it) {
                recyclerView.scrollToPosition(0)
            }
        }
    }

}