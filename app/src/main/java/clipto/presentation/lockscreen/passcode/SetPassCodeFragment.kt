package clipto.presentation.lockscreen.passcode
import android.view.View
import android.os.Bundle

import com.wb.clipboard.databinding.FragmentSetPasscodeBinding
import android.view.Gravity
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.StringRes
import androidx.fragment.app.viewModels
import androidx.transition.Slide
import androidx.transition.TransitionManager
import clipto.common.extensions.animateVisibility
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.setVisibleOrGone
import clipto.common.presentation.mvvm.MvvmFragment
import clipto.common.presentation.mvvm.base.FragmentBackButtonListener
import clipto.presentation.lockscreen.PassKeyboardView
import com.transitionseverywhere.ChangeText
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetPassCodeFragment : MvvmFragment<SetPassCodeViewModel>(), FragmentBackButtonListener {

    
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSetPasscodeBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
    }
private var _binding: FragmentSetPasscodeBinding? = null
    private val binding get() = _binding!!
override val layoutResId: Int = R.layout.fragment_set_passcode
    override val viewModel: SetPassCodeViewModel by viewModels()

    override fun bind(viewModel: SetPassCodeViewModel) {
        withDefaults(binding.toolbar, onBackPressed = { viewModel.onBackPressed(this) })
        binding.contentView.postDelayed({
            binding.tvTitle?.animateVisibility(true)
            binding.indicator?.animateVisibility(true)
            binding.passKeyboard?.animateVisibility(true)

        }, viewModel.appConfig.getUiTimeout())
        binding.btnSkip.setDebounceClickListener { navigateUp() }
        binding.passKeyboard.keyboardListener = object : PassKeyboardView.InputListener {
            override fun onInput(code: String) {
                viewModel.onInput(code, this@SetPassCodeFragment)
            }
        }
    }

    override fun onFragmentBackPressed(): Boolean {
        viewModel.onBackPressed(this@SetPassCodeFragment)
        return true
    }

    fun showSetPassCode() {
        animateBtnSkip(true)
        animateTitle(R.string.auth_set_passcode_label_set_passcode)
        binding.indicator.reset()
    }

    fun showPassCodeConfirmation() {
        animateBtnSkip(false)
        animateTitle(R.string.auth_set_passcode_label_reenter_passcode)
        binding.indicator.reset(true)
        binding.passKeyboard.reset()
    }

    fun showInputLength(length: Int) {
        binding.indicator.selectedCount = length
    }

    fun onWrongPassConfirmation() {
        binding.indicator.onWrongCode()
        binding.passKeyboard.reset()
    }

    private fun animateBtnSkip(show: Boolean) {
        TransitionManager.beginDelayedTransition(
            binding.toolbar, Slide(Gravity.END).setDuration(500)
                .setInterpolator(AccelerateDecelerateInterpolator()).addTarget(binding.btnSkip)
        )
        binding.btnSkip.setVisibleOrGone(show)
    }

    private fun animateTitle(@StringRes titleRes: Int) {
        TransitionManager.beginDelayedTransition(
            binding.contentView, ChangeText().setChangeBehavior(
                ChangeText.CHANGE_BEHAVIOR_OUT_IN
            ).setDuration(500).addTarget(binding.tvTitle)
        )
        binding.tvTitle.setText(titleRes)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
