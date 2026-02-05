package clipto.presentation.lockscreen.pinlock

import android.app.Application
import clipto.common.presentation.mvvm.RxViewModel
import clipto.config.IAppConfig
import clipto.presentation.lockscreen.VibratePattern
import clipto.presentation.lockscreen.VibrationUtils
import clipto.repository.ISecurityRepository
import com.wb.clipboard.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Single
import javax.inject.Inject

@HiltViewModel
class PinLockViewModel @Inject constructor(
    app: Application,
    val appConfig: IAppConfig,
    private val securityRepository: ISecurityRepository
) : RxViewModel(app) {

    fun onBind(view: PinLockFragment) {
        securityRepository.isFingerprintEnabled()
            .observeOn(getViewScheduler())
            .subscribeBy("isFingerprintEnabled") { enabled ->
                if (enabled == true) {
                    view.setTouchIdBtnVisibility(enabled)
                    view.showTouchIdScreen()
                } else {
                    view.setTouchIdBtnVisibility(false)
                }
            }
    }

    fun onInput(code: String, view: PinLockFragment) {
        view.showInputLength(code.length)
        if (code.length != BuildConfig.pinCodeLength) return
        securityRepository.checkPassCode(code)
            .flatMap { checked -> if (checked) securityRepository.unlock().toSingle { checked } else Single.just(checked) }
            .observeOn(getViewScheduler())
            .subscribeBy(
                onSuccess = {
                    if (it) {
                        view.onPinOk()
                    } else {
                        view.onWrongCode()
                        VibrationUtils.vibrate(app, VibratePattern.PIN_WRONG)
                    }
                },
                onError = {
                    view.onWrongCode()
                    VibrationUtils.vibrate(app, VibratePattern.PIN_WRONG)
                })
    }

    fun onFingerprintSuccess(view: PinLockFragment) {
        securityRepository.unlock()
            .observeOn(getViewScheduler())
            .subscribeBy { view.onPinOk() }
    }

    fun onForgotClicked(view: PinLockFragment) {

    }

    fun onTouchIdClicked(view: PinLockFragment) {
        view.showTouchIdScreen()
    }
}