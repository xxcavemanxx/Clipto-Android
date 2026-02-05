package clipto.presentation.clip.details.pages.attachments

import androidx.fragment.app.viewModels
import androidx.paging.PagedList
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clipto.presentation.clip.details.pages.PageFragment
import clipto.presentation.common.recyclerview.BlockListAdapter
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.recyclerview.BlockPagedListAdapter
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AttachmentsPageFragment : PageFragment<AttachmentsPageViewModel>() {

    override val viewModel: AttachmentsPageViewModel by viewModels()

    override fun bind(recyclerView: RecyclerView, viewModel: AttachmentsPageViewModel) {
        val ctx = requireContext()

        val actionsAdapter = BlockListAdapter(this)
        val searchFilesAdapter = BlockListAdapter(this)
        val selectedFilesAdapter = BlockListAdapter(this)
        val filesAdapter = BlockPagedListAdapter(this)
        val bottomSpaceAdapter = BlockListAdapter(this)

        recyclerView.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = ConcatAdapter(actionsAdapter, selectedFilesAdapter, searchFilesAdapter, filesAdapter, bottomSpaceAdapter)

        viewModel.actionBlocksLive.observe(viewLifecycleOwner) {
            actionsAdapter.submitList(it)
        }

        viewModel.searchFilesBlocksLive.observe(viewLifecycleOwner) {
            searchFilesAdapter.submitList(it)
        }

        viewModel.selectedFilesBlocksLive.observe(viewLifecycleOwner) {
            selectedFilesAdapter.submitList(it)
        }

        viewModel.bottomSpaceBlocksLive.observe(viewLifecycleOwner) {
            bottomSpaceAdapter.submitList(it)
        }

        viewModel.fileBlocksLive.observe(viewLifecycleOwner) {
            @Suppress("UNCHECKED_CAST")
            filesAdapter.submitList(it as? PagedList<BlockItem<AttachmentsPageFragment>>)
        }
    }

}