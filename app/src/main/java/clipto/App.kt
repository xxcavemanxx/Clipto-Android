package clipto

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import clipto.action.ActionContext
import clipto.action.InitAppAction
import clipto.store.clipboard.IClipboardStateManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var appContext: AppContext

    @Inject
    lateinit var lockManager: AppLockManager

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var linkPreviewManager: AppLinkPreviewManager

    @Inject
    lateinit var clipboardStateManager: IClipboardStateManager

    @Inject
    lateinit var initAppAction: InitAppAction

    override fun onCreate() {
        super.onCreate()
        initAppAction.execute(ActionContext.DETATCHED)
    }

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

}