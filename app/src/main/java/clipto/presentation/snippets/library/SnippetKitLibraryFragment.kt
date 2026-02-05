package clipto.presentation.snippets.library

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
import kotlinx.android.synthetic.main.fragment_snippet_kit_library.*

@AndroidEntryPoint
class SnippetKitLibraryFragment : MvvmFragment<SnippetKitLibraryViewModel>(), StatefulFragment {

    override val layoutResId: Int = R.layout.fragment_snippet_kit_library

    override val viewModel: SnippetKitLibraryViewModel by viewModels()

    override fun bind(viewModel: SnippetKitLibraryViewModel) {
        val ctx = requireContext()

        ivBack.setDebounceClickListener { navigateUp() }

        srlBlocks.setOnRefreshListener { viewModel.onRefresh() }

        val categoriesAdapter = BlockListAdapter(this)
        rvCategories.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
        rvCategories.adapter = categoriesAdapter
        viewModel.categoriesBlocksLive.observe(viewLifecycleOwner) {
            categoriesAdapter.submitList(it)
            rvCategories?.visible()
            if (it == null || it.isNotEmpty()) {
                rvBlocks?.updatePadding(top = Units.DP.toPx(64f).toInt())
            } else {
                rvBlocks?.updatePadding(top = Units.DP.toPx(12f).toInt())
            }
        }

        val snippetsAdapter = BlockListAdapter(this)
        rvBlocks.layoutManager = FlowLayoutManagerExt().also {
            it.setAlignment(Alignment.CENTER)
        }
        rvBlocks.adapter = snippetsAdapter
        viewModel.blocksLive.observe(viewLifecycleOwner) {
            if (it.firstOrNull() is ZeroStateVerticalBlock) {
                rvBlocks?.scrollToPosition(0)
            }
            snippetsAdapter.submitList(it)
        }

        viewModel.refreshLive.observe(viewLifecycleOwner) {
            srlBlocks.isRefreshing = it
        }
    }

}