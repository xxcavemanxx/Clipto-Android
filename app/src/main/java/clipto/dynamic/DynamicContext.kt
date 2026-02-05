package clipto.dynamic

import android.app.Application
import clipto.config.IAppConfig
import clipto.presentation.common.dialog.DialogState
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
class DynamicContext @Inject constructor(
        val dialogState: DialogState,
        val appConfig: IAppConfig,
        val app: Application
)