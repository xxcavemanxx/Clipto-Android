package clipto.presentation.lockscreen.changepasscode

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.viewModels
import androidx.transition.TransitionManager
import clipto.common.presentation.mvvm.MvvmFragment
import clipto.common.presentation.mvvm.base.FragmentBackButtonListener
import clipto.common.presentation.mvvm.base.StatefulFragment
import com.transitionseverywhere.ChangeText
import com.wb.clipboard.R
import clipto.presentation.lockscreen.PassKeyboardView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_change_passcode.*

@AndroidEntryPoint
class ChangePassCodeFragment : MvvmFragment<ChangePassCodeViewModel>(), FragmentBackButtonListener, StatefulFragment {

    override val layoutResId: Int = R.layout.fragment_change_passcode
    override val viewModel: ChangePassCodeViewModel by viewModels()

    override fun bind(viewModel: ChangePassCodeViewModel) {
        toolbar.setNavigationIcon(R.drawable.action_arrow_back)
        toolbar.setNavigationOnClickListener { viewModel.onBackPressed(this) }
        viewModel.inputLengthLiveData.observe(this, {
            it?.run { indicator.selectedCount = it }
        })
        viewModel.statusLiveData.observe(this, {
            when (it) {
                PassCodeStatus.ENTER_EXISTING_PASS -> showEnterExistingPassCode()
                PassCodeStatus.SET_NEW_PASS -> showCreateNewPassCode()
                PassCodeStatus.CONFIRM_NEW_PASS -> showPassCodeConfirmation()
                else -> Unit
            }
        })
        viewModel.wrongCodeLiveData.observe(this, {
            if (it == true) onWrongCode()
        })
        passKeyboard.apply {
            buttonTouchIdVisible = false
            keyboardListener = object : PassKeyboardView.InputListener {
                override fun onInput(code: String) {
                    viewModel.onInput(code, this@ChangePassCodeFragment)
                }
            }
        }
        viewModel.onBind(
                disablePasscode = arguments?.getBoolean(ATTR_DISABLE_PASSCODE) ?: false,
                actionSetFingerPrint = arguments?.getInt(ATTR_ACTION_FINGERPRINT)
                        ?: R.id.action_change_passcode_to_set_touch_id
        )
    }

    override fun onFragmentBackPressed(): Boolean {
        viewModel.onBackPressed(this)
        return true
    }

    private fun showEnterExistingPassCode() {
        animateTitle(R.string.change_passcode_enter_existing)
        indicator.reset()
        passKeyboard.reset()
    }

    private fun showCreateNewPassCode() {
        animateTitle(R.string.change_passcode_set_new)
        indicator.reset(true)
        passKeyboard.reset()
    }

    private fun showPassCodeConfirmation() {
        animateTitle(R.string.change_passcode_reenter)
        indicator.reset(true)
        passKeyboard.reset()
    }

    private fun animateTitle(@StringRes titleRes: Int) {
        TransitionManager.beginDelayedTransition(clParent, ChangeText().setChangeBehavior(
                ChangeText.CHANGE_BEHAVIOR_OUT_IN).setDuration(500).addTarget(tvTitle))
        tvTitle.setText(titleRes)
    }

    private fun onWrongCode() {
        indicator.onWrongCode()
        passKeyboard.reset()
    }

    companion object {
        private const val ATTR_DISABLE_PASSCODE = "attr_disable_passcode"
        private const val ATTR_ACTION_FINGERPRINT = "attr_action_fingerprint"

        fun args(disablePasscode: Boolean, actionSetFingerprint: Int = R.id.action_change_passcode_to_set_touch_id): Bundle = Bundle().apply {
            putInt(ATTR_ACTION_FINGERPRINT, actionSetFingerprint)
            putBoolean(ATTR_DISABLE_PASSCODE, disablePasscode)
        }

    }
}
