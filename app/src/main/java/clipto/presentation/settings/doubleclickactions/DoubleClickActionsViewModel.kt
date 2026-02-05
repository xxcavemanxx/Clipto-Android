package clipto.presentation.settings.doubleclickactions

import android.app.Application
import clipto.common.presentation.mvvm.RxViewModel
import clipto.store.app.AppState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DoubleClickActionsViewModel @Inject constructor(
        app: Application,
        val appState: AppState
) : RxViewModel(app) {

    val settings = appState.getSettings()

}