package clipto.presentation.clip.details

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import clipto.action.SaveSettingsAction
import clipto.common.presentation.mvvm.RxViewModel
import clipto.domain.ClipDetailsTab
import clipto.dynamic.IDynamicValuesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ClipDetailsViewModel @Inject constructor(
    app: Application,
    val state: ClipDetailsState,
    private val saveSettingsAction: SaveSettingsAction,
    private val dynamicValuesRepository: IDynamicValuesRepository
) : RxViewModel(app) {

    fun getDynamicFieldsCountLive(): LiveData<Int> {
        val liveData = MutableLiveData<Int>()
        state.openedClip.getLiveChanges()
            .filter { it.isNotNull() }
            .map { it.requireValue() }
            .flatMapSingle { dynamicValuesRepository.getFieldsCount(it) }
            .subscribeBy("getDynamicFieldsCountLive") {
                liveData.postValue(it)
            }
        return liveData
    }

    fun onClose() {
        state.getSettingsIfChanged()?.let { saveSettingsAction.execute() }
    }

    fun onSelectTab(tab: ClipDetailsTab) {
        val viewMode = ViewMode.valueOf(state.selectedTab.requireValue())
        val newViewMode = ViewMode.valueOf(tab)
        if (newViewMode != viewMode) {
            state.selectedTab.setValue(tab)
        }
    }

}