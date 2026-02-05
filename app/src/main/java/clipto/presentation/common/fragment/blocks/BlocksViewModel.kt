package clipto.presentation.common.fragment.blocks

import android.app.Application
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import clipto.common.presentation.mvvm.RxViewModel
import clipto.common.presentation.mvvm.lifecycle.UniqueLiveData
import clipto.config.IAppConfig
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.contextactions.ContextActionsState
import clipto.store.app.AppState
import clipto.store.main.MainState
import javax.inject.Inject

abstract class BlocksViewModel(app: Application) : RxViewModel(app) {

    @Inject
    lateinit var appState: AppState

    @Inject
    lateinit var mainState: MainState

    @Inject
    lateinit var appConfig: IAppConfig

    @Inject
    lateinit var dialogState: DialogState

    @Inject
    lateinit var contextActionsState: ContextActionsState

    private val blocksLiveData: MutableLiveData<BlocksData> = MutableLiveData()
    val showHideKeyboard: MutableLiveData<Boolean> = UniqueLiveData()

    fun postBlocks(blocks: List<BlockItem<Fragment>>, scrollToTop: Boolean = false) = blocksLiveData.postValue(BlocksData(blocks, scrollToTop))

    fun setBlocks(blocks: List<BlockItem<Fragment>>, scrollToTop: Boolean = false) {
        blocksLiveData.value = BlocksData(blocks, scrollToTop)
    }

    fun getBlocksLive(): LiveData<BlocksData> = blocksLiveData

    fun getSettings() = appState.getSettings()

    @CallSuper
    open fun onShowHideKeyboard(visible: Boolean) {
        log("onShowHideKeyboard :: {}", visible)
        showHideKeyboard.postValue(visible)
    }

    fun onHideKeyboard() {
        showHideKeyboard.postValue(false)
    }

    fun onClosed() {
        contextActionsState.requestFinish()
    }

}