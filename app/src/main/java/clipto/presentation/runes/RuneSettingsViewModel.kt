package clipto.presentation.runes

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import clipto.action.SaveSettingsAction
import clipto.common.presentation.mvvm.RxViewModel
import clipto.config.IAppConfig
import clipto.repository.IRunesRepository
import clipto.store.app.AppState
import clipto.store.user.UserState
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RuneSettingsViewModel @Inject constructor(
    app: Application,
    val appState: AppState,
    val userState: UserState,
    val appConfig: IAppConfig,
    val repository: IRunesRepository,
    val saveSettingsAction: SaveSettingsAction,
    savedStateHandle: SavedStateHandle
) : RxViewModel(app) {

    private val runeId: String by lazy { savedStateHandle.get(ATTR_RUNE_ID)!! }

    val runeProviderLive = MutableLiveData<RuneSettingsProvider>()

    override fun doSubscribe() {
        repository.getById(runeId)
            .subscribeBy("getById") {
                if (it is RuneSettingsProvider) runeProviderLive.postValue(it)
            }
    }

    fun getLayoutRes(): Int = R.layout.fragment_rune_settings

    fun onRefreshRune() {
        runeProviderLive.value?.let { runeProviderLive.postValue(it) }
    }

    fun onSaveRune() {
        saveSettingsAction.execute()
    }

    companion object {
        private const val ATTR_RUNE_ID = "rune_id"

        fun withArgs(runeId: String): Bundle = Bundle().apply {
            putString(ATTR_RUNE_ID, runeId)
        }
    }

}