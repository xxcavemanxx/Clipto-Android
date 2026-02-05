package clipto.store.folder

import androidx.fragment.app.FragmentActivity
import clipto.config.IAppConfig
import clipto.domain.FileRef
import clipto.presentation.folder.details.FolderDetailsFragment
import clipto.store.StoreObject
import clipto.store.StoreState
import clipto.store.app.AppState
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
class FolderState @Inject constructor(
    appConfig: IAppConfig,
    val appState: AppState
) : StoreState(appConfig) {

    val requestsQueue = mutableMapOf<Int, FolderRequest>()

    private val requestOpenFolder by lazy {
        StoreObject<FolderRequest>(
            id = "request_open_folder",
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER,
            onChanged = { _, next ->
                next?.let { requestsQueue[it.id] = it }
            }
        )
    }

    val requestUpdateFolder by lazy {
        StoreObject<FileRef>(id = "request_update_folder")
    }

    fun requestUpdateFolder(folder: FileRef) = requestUpdateFolder.setValue(folder, force = true)

    fun requestNewFolder() {
        requestOpenFolder.setValue(FolderRequest())
    }

    fun requestOpenFolder(fileRef: FileRef) {
        requestOpenFolder(FolderRequest(folderRef = fileRef))
    }

    fun requestOpenFolder(request: FolderRequest) {
        requestOpenFolder.setValue(request)
    }

    fun bind(activity: FragmentActivity) {
        requestOpenFolder.getLiveData().observe(activity) {
            FolderDetailsFragment.show(activity, it)
        }
    }

}