package clipto.presentation.runes

import com.wb.clipboard.databinding.FragmentRuneSettingsBinding
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

@AndroidEntryPoint
class RuneSettingsFragment : MvvmFragment<RuneSettingsViewModel>() {

    
    private var _binding: FragmentRuneSettingsBinding? = null
    val binding get() = _binding!!
override val layoutResId: Int by lazy { viewModel.getLayoutRes() }
    override val viewModel: RuneSettingsViewModel by viewModels()
    private var isExpanded: Boolean? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        isExpanded?.let { outState.putBoolean("expanded", it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentRuneSettingsBinding.bind(view)
        isExpanded = savedInstanceState?.getBoolean("expanded")
        super.onViewCreated(view, savedInstanceState)
    }

    override fun bind(viewModel: RuneSettingsViewModel) {
        val ctx = requireContext()

        binding.ivBack.setDebounceClickListener { getNavController().navigateUp() }

        viewModel.runeProviderLive.observe(this) {
            val isActive = it.isActive()

            binding.nameText.text = it.getTitle()
            binding.nameText.setTextColor(it.getTextColor(ctx, isActive))

            binding.iconView.imageTintList = ColorStateList.valueOf(it.getIconColor(ctx, isActive))
            binding.iconView.setImageResource(it.getIcon())

            binding.bgView.imageTintList = ColorStateList.valueOf(it.getBgColor(ctx, isActive))

            binding.iconView.refreshDrawableState()
            binding.bgView.refreshDrawableState()

            if (binding.rvBlocks?.adapter == null) it.bind(this)
        }
    }

    override fun onDestroyView() {
        _binding = null
        viewModel.onSaveRune()
        super.onDestroyView()
    }

}
