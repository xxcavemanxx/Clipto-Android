package clipto.presentation.lockscreen.pinlock
import android.view.View
import android.os.Bundle

import com.wb.clipboard.databinding.FragmentPinLockBinding
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import clipto.common.extensions.animateScale
import clipto.common.presentation.mvvm.MvvmFragment
import clipto.common.presentation.mvvm.base.FragmentBackButtonListener
import clipto.presentation.lockscreen.PassKeyboardView
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.Executor

@AndroidEntryPoint
class PinLockFragment : MvvmFragment<PinLockViewModel>(), FragmentBackButtonListener {

    
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentPinLockBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
    }
private var _binding: FragmentPinLockBinding? = null
    private val binding get() = _binding!!
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
        binding.passKeyboard.keyboardListener = object : PassKeyboardView.InputListener {
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
        binding.ivLogo?.animateScale(true)
    }

    fun setTouchIdBtnVisibility(visible: Boolean) {
        binding.passKeyboard?.buttonTouchIdVisible = visible
    }

    fun showInputLength(length: Int) {
        binding.indicator?.selectedCount = length
    }

    fun onWrongCode() {
        binding.passKeyboard?.reset()
        binding.indicator?.onWrongCode()
    }

    fun showTouchIdScreen() {
        runCatching { biometricPrompt.authenticate(promptInfo) }
    }

    fun onPinOk() {
        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
