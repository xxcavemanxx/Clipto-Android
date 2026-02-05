package clipto.presentation.preview.video

import android.app.Application
import androidx.lifecycle.MutableLiveData
import clipto.common.presentation.mvvm.ViewModel
import clipto.common.presentation.mvvm.lifecycle.SingleLiveData
import clipto.common.presentation.mvvm.lifecycle.UniqueLiveData


open class VideoPreviewEmbedViewModel(app: Application) : ViewModel(app) {

    val fullScreenLiveData: MutableLiveData<Boolean> = UniqueLiveData()
    val errorLiveData: MutableLiveData<Exception> = SingleLiveData()
    val dataLiveData: MutableLiveData<Data> = UniqueLiveData()

    fun isInFullScreen() = fullScreenLiveData.value == true

    fun onEnterFullscreen() {
        fullScreenLiveData.value = true
    }

    fun onExitFullscreen() {
        fullScreenLiveData.value = false
    }

    data class Data(val slideOffset: Float? = null, val bottomSheetState: Int? = null)

}