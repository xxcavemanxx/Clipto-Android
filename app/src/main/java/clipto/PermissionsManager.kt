package clipto

import android.accessibilityservice.AccessibilityService
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.fragment.app.FragmentActivity
import clipto.common.extensions.*
import clipto.store.app.AppState
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class PermissionsManager @Inject constructor(
        private val app: Application,
        private val appState: AppState
) {

    fun canDrawOverlayViews() = app.canDrawOverlayViews()
    fun canReadLogs(): Boolean = app.hasPermission(android.Manifest.permission.READ_LOGS)
    fun isAccessibilityEnabled(type: Class<out AccessibilityService>) = app.isAccessibilityEnabled(type)

    fun requestOverlayViews(activity: FragmentActivity) {
        val uri = Uri.parse("package:${app.packageName}")
        val request = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
        }
        activity.withResult(request) { _, _ -> appState.refreshSettings() }
    }

    fun requestAccessibility(activity: FragmentActivity) {
        val request = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        activity.withResult(request) { _, _ -> appState.refreshSettings() }
    }

    fun requestAutoStart(activity: FragmentActivity) {
        runCatching {
            val intent = Intent()
            val manufacturer = Build.MANUFACTURER
            if ("xiaomi".equals(manufacturer, ignoreCase = true)) {
                intent.component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            } else if ("oppo".equals(manufacturer, ignoreCase = true)) {
                intent.component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
            } else if ("vivo".equals(manufacturer, ignoreCase = true)) {
                intent.component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
            } else if ("Letv".equals(manufacturer, ignoreCase = true)) {
                intent.component = ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")
            } else if ("Honor".equals(manufacturer, ignoreCase = true)) {
                intent.component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
            }
            val list: List<ResolveInfo> = activity.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (list.isNotEmpty()) {
                activity.safeIntent(intent)
            }
        }
    }

}