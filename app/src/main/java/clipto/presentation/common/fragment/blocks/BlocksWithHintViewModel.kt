package clipto.presentation.common.fragment.blocks

import android.app.Application
import androidx.lifecycle.LiveData
import clipto.common.presentation.mvvm.lifecycle.UniqueLiveData
import clipto.domain.Filter

abstract class BlocksWithHintViewModel(app: Application) : BlocksViewModel(app) {

    private val hintLiveData = UniqueLiveData<HintPresenter>()

    open fun onHideHint() = hintLiveData.value?.let { hintLiveData.postValue(it.copy(hideHint = true)) }
    fun setHint(hint: HintPresenter) = hintLiveData.postValue(hint)
    fun getHintLive(): LiveData<HintPresenter> = hintLiveData

    data class HintPresenter(
        val id: String?,
        val editMode: Boolean,
        val hideHint: Boolean,
        val value: String?,
        val title: String?,
        val onChanged: (value: String?) -> Unit
    )
}