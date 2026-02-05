package clipto.presentation.config.fonts

import android.app.Application
import clipto.action.SaveSettingsAction
import clipto.common.presentation.mvvm.RxViewModel
import clipto.domain.Font
import clipto.domain.ListConfig
import clipto.store.app.AppState
import clipto.store.main.MainState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FontsViewModel @Inject constructor(
        app: Application,
        val appState: AppState,
        val mainState: MainState,
        private val saveSettingsAction: SaveSettingsAction
) : RxViewModel(app) {

    val settingsLive = appState.settings.getLiveData()

    override fun doClear() {
        super.doClear()
        val newFonts = Font.getMoreFonts().map { it.toMeta() }
        val settings = appState.getSettings()
        settings.updateFonts(newFonts)
        saveSettingsAction.execute {
            mainState.requestFontsUpdate()
        }
    }

    fun getTextFont() = appState.getSettings().textFont
    fun getListConfig() = mainState.getListConfig()

    fun onApplyConfig(callback: (state: ListConfig) -> ListConfig) {
        mainState.requestApplyListConfig(callback = callback)
    }

}