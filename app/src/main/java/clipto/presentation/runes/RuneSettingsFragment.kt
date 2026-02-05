package clipto.presentation.runes

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import clipto.common.extensions.getNavController
import clipto.common.extensions.setDebounceClickListener
import clipto.common.presentation.mvvm.MvvmFragment
import clipto.presentation.runes.extensions.getBgColor
import clipto.presentation.runes.extensions.getIconColor
import clipto.presentation.runes.extensions.getTextColor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_rune_settings.*

@AndroidEntryPoint
class RuneSettingsFragment : MvvmFragment<RuneSettingsViewModel>() {

    override val layoutResId: Int by lazy { viewModel.getLayoutRes() }
    override val viewModel: RuneSettingsViewModel by viewModels()
    private var isExpanded: Boolean? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        isExpanded?.let { outState.putBoolean("expanded", it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        isExpanded = savedInstanceState?.getBoolean("expanded")
        super.onViewCreated(view, savedInstanceState)
    }

    override fun bind(viewModel: RuneSettingsViewModel) {
        val ctx = requireContext()

        ivBack.setDebounceClickListener { getNavController().navigateUp() }

        viewModel.runeProviderLive.observe(this) {
            val isActive = it.isActive()

            nameText.text = it.getTitle()
            nameText.setTextColor(it.getTextColor(ctx, isActive))

            iconView.imageTintList = ColorStateList.valueOf(it.getIconColor(ctx, isActive))
            iconView.setImageResource(it.getIcon())

            bgView.imageTintList = ColorStateList.valueOf(it.getBgColor(ctx, isActive))

            iconView.refreshDrawableState()
            bgView.refreshDrawableState()

            if (rvBlocks?.adapter == null) it.bind(this)
        }
    }

    override fun onDestroyView() {
        viewModel.onSaveRune()
        super.onDestroyView()
    }

}
