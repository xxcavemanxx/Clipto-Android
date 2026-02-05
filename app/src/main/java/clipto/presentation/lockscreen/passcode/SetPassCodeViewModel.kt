package clipto.presentation.lockscreen.passcode

import android.app.Application
import clipto.common.extensions.getNavController
import clipto.common.extensions.navigateTo
import clipto.common.presentation.mvvm.RxViewModel
import clipto.config.IAppConfig
import clipto.presentation.lockscreen.VibratePattern
import clipto.presentation.lockscreen.VibrationUtils
import clipto.repository.ISecurityRepository
import clipto.store.app.AppState
import clipto.store.lock.LockState
import com.wb.clipboard.BuildConfig
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SetPassCodeViewModel @Inject constructor(
    app: Application,
    val appConfig: IAppConfig,
    private val appState: AppState,
    private val lockState: LockState,
    private val securityRepository: ISecurityRepository
) : RxViewModel(app) {

    private var code = ""
    private var codeEntered: Boolean = false

    fun onInput(input: String, view: SetPassCodeFragment) {
        if (!codeEntered) {
            onCode(input, view)
        } else {
            onCodeConfirmation(input, view)
        }
    }

    private fun onCode(input: String, view: SetPassCodeFragment) {
        code = input
        view.showInputLength(input.length)
        if (input.length == BuildConfig.pinCodeLength) {
            codeEntered = true
            view.showPassCodeConfirmation()
            VibrationUtils.vibrate(app, VibratePattern.PIN_OK)
        }
    }

    private fun onCodeConfirmation(input: String, view: SetPassCodeFragment) {
        view.showInputLength(input.length)
        when {
            input == code -> {
                VibrationUtils.vibrate(app, VibratePattern.PIN_OK)
                onCodeConfirmed(code, view)
            }
            input.length >= BuildConfig.pinCodeLength -> {
                VibrationUtils.vibrate(app, VibratePattern.PIN_WRONG)
                view.onWrongPassConfirmation()
            }
        }
    }

    fun onBackPressed(view: SetPassCodeFragment) {
        if (codeEntered) {
            reset(view)
        } else {
            onPasscodeFinish(view)
        }
    }

    private fun reset(view: SetPassCodeFragment) {
        code = ""
        codeEntered = false
        view.showSetPassCode()
    }

    private fun onCodeConfirmed(confirmedCode: String, view: SetPassCodeFragment) {
        securityRepository.savePassCode(confirmedCode).toSingle { confirmedCode }
            .map { lockState.isFingerprintAvailable.requireValue() }
            .observeOn(getViewScheduler())
            .subscribeBy(
                "onCodeConfirmed",
                {
                    if (it && !appState.getSettings().useFingerprint) {
                        view.navigateTo(R.id.action_set_passcode_to_set_touch_id)
                    } else {
                        onPasscodeFinish(view)
                    }
                }, {
                    onPasscodeFinish(view)
                })

    }

    private fun onPasscodeFinish(view: SetPassCodeFragment) {
        view.getNavController().navigateUp()
    }
}