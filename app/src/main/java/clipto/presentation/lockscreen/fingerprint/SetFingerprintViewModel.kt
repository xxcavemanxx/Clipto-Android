package clipto.presentation.lockscreen.fingerprint

import android.app.Application
import clipto.common.presentation.mvvm.RxViewModel
import clipto.common.presentation.mvvm.lifecycle.SingleLiveData
import clipto.repository.ISecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SetFingerprintViewModel @Inject constructor(
    app: Application,
    private val securityRepository: ISecurityRepository
) : RxViewModel(app) {

    val doneLive = SingleLiveData<Boolean>()

    fun onUseFingerprintClick() = saveUserFingerprint(true)
    fun onSkipForNowClick() = saveUserFingerprint(false)

    private fun saveUserFingerprint(useFingerprint: Boolean) = securityRepository
        .setFingerprintEnabled(useFingerprint)
        .subscribeBy { doneLive.postValue(true) }

}
