package clipto.presentation.config.list
import android.view.View
import android.os.Bundle

import com.wb.clipboard.databinding.FragmentConfigClipListBinding
import android.content.Context
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import clipto.analytics.Analytics
import clipto.common.extensions.setBottomSheetHeight
import clipto.common.extensions.withSafeFragmentManager
import clipto.common.presentation.mvvm.MvvmBottomSheetDialogFragment
import clipto.domain.ClientSession
import clipto.domain.Font
import clipto.presentation.config.TextFontAdapter
import clipto.presentation.config.TextFontItem
import clipto.presentation.config.fonts.FontsFragment
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ClipListConfigFragment : MvvmBottomSheetDialogFragment<ClipListConfigViewModel>() {

    
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentConfigClipListBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
    }
private var _binding: FragmentConfigClipListBinding? = null
    private val binding get() = _binding!!
override val layoutResId: Int = R.layout.fragment_config_clip_list
    override val viewModel: ClipListConfigViewModel by viewModels()

    override fun bind(viewModel: ClipListConfigViewModel) {
        val activity = requireActivity()

        binding.contentView.setBottomSheetHeight(noBackground = true)

        val config = viewModel.getListConfig()

        // text font
        val fontAdapter = TextFontAdapter(activity) {
            if (it.font == Font.MORE) {
                FontsFragment.show(this)
            } else {
                viewModel.onApplyConfig { cfg -> cfg.copy(textFont = it.font.id) }
            }
        }
        binding.textFontRecyclerView?.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        binding.textFontRecyclerView?.adapter = fontAdapter

        viewModel.fontsUpdated.observe(viewLifecycleOwner) {
            val fonts = viewModel.getVisibleFonts()
            val newConfig = viewModel.getListConfig()
            val fontItems = fonts.map { TextFontItem(it, it.id == newConfig.textFont) }
            val indexOfActiveFont = fonts.indexOfFirst { it.id == newConfig.textFont }
            fontAdapter.submitList(fontItems)
            if (indexOfActiveFont != -1) {
                binding.textFontRecyclerView?.smoothScrollToPosition(indexOfActiveFont)
            }
        }

        // text size

        binding.textSizeSeekBar.valueTo = (ClientSession.TEXT_SIZE_MAX - ClientSession.TEXT_SIZE_MIN).toFloat()
        binding.textSizeSeekBar.value = (config.textSize - ClientSession.TEXT_SIZE_MIN).toFloat()
        binding.textSizeSeekBar.addOnChangeListener { _, value, _ ->
            val newTextSize = value.toInt() + ClientSession.TEXT_SIZE_MIN
            viewModel.onApplyConfig { cfg -> cfg.copy(textSize = newTextSize) }
        }

        // text lines
        binding.textLinesSeekBar.valueTo = (ClientSession.TEXT_LINES_MAX - ClientSession.TEXT_LINES_MIN).toFloat()
        binding.textLinesSeekBar.value = (config.textLines - ClientSession.TEXT_LINES_MIN).toFloat()
        binding.textLinesSeekBar.addOnChangeListener { _, value, _ ->
            val newTextLines = value.toInt() + ClientSession.TEXT_LINES_MIN
            viewModel.onApplyConfig { cfg -> cfg.copy(textLines = newTextLines) }
        }

        binding.textSizeDescription?.text = config.textSize.toString()
        binding.textLinesDescription?.text = config.textLines.toString()

        viewModel.listConfig.observe(viewLifecycleOwner) {
            binding.textSizeDescription?.text = it.textSize.toString()
            binding.textLinesDescription?.text = it.textLines.toString()
        }

        Analytics.screenConfigClipList()
    }

    companion object {
        const val TAG = "ClipListConfigFragment"

        fun show(context: Context) {
            context.withSafeFragmentManager()?.let { fm ->
                ClipListConfigFragment().show(fm, TAG)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
