package clipto.presentation.clip.details.pages.general

import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clipto.presentation.clip.details.pages.PageFragment
import clipto.presentation.common.recyclerview.BlockListAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GeneralPageFragment : PageFragment<GeneralPageViewModel>() {

    override val viewModel: GeneralPageViewModel by viewModels()

    override fun bind(recyclerView: RecyclerView, viewModel: GeneralPageViewModel) {
        val ctx = requireContext()
        val adapter = BlockListAdapter(this)
        recyclerView.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter
        viewModel.blocksLive.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
    }

}