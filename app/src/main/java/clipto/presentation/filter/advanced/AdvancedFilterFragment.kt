package clipto.presentation.filter.advanced

import android.text.style.ForegroundColorSpan
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import clipto.common.extensions.animateScale
import clipto.common.extensions.inBrackets
import clipto.common.extensions.setDebounceClickListener
import clipto.common.presentation.mvvm.MvvmFragment
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.extensions.getActionIconColorHighlight
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.recyclerview.BlockListAdapter
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_filter_advanced.*

@AndroidEntryPoint
class AdvancedFilterFragment : MvvmFragment<AdvancedFilterViewModel>() {

    override val layoutResId: Int = R.layout.fragment_filter_advanced
    override val viewModel: AdvancedFilterViewModel by viewModels()

    private val titleRes by lazy { arguments?.getInt(ATTR_TITLE, R.string.filter_toolbar_title) ?: R.string.filter_toolbar_title }

    override fun bind(viewModel: AdvancedFilterViewModel) {
        val ctx = requireActivity()
        val title = ctx.getString(titleRes)

        // NAVIGATION
        ivBack.setDebounceClickListener { navigateUp() }
        ivClear.setDebounceClickListener {
            viewModel.onClearFilter()
            navigateUp()
        }

        // TITLE
        viewModel.counterLive.observe(viewLifecycleOwner) {
            val counter = it.getFilteredNotesCount().toString().inBrackets()
            val color = if (it.hasActiveFilter()) ctx.getActionIconColorHighlight() else ctx.getTextColorSecondary()
            tvTitle?.text = SimpleSpanBuilder()
                .append(title)
                .append(" ")
                .append(counter, ForegroundColorSpan(color))
                .build()
        }

        // BLOCKS
        val blocksAdapter = BlockListAdapter(this)
        rvBlocks.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        rvBlocks.adapter = blocksAdapter
        viewModel.blocksLive.observe(viewLifecycleOwner) {
            blocksAdapter.submitList(it)
        }

        // SAVE AS
        fabSaveAs.setDebounceClickListener { viewModel.onSaveAs() }
        viewModel.saveAsLive.observe(viewLifecycleOwner) {
            fabSaveAs?.animateScale(it)
        }
    }

    companion object {
        const val ATTR_TITLE = "attr_title"
    }

}