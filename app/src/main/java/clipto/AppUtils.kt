package clipto

import android.net.Uri
import android.os.Build
import clipto.analytics.Analytics
import clipto.common.extensions.toStackTrace
import clipto.common.misc.AesUtils
import clipto.common.misc.FormatUtils
import clipto.common.misc.IntentUtils
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.wb.clipboard.BuildConfig
import com.wb.clipboard.R

object AppUtils {

    private const val paramRef = "r"
    private const val paramCred = "c"
    private val key = String(arrayOf('r', 'R', 'f', 'v', '!', '5', '0', '@', 'P', 'x', 'Q', 'b', 'N', '5', '2', '$').toCharArray())

    fun getPlatform(): String {
        return "${Build.BRAND} ${Build.MODEL}"
    }

    fun getAuthTokenFromUrl(url: String?): String? {
        if (url == null) return null
        try {
            val regexp = "$paramCred=(.*)".toRegex()
            val hash = regexp.find(url)?.groupValues?.getOrNull(1)
            if (hash != null) {
                return AesUtils.decrypt(key, hash)
            }
        } catch (e: Exception) {
            Analytics.onError("error_get_auth_token_from_url", e)
        }
        return null
    }

    fun fromSnippetKitUri(uri: Uri, callback: (id: String) -> Unit) {
        val uriString = uri.toString()
        if (uriString.startsWith(BuildConfig.kitLink)) {
            runCatching {
                val id = uriString.substring(BuildConfig.kitLink.length + 1)
                callback.invoke(id)
            }
        }
    }

    fun fromReferralUri(uri: Uri, callback: (referralId: String?) -> Unit) {
        val uriString = uri.toString()
        if (uriString.startsWith(BuildConfig.referralLink)) {
            val regexp = "$paramRef=(.*)".toRegex()
            val linkHash = regexp.find(uriString)?.groupValues?.getOrNull(1)
            if (linkHash != null) {
                val appContext = AppContext.get()
                val appState = appContext.appState
                appContext.withInternet(
                        success = {
                            appState.onBackground {
                                try {
                                    appState.setLoadingState()
                                    val userId = AesUtils.decrypt(key, linkHash)
                                    callback.invoke(userId)
                                } catch (e: Exception) {
                                    Analytics.onError("error_user_from_uri", e)
                                } finally {
                                    appState.setLoadedState()
                                }
                            }
                        },
                        failed = { callback.invoke(null) }
                )
            } else {
                callback.invoke(null)
            }
        } else {
            callback.invoke(null)
        }
    }

    fun sendRequest(title: String? = null, info: String? = null, th: Throwable? = null) {
        val appContext = AppContext.get()
        val context = appContext.app
        val subject = context.getString(R.string.about_label_email_support_subject, context.getString(R.string.app_name))
        val settings = appContext.getSettings()
        val filters = appContext.getFilters()
        val filter = filters.findActive()
        val message = """
                        ${title ?: context.getString(R.string.about_label_email_support_message)}

                        ***** Info:
                        Locale: ${appContext.appState.getSelectedLocale()}
                        Time: ${FormatUtils.formatDateTime(System.currentTimeMillis())}
                        Build: ${BuildConfig.VERSION_NAME}
                        User: ${appContext.userState.user.getValue()?.firebaseId}
                        ${getInfoBody(info)}
                        ***** Device:
                        Board: ${Build.BOARD}
                        Brand: ${Build.BRAND}
                        Device: ${Build.DEVICE}
                        Model: ${Build.MODEL}
                        Product: ${Build.PRODUCT}
                        SDK: ${Build.VERSION.SDK_INT}
                        ID: ${Build.ID}
                        
                        ***** Settings:
                        auto_save: ${settings.autoSave}
                        sync_notes: ${!settings.disableSync}
                        emulate_copy_action: ${settings.emulateCopyAction}
                        support_advanced_clipboard: ${appContext.clipboardState.isClipboardSupportedBySomehow()}
                        track_clipboard: ${!settings.doNotTrackClipboardChanges}
                        universal_clipboard: ${settings.universalClipboard}
                        run_at_startup: ${settings.launchOnStartup}
                        notification_style: ${settings.notificationStyle.id}
                        cleanup_clipboard: ${settings.removeNotesFromClipboard}
                        hide_on_copy: ${settings.hideOnCopy}
                        remember_last_filter: ${settings.restoreFilterOnStart}
                        pin_starred_notes: ${filters.starred.pinStarredEnabled}
                        swipe_action_left: ${settings.swipeActionLeft.id}
                        swipe_action_right: ${settings.swipeActionRight.id}
                        double_click_exit: ${settings.doubleClickToExit}
                        double_click_delete: ${settings.doubleClickToDelete}
                        double_click_edit: ${settings.doubleClickToEdit}
                        theme: ${settings.theme}
                        text_font: ${settings.textFont}
                        text_size: ${settings.textSize}
                        text_lines: ${settings.textLines}
                        focusOnTitle: ${settings.focusOnTitle}
                        
                        filter_uid: ${filter.uid}
                        filter_type: ${filter.type.id}
                        filter_list_style: ${filter.listStyle.id}
                        filter_sort_order: ${filter.sortBy.id}
                        
                        ***** Config:
                        ${getRemoteConfig()}
                        
                        ${getErrorBody(th)}
                    """.trimIndent()
        IntentUtils.email(context, appContext.appConfig.getSupportEmail(), subject, message)
    }

    private fun getRemoteConfig(): String {
        if (AppContext.get().appConfig.canIncludeRemoteConfigInEmail()) {
            val sb = StringBuilder()
            FirebaseRemoteConfig.getInstance().all
                    .filter { it.key.startsWith("can_") && !it.key.contains("donate") && !it.key.contains("negative") }
                    .forEach {
                        sb.append(it.key)
                        sb.append(": ")
                        sb.append(it.value.asBoolean())
                        sb.appendLine()
                    }
            return sb.toString()
        }
        return ""
    }

    private fun getInfoBody(info: String?): String {
        if (info == null) {
            return ""
        }
        return """
            $info
            
        """.trimIndent()
    }

    private fun getErrorBody(th: Throwable?): String {
        if (th == null) {
            return ""
        }
        return """
            ***** Error:
            ${th.toStackTrace()}
        """.trimIndent()
    }

}