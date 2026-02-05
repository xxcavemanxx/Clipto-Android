package clipto.presentation.lockscreen.pinlock

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import clipto.common.extensions.animateScale
import clipto.common.presentation.mvvm.MvvmFragment
import clipto.common.presentation.mvvm.base.FragmentBackButtonListener
import clipto.presentation.lockscreen.PassKeyboardView
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_pin_lock.*
import java.util.concurrent.Executor

@AndroidEntryPoint
class PinLockFragment : MvvmFragment<PinLockViewModel>(), FragmentBackButtonListener {

    override val layoutResId: Int = R.layout.fragment_pin_lock
    override val viewModel: PinLockViewModel by viewModels()

    private val executor: Executor by lazy {
        ContextCompat.getMainExecutor(requireContext())
    }
    private val biometricPrompt: BiometricPrompt by lazy {
        BiometricPrompt(this, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        viewModel.onFingerprintSuccess(this@PinLockFragment)
                    }
                })
    }
    private val promptInfo: BiometricPrompt.PromptInfo by lazy {
        BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.account_button_sign_in))
                .setSubtitle(getString(R.string.auth_fingerprint_touch_sensor))
                .setNegativeButtonText(getString(R.string.menu_cancel))
                .build()
    }

    override fun bind(viewModel: PinLockViewModel) {
        viewModel.onBind(this)
        passKeyboard.keyboardListener = object : PassKeyboardView.InputListener {
            override fun onInput(code: String) {
                viewModel.onInput(code, this@PinLockFragment)
            }

            override fun onForgotClicked() {
                viewModel.onForgotClicked(this@PinLockFragment)
            }

            override fun onTouchIdClick() {
                viewModel.onTouchIdClicked(this@PinLockFragment)
            }
        }
        ivLogo?.animateScale(true)
    }

    fun setTouchIdBtnVisibility(visible: Boolean) {
        passKeyboard?.buttonTouchIdVisible = visible
    }

    fun showInputLength(length: Int) {
        indicator?.selectedCount = length
    }

    fun onWrongCode() {
        passKeyboard?.reset()
        indicator?.onWrongCode()
    }

    fun showTouchIdScreen() {
        runCatching { biometricPrompt.authenticate(promptInfo) }
    }

    fun onPinOk() {
        activity?.finish()
    }
}