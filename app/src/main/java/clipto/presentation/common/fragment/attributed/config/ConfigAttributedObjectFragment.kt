package clipto.presentation.common.fragment.attributed.config

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
import kotlinx.android.synthetic.main.fragment_config_attributed_object.*

@AndroidEntryPoint
class ConfigAttributedObjectFragment : MvvmBottomSheetDialogFragment<ConfigAttributedObjectViewModel>() {

    override val layoutResId: Int = R.layout.fragment_config_attributed_object
    override val viewModel: ConfigAttributedObjectViewModel by viewModels()

    override fun bind(viewModel: ConfigAttributedObjectViewModel) {
        val activity = requireActivity()

        contentView.setBottomSheetHeight(noBackground = true)

        val fontAdapter = TextFontAdapter(activity) {
            if (it.font == Font.MORE) {
                FontsFragment.show(this)
            } else {
                viewModel.onApplyConfig { config -> config.copy(textFont = it.font.id) }
            }

        }
        textFontRecyclerView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        textFontRecyclerView.adapter = fontAdapter
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

        textSizeSeekBar.valueTo = (ClientSession.TEXT_SIZE_MAX - ClientSession.TEXT_SIZE_MIN).toFloat()
        textSizeSeekBar.value = (viewModel.getListConfig().textSize - ClientSession.TEXT_SIZE_MIN).toFloat()
        textSizeSeekBar.addOnChangeListener { _, value, _ ->
            val newTextSize = value.toInt() + ClientSession.TEXT_SIZE_MIN
            viewModel.onApplyConfig { it.copy(textSize = newTextSize) }
        }

        textSizeDescription?.text = viewModel.getListConfig().textSize.toString()

        viewModel.listConfig.observe(viewLifecycleOwner) {
            textSizeDescription?.text = it.textSize.toString()
        }

        Analytics.screenConfigClip()
    }

    companion object {
        const val TAG = "ClipConfigFragment"

        fun show(context: Context) {
            context.withSafeFragmentManager()?.let { fm ->
                ConfigAttributedObjectFragment().show(fm, TAG)
            }
        }
    }
}