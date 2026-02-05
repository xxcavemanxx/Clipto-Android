package clipto.presentation.clip.details.pages.attributes

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clipto.presentation.clip.details.pages.PageFragment
import clipto.presentation.common.recyclerview.BlockListAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AttributesPageFragment : PageFragment<AttributesPageViewModel>() {

    override val viewModel: AttributesPageViewModel by viewModels()

    override fun bind(recyclerView: RecyclerView, viewModel: AttributesPageViewModel) {
        val ctx = requireContext()
        val adapter = BlockListAdapter(this as Fragment)
        recyclerView.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter
        viewModel.viewModeLive.observe(viewLifecycleOwner) { vm ->
            if (vm == null) return@observe
            when (vm) {
                ViewMode.TAGS -> viewModel.tagBlocksLive.value?.let { blocks -> adapter.submitList(blocks) }
                ViewMode.KITS -> viewModel.kitBlocksLive.value?.let { blocks -> adapter.submitList(blocks) }
                ViewMode.FOLDER -> viewModel.folderBlocksLive.value?.let { blocks -> adapter.submitList(blocks) }
            }
        }
        viewModel.tagBlocksLive.observe(viewLifecycleOwner) {
            if (viewModel.viewModeLive.value == ViewMode.TAGS) {
                adapter.submitList(it)
            }
        }
        viewModel.kitBlocksLive.observe(viewLifecycleOwner) {
            if (viewModel.viewModeLive.value == ViewMode.KITS) {
                adapter.submitList(it)
            }
        }
        viewModel.folderBlocksLive.observe(viewLifecycleOwner) {
            if (viewModel.viewModeLive.value == ViewMode.FOLDER) {
                adapter.submitList(it) {
                    recyclerView.scrollToPosition(0)
                }
            }
        }
    }

}