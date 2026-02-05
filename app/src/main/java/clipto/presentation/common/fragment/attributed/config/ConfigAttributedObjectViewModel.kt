package clipto.presentation.common.fragment.attributed.config

import android.app.Application
import clipto.common.presentation.mvvm.RxViewModel
import clipto.domain.ListConfig
import clipto.store.main.MainState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ConfigAttributedObjectViewModel @Inject constructor(
        app: Application,
        private val mainState: MainState
) : RxViewModel(app) {

    val fontsUpdated = mainState.requestFontsUpdate.getLiveData()
    val listConfig = mainState.listConfig.getLiveData()

    fun getVisibleFonts() = mainState.getVisibleFonts()
    fun getListConfig() = mainState.listConfig.requireValue()

    fun onApplyConfig(callback: (state: ListConfig) -> ListConfig) = mainState.requestApplyListConfig(callback = callback)

}