package clipto.presentation.contextactions

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import clipto.action.intent.IntentActionFactory
import clipto.common.presentation.mvvm.RxViewModel
import clipto.common.presentation.mvvm.lifecycle.SingleLiveData
import clipto.dynamic.presentation.field.DynamicFieldState
import clipto.presentation.common.dialog.DialogState
import clipto.store.filter.FilterDetailsState
import clipto.store.folder.FolderState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ContextActionsViewModel @Inject constructor(
    app: Application,
    val dialogState: DialogState,
    val folderState: FolderState,
    val dynamicFieldState: DynamicFieldState,
    val filterDetailsState: FilterDetailsState,
    val contextActionsState: ContextActionsState,
    private val intentActionFactory: IntentActionFactory
) : RxViewModel(app) {

    val intentLive: MutableLiveData<Intent> = SingleLiveData()

    val finishRequestLive = contextActionsState.finishRequest.getLiveData()

    val actions = mutableMapOf<String, (fragment: ContextActionsFragment) -> Unit>()

    fun onIntent(intent: Intent?) {
        intentLive.postValue(intent)
    }

    fun onComplete() {
        contextActionsState.requestFinish()
    }

    fun onHandleIntent(context: Context, intent: Intent, callback: () -> Unit) {
        intentActionFactory.handle(context, intent, callback)
    }

}