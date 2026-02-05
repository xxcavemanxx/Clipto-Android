package clipto.presentation.lockscreen.passcode

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
import kotlinx.android.synthetic.main.fragment_set_passcode.*

@AndroidEntryPoint
class SetPassCodeFragment : MvvmFragment<SetPassCodeViewModel>(), FragmentBackButtonListener {

    override val layoutResId: Int = R.layout.fragment_set_passcode
    override val viewModel: SetPassCodeViewModel by viewModels()

    override fun bind(viewModel: SetPassCodeViewModel) {
        withDefaults(toolbar, onBackPressed = { viewModel.onBackPressed(this) })
        contentView.postDelayed({
            tvTitle?.animateVisibility(true)
            indicator?.animateVisibility(true)
            passKeyboard?.animateVisibility(true)

        }, viewModel.appConfig.getUiTimeout())
        btnSkip.setDebounceClickListener { navigateUp() }
        passKeyboard.keyboardListener = object : PassKeyboardView.InputListener {
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
        indicator.reset()
    }

    fun showPassCodeConfirmation() {
        animateBtnSkip(false)
        animateTitle(R.string.auth_set_passcode_label_reenter_passcode)
        indicator.reset(true)
        passKeyboard.reset()
    }

    fun showInputLength(length: Int) {
        indicator.selectedCount = length
    }

    fun onWrongPassConfirmation() {
        indicator.onWrongCode()
        passKeyboard.reset()
    }

    private fun animateBtnSkip(show: Boolean) {
        TransitionManager.beginDelayedTransition(
            toolbar, Slide(Gravity.END).setDuration(500)
                .setInterpolator(AccelerateDecelerateInterpolator()).addTarget(btnSkip)
        )
        btnSkip.setVisibleOrGone(show)
    }

    private fun animateTitle(@StringRes titleRes: Int) {
        TransitionManager.beginDelayedTransition(
            contentView, ChangeText().setChangeBehavior(
                ChangeText.CHANGE_BEHAVIOR_OUT_IN
            ).setDuration(500).addTarget(tvTitle)
        )
        tvTitle.setText(titleRes)
    }

}
