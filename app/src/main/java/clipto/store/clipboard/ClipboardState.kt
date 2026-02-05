package clipto.store.clipboard

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import clipto.analytics.Analytics
import clipto.common.extensions.canDrawOverlayViews
import clipto.common.extensions.hasPermission
import clipto.common.misc.AndroidUtils
import clipto.config.IAppConfig
import clipto.dao.objectbox.SettingsBoxDao
import clipto.dao.objectbox.model.ClipBox
import clipto.domain.Clip
import clipto.extensions.getId
import clipto.extensions.isNew
import clipto.extensions.toClip
import clipto.store.StoreObject
import clipto.store.StoreState
import clipto.store.clipboard.data.ClipboardNotification
import clipto.store.clipboard.data.HistoryStackItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardState @Inject constructor(
    appConfig: IAppConfig,
    val app: Application,
    private val settingsBoxDao: SettingsBoxDao
) : StoreState(appConfig) {

    val clipboardManager by lazy { app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    val hideOnCopy by lazy {
        StoreObject<Long>(
            id = "hide_on_copy",
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }
    val canTakeNoteFromClipboard by lazy {
        StoreObject(
            id = "can_take_note_from_clipboard",
            initialValue = !settingsBoxDao.get().pauseClipboard
        )
    }
    val notification by lazy {
        StoreObject<ClipboardNotification>(
            id = "notification"
        )
    }
    val historyStack by lazy {
        StoreObject<List<HistoryStackItem>>(
            id = "history_stack",
            initialValue = emptyList()
        )
    }
    val clipData by lazy {
        val storeObject = StoreObject<ClipData>(
            id = "clip_data",
            onChanged = { prev, value ->
                val newClip = value.toClip(app, withMetadata = true)
                val newClipValue =
                    when {
                        newClip == null -> null
                        newClip.isNew() -> newClip
                        else -> ClipBox().apply(newClip).apply { localId = 0L }
                    }
                if (value.isNew() || prev == null || newClip?.text.isNullOrBlank()) {
                    clip.setValue(newClipValue, true)
                }
                notification.setValue(ClipboardNotification(newClip))
            }
        )
        storeObject.setValue(getPrimaryClip())
        storeObject
    }
    val clip by lazy {
        val storeObject = StoreObject<Clip>(
            id = "clip",
            onChanged = { prevValue, newValue ->
                prevValue?.isChanged = true
                prevValue?.isActive = false
                newValue?.isChanged = true
                newValue?.isActive = true
            })
        storeObject
    }

    val supportAdvancedClipboardTracking by lazy { StoreObject<Boolean>(id = "support_advanced_clipboard_tracking") }

    fun requestHide() = hideOnCopy.setValue(System.currentTimeMillis())

    fun refreshNotification() = notification.setValue(notification.getValue(), true)

    fun refreshClipboard(internal: Boolean = false, primaryClip: ClipData? = getPrimaryClip()): ClipData? {
        var primaryClipRef = primaryClip
        if (internal && primaryClipRef.isNew()) {
            val newPrimaryClip = primaryClipRef.toClip(app)?.toClipData()
            if (newPrimaryClip != null) {
                primaryClipRef = newPrimaryClip
                clipboardManager.setPrimaryClip(newPrimaryClip)
            }
        }
        primaryClipRef?.let { clipData.setValue(it) }
        return primaryClipRef
    }

    fun clearClipboard() {
        try {
            val clip = ClipData.newPlainText(CLIP_DESCRIPTION, "")
            clipData.setValue(clip)
            clipboardManager.setPrimaryClip(clip)
        } catch (e: Exception) {
            runCatching {
                val clip = ClipData.newPlainText("", "")
                clipData.setValue(clip)
                clipboardManager.setPrimaryClip(clip)
            }
        }
    }

    fun clearAll() {
        historyStack.clearValue()
        clearClipboard()
        refreshNotification()
    }

    fun canEmulateCopyAction() = isClipboardActivated() && settingsBoxDao.get().emulateCopyAction && !isClipboardSupportedBySomehow()
    fun canTakeNoteFromClipboard() = isClipboardActivated() && canTakeNoteFromClipboard.requireValue()
    fun canRunOnStartup() = isClipboardActivated() && settingsBoxDao.get().launchOnStartup
    fun canRestoreClip() = false // clipData.getValue() == null

    fun isUniversalClipboardActivated() = isClipboardActivated() && settingsBoxDao.get().universalClipboard
    fun isClipboardSupportedViaForce() = settingsBoxDao.get().doNotDisplayAndroidQWarning
    fun isClipboardActivated() = !settingsBoxDao.get().doNotTrackClipboardChanges
    fun isClipboardSupportedNatively() = AndroidUtils.isPreQ()
    fun isClipboardSupportedByAdb(forceCheck: Boolean = false): Boolean {
        var value = supportAdvancedClipboardTracking.getValue()
        if (value == null || forceCheck) {
            value = app.hasPermission(android.Manifest.permission.READ_LOGS) && app.canDrawOverlayViews()
        }
        supportAdvancedClipboardTracking.setValue(value)
        return value == true
    }

    fun isClipboardSupportedBySomehow(forceCheck: Boolean = false): Boolean =
        isClipboardSupportedNatively() || isClipboardSupportedViaForce() || isClipboardSupportedByAdb(forceCheck)

    fun getPrimaryClip(): ClipData? {
        try {
            return if (clipboardManager.hasPrimaryClip()) {
                clipboardManager.primaryClip
            } else {
                null
            }
        } catch (th: Throwable) {
            Analytics.onError("error_get_primary_clip", th)
        }
        return null
    }

    companion object {
        const val CLIP_DESCRIPTION = "clipto.note.marked"
        const val CLIP_ID = "clipto.note.id"
    }

}

fun ClipData?.isNew(): Boolean = this != null && description?.label?.toString() != ClipboardState.CLIP_DESCRIPTION
fun Clip.toClipData(label: String = ClipboardState.CLIP_DESCRIPTION): ClipData = ClipData.newPlainText(label, text).apply {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val extras = PersistableBundle()
        extras.putLong(ClipboardState.CLIP_ID, getId())
        description.extras = extras
    }
}