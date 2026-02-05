package clipto.presentation.config.list

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
import kotlinx.android.synthetic.main.fragment_config_clip_list.*

@AndroidEntryPoint
class ClipListConfigFragment : MvvmBottomSheetDialogFragment<ClipListConfigViewModel>() {

    override val layoutResId: Int = R.layout.fragment_config_clip_list
    override val viewModel: ClipListConfigViewModel by viewModels()

    override fun bind(viewModel: ClipListConfigViewModel) {
        val activity = requireActivity()

        contentView.setBottomSheetHeight(noBackground = true)

        val config = viewModel.getListConfig()

        // text font
        val fontAdapter = TextFontAdapter(activity) {
            if (it.font == Font.MORE) {
                FontsFragment.show(this)
            } else {
                viewModel.onApplyConfig { cfg -> cfg.copy(textFont = it.font.id) }
            }
        }
        textFontRecyclerView?.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        textFontRecyclerView?.adapter = fontAdapter

        viewModel.fontsUpdated.observe(viewLifecycleOwner) {
            val fonts = viewModel.getVisibleFonts()
            val newConfig = viewModel.getListConfig()
            val fontItems = fonts.map { TextFontItem(it, it.id == newConfig.textFont) }
            val indexOfActiveFont = fonts.indexOfFirst { it.id == newConfig.textFont }
            fontAdapter.submitList(fontItems)
            if (indexOfActiveFont != -1) {
                textFontRecyclerView?.smoothScrollToPosition(indexOfActiveFont)
            }
        }

        // text size

        textSizeSeekBar.valueTo = (ClientSession.TEXT_SIZE_MAX - ClientSession.TEXT_SIZE_MIN).toFloat()
        textSizeSeekBar.value = (config.textSize - ClientSession.TEXT_SIZE_MIN).toFloat()
        textSizeSeekBar.addOnChangeListener { _, value, _ ->
            val newTextSize = value.toInt() + ClientSession.TEXT_SIZE_MIN
            viewModel.onApplyConfig { cfg -> cfg.copy(textSize = newTextSize) }
        }

        // text lines
        textLinesSeekBar.valueTo = (ClientSession.TEXT_LINES_MAX - ClientSession.TEXT_LINES_MIN).toFloat()
        textLinesSeekBar.value = (config.textLines - ClientSession.TEXT_LINES_MIN).toFloat()
        textLinesSeekBar.addOnChangeListener { _, value, _ ->
            val newTextLines = value.toInt() + ClientSession.TEXT_LINES_MIN
            viewModel.onApplyConfig { cfg -> cfg.copy(textLines = newTextLines) }
        }

        textSizeDescription?.text = config.textSize.toString()
        textLinesDescription?.text = config.textLines.toString()

        viewModel.listConfig.observe(viewLifecycleOwner) {
            textSizeDescription?.text = it.textSize.toString()
            textLinesDescription?.text = it.textLines.toString()
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
}