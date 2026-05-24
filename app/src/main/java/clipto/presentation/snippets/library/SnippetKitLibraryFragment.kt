package clipto.presentation.snippets.library
import android.view.View
import android.os.Bundle

import com.wb.clipboard.databinding.FragmentSnippetKitLibraryBinding
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.visible
import clipto.common.misc.Units
import clipto.common.presentation.mvvm.MvvmFragment
import clipto.common.presentation.mvvm.base.StatefulFragment
import clipto.presentation.blocks.ux.ZeroStateVerticalBlock
import clipto.presentation.common.recyclerview.FlowLayoutManagerExt
import clipto.presentation.common.recyclerview.BlockListAdapter
import com.wb.clipboard.R
import com.xiaofeng.flowlayoutmanager.Alignment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SnippetKitLibraryFragment : MvvmFragment<SnippetKitLibraryViewModel>(), StatefulFragment {

    
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSnippetKitLibraryBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
    }
private var _binding: FragmentSnippetKitLibraryBinding? = null
    private val binding get() = _binding!!
override val layoutResId: Int = R.layout.fragment_snippet_kit_library

    override val viewModel: SnippetKitLibraryViewModel by viewModels()

    override fun bind(viewModel: SnippetKitLibraryViewModel) {
        val ctx = requireContext()

        binding.ivBack.setDebounceClickListener { navigateUp() }

        binding.srlBlocks.setOnRefreshListener { viewModel.onRefresh() }

        val categoriesAdapter = BlockListAdapter(this)
        binding.rvCategories.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
        binding.rvCategories.adapter = categoriesAdapter
        viewModel.categoriesBlocksLive.observe(viewLifecycleOwner) {
            categoriesAdapter.submitList(it)
            binding.rvCategories?.visible()
            if (it == null || it.isNotEmpty()) {
                binding.rvBlocks?.updatePadding(top = Units.DP.toPx(64f).toInt())
            } else {
                binding.rvBlocks?.updatePadding(top = Units.DP.toPx(12f).toInt())
            }
        }

        val snippetsAdapter = BlockListAdapter(this)
        binding.rvBlocks.layoutManager = FlowLayoutManagerExt().also {
            it.setAlignment(Alignment.CENTER)
        }
        binding.rvBlocks.adapter = snippetsAdapter
        viewModel.blocksLive.observe(viewLifecycleOwner) {
            if (it.firstOrNull() is ZeroStateVerticalBlock) {
                binding.rvBlocks?.scrollToPosition(0)
            }
            snippetsAdapter.submitList(it)
        }

        viewModel.refreshLive.observe(viewLifecycleOwner) {
            binding.srlBlocks.isRefreshing = it
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
