package clipto.presentation.clip.fastactions

import android.app.Application
import androidx.lifecycle.MutableLiveData
import clipto.action.SaveSettingsAction
import clipto.common.presentation.mvvm.RxViewModel
import clipto.common.presentation.mvvm.lifecycle.SingleLiveData
import clipto.domain.FastAction
import clipto.store.app.AppState
import clipto.store.clipboard.ClipboardState
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FastActionsViewModel @Inject constructor(
    app: Application,
    private val appState: AppState,
    private val clipboardState: ClipboardState,
    private val saveSettingsAction: SaveSettingsAction
) : RxViewModel(app) {

    var titleRes = R.string.fast_actions_title
    var backgroundAttr = R.attr.colorContext
    var editMode = false

    var fastActionLive: MutableLiveData<FastAction> = SingleLiveData()
    val settings = appState.getSettings()
    val theme = appState.getTheme()

    fun onSave() {
        val prevActions = settings.fastActionsMeta
        val newActions = FastAction.getMoreActions().map { it.toMeta() }
        if (prevActions != newActions) {
            settings.updateFastActions(newActions)
            saveSettingsAction.execute {
                appState.requestFastActionsUpdate()
                clipboardState.refreshNotification()
            }
        }
    }

    fun onClicked(action: FastAction) = fastActionLive.postValue(action)

}