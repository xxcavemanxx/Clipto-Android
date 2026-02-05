package clipto.presentation.common.fragment.attributed

import android.app.Application
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import clipto.action.SaveSettingsAction
import clipto.common.presentation.mvvm.RxViewModel
import clipto.common.presentation.mvvm.lifecycle.SingleLiveData
import clipto.config.IAppConfig
import clipto.domain.*
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.preview.link.LinkPreviewState
import clipto.repository.IRunesRepository
import clipto.store.StoreObject
import clipto.store.app.AppState
import clipto.store.filter.FilterDetailsState
import clipto.store.filter.FilterState
import clipto.store.main.MainState
import javax.inject.Inject

abstract class AttributedObjectViewModel<O : AttributedObject, S : AttributedObjectScreenState<O>>(app: Application) : RxViewModel(app) {

    @Inject
    lateinit var appState: AppState

    @Inject
    lateinit var mainState: MainState

    @Inject
    lateinit var filterState: FilterState

    @Inject
    lateinit var appConfig: IAppConfig

    @Inject
    lateinit var dialogState: DialogState

    @Inject
    lateinit var linkPreviewState: LinkPreviewState

    @Inject
    lateinit var saveSettingsAction: SaveSettingsAction

    @Inject
    lateinit var filterDetailsState: FilterDetailsState

    @Inject
    lateinit var runesRepository: IRunesRepository

    val screenStateLive by lazy { createScreenStateLive() }
    val navigatorProgress: MutableLiveData<Int> = SingleLiveData()
    val nextFocusLive: MutableLiveData<FocusMode> = SingleLiveData()
    val contentChangedLive: MutableLiveData<Boolean> = SingleLiveData()
    val autoSaveState = StoreObject<Pair<IRune, Boolean>>(id = "autoSaveState")
    val showPreviewLive by lazy { linkPreviewState.canShowPreview.getLiveData() }
    val hideActionsState = StoreObject<Boolean>("hide_actions_state")

    protected var showHideAdditionalAttributesCalled = false

    fun getSettings() = appState.getSettings()
    fun onNextFocus(nextFocus: FocusMode) = nextFocusLive.postValue(nextFocus)
    fun isContentChanged(): Boolean = contentChangedLive.value == true
    fun isPreviewMode(): Boolean = getScreenState().isPreviewMode()
    fun isEditMode(): Boolean = getScreenState().isEditMode()
    fun isViewMode(): Boolean = getScreenState().isViewMode()

    fun getTextSize() = mainState.getListConfig().textSize
    fun getTextFont() = mainState.getListConfig().textFont
    fun getTitle(): CharSequence? = getScreenState()?.title
    fun getFilterSnapshot() = mainState.filterSnapshot.requireValue()
    fun getViewMode(): ViewMode = getScreenState()?.viewMode ?: ViewMode.VIEW
    fun getFocusMode(): FocusMode = getScreenState()?.focusMode ?: FocusMode.NONE
    open fun isSettingsChanged(): Boolean = showHideAdditionalAttributesCalled
    fun onHideActions(hideActions: Boolean) = hideActionsState.setValue(hideActions)

    @CallSuper
    override fun doCreate() {
        super.doCreate()
        if (getSettings().autoSave) {
            onAutoSaveStateChanged(false)
        }
    }

    @CallSuper
    override fun doClear() {
        super.doClear()
        if (isSettingsChanged()) {
            saveSettingsAction.execute()
        }
    }

    fun onListConfigChanged(callback: (listConfig: ListConfig) -> Unit) {
        mainState.listConfig.getLiveChanges()
            .filter { it.isNotNull() }
            .map { it.value!! }
            .observeOn(getViewScheduler())
            .subscribeBy("onListConfigChanged", callback)
    }

    fun onShowHideAdditionalAttributes(showAdditionalAttrs: Boolean) {
        val settings = getSettings()
        showHideAdditionalAttributesCalled = true
        settings.noteShowAdditionalAttributes = showAdditionalAttrs
        onUpdateState()
    }

    fun getScreenState(): S? {
        return screenStateLive.value
    }

    protected fun onAutoSaveStateChanged(active: Boolean) {
        val value = autoSaveState.getValue()
        if (value == null) {
            runesRepository.getById(IRune.RUNE_AUTO_SAVE)
                .subscribeBy("onAutoSaveStateChanged") { autoSaveState.setValue(it to active) }
        } else {
            autoSaveState.setValue(value.first to active)
        }
    }

    protected fun onCancelAutoSave() {
        if (getSettings().autoSave) {
            onAutoSaveStateChanged(false)
        }
    }

    abstract fun onCreateBlocks(from: S?, blocksCallback:(blocks:List<BlockItem<Fragment>>) -> Unit)
    abstract fun createScreenStateLive(): MutableLiveData<S>
    abstract fun onUpdateState()

    abstract fun onInitNavigator(callback: (index: Int) -> Unit)
    abstract fun getNavigatorMaxValue(): Int
    abstract fun onNavigate(index: Int)
    abstract fun hasNavigator(): Boolean

}