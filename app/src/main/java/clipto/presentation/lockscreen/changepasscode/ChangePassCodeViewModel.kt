package clipto.presentation.lockscreen.changepasscode

import android.app.Application
import androidx.lifecycle.MutableLiveData
import clipto.analytics.Analytics
import clipto.common.extensions.getNavController
import clipto.common.extensions.navigateTo
import clipto.common.presentation.mvvm.RxViewModel
import clipto.common.presentation.mvvm.lifecycle.SingleLiveData
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
class ChangePassCodeViewModel @Inject constructor(
    app: Application,
    private val appState: AppState,
    private val lockState: LockState,
    private val repository: ISecurityRepository
) : RxViewModel(app) {

    var statusLiveData = MutableLiveData<PassCodeStatus>(); private set
    var inputLengthLiveData = MutableLiveData<Int>(); private set
    var wrongCodeLiveData = SingleLiveData<Boolean>(); private set
    var actionSetFingerPrint = R.id.action_change_passcode_to_set_touch_id
    private var disablePasscode: Boolean = false
    private var code = ""

    override fun doClear() {
        super.doClear()
        appState.refreshSettings()
    }

    fun onBind(disablePasscode: Boolean, actionSetFingerPrint: Int) {
        reset()
        this.disablePasscode = disablePasscode
        this.actionSetFingerPrint = actionSetFingerPrint
        repository.isPassCodeSet()
            .map { if (it) PassCodeStatus.ENTER_EXISTING_PASS else PassCodeStatus.SET_NEW_PASS }
            .doOnSuccess { sendStatisticsEvent(it) }
            .subscribeBy("isPassCodeSet") { statusLiveData.postValue(it) }
    }

    private fun reset() {
        inputLengthLiveData.value = 0
        code = ""
    }

    fun onInput(input: String, view: ChangePassCodeFragment) {
        inputLengthLiveData.value = input.length
        when (statusLiveData.value) {
            PassCodeStatus.SET_NEW_PASS -> onSetNewCode(input)
            PassCodeStatus.ENTER_EXISTING_PASS -> onCurrentCode(input, view)
            PassCodeStatus.CONFIRM_NEW_PASS -> onConfirmNewCode(input, view)
        }
    }

    private fun onCurrentCode(input: String, view: ChangePassCodeFragment) {
        if (input.length != BuildConfig.pinCodeLength) return
        repository.checkPassCode(input)
            .observeOn(getViewScheduler())
            .subscribeBy("onCurrentCode") {
                if (it) {
                    if (disablePasscode) {
                        appState.getSettings().passcode = null
                        appState.getSettings().useFingerprint = false
                        appState.refreshSettings()
                        view.getNavController().navigateUp()
                    } else {
                        statusLiveData.postValue(PassCodeStatus.SET_NEW_PASS)
                        sendStatisticsEvent(PassCodeStatus.SET_NEW_PASS)
                    }
                } else {
                    wrongCodeLiveData.postValue(true)
                    VibrationUtils.vibrate(app, VibratePattern.PIN_WRONG)
                }
            }
    }

    private fun onSetNewCode(input: String) {
        code = input
        if (input.length == BuildConfig.pinCodeLength) {
            statusLiveData.value = PassCodeStatus.CONFIRM_NEW_PASS
            sendStatisticsEvent(PassCodeStatus.CONFIRM_NEW_PASS)
            VibrationUtils.vibrate(app, VibratePattern.PIN_OK)
        }
    }

    private fun onConfirmNewCode(input: String, view: ChangePassCodeFragment) {
        when {
            input == code -> {
                VibrationUtils.vibrate(app, VibratePattern.PIN_OK)
                onNewCodeConfirmed(code, view)
            }
            input.length >= BuildConfig.pinCodeLength -> {
                VibrationUtils.vibrate(app, VibratePattern.PIN_WRONG)
                wrongCodeLiveData.postValue(true)
            }
        }
    }

    fun onBackPressed(view: ChangePassCodeFragment) {
        when (statusLiveData.value) {
            PassCodeStatus.CONFIRM_NEW_PASS -> {
                code = ""
                statusLiveData.value = PassCodeStatus.SET_NEW_PASS
            }
            else -> view.getNavController().navigateUp()
        }
    }

    private fun onNewCodeConfirmed(confirmedCode: String, view: ChangePassCodeFragment) {
        repository.savePassCode(confirmedCode).toSingle { confirmedCode }
            .map { lockState.isFingerprintAvailable.requireValue() }
            .observeOn(getViewScheduler())
            .subscribeBy(
                "onNewCodeConfirmed",
                {
                    if (it && !appState.getSettings().useFingerprint) {
                        view.navigateTo(actionSetFingerPrint)
                    } else {
                        view.getNavController().navigateUp()
                    }
                }, {
                    view.getNavController().navigateUp()
                })
    }

    private fun sendStatisticsEvent(status: PassCodeStatus) {
        val screen = when (status) {
            PassCodeStatus.ENTER_EXISTING_PASS -> "passcode_changing"
            PassCodeStatus.SET_NEW_PASS -> "passcode_setting"
            PassCodeStatus.CONFIRM_NEW_PASS -> "passcode_setting_confirm"
        }
        Analytics.onScreen(screen)
    }
}
