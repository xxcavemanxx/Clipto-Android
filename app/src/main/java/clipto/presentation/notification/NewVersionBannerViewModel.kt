package clipto.presentation.notification

import android.app.Application
import clipto.analytics.Analytics
import clipto.common.misc.IntentUtils
import clipto.common.presentation.mvvm.ViewModel
import clipto.config.IAppConfig
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import com.wb.clipboard.BuildConfig
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NewVersionBannerViewModel @Inject constructor(
        app: Application,
        private val appConfig: IAppConfig,
        private val dialogState: DialogState
) : ViewModel(app) {

    val latestVersion = appConfig.getLatestVersion()
    private val latestVersionChanges = appConfig.getLatestVersionChanges().trim().replace(";", ";\n\n")

    fun onClicked() {
        dialogState.showConfirm(ConfirmDialogData(
                iconRes = R.drawable.ic_new_releases,
                title = latestVersion,
                description = latestVersionChanges,
                confirmActionTextRes = R.string.desktop_update_action_download,
                onConfirmed = {
                    IntentUtils.open(app, BuildConfig.appDownloadLink)
                    Analytics.onDownloadNow()
                    dismiss()
                },
                cancelActionTextRes = R.string.desktop_update_action_later,
                onCanceled = {
                    appConfig.remindAboutNewVersionLater(latestVersion)
                    Analytics.onDownloadLater()
                    dismiss()
                }
        ))
    }

}