package clipto.common.extensions

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import clipto.common.R
import clipto.common.logging.L
import clipto.common.misc.AndroidUtils
import clipto.common.misc.Units
import java.lang.ref.WeakReference
import kotlin.math.max

fun Context.closeSystemDialogs() {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
        runCatching { sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) }
    }
}

fun Context.color(@ColorRes res: Int): Int = ContextCompat.getColor(this, res)

fun Context.calculateWidth(textSize: Int, padding: Int, vararg stringsRes: Int): Int {
    val paint = Paint()
    paint.textSize = textSize.toFloat()
    return stringsRes
        .map { getString(it) }
        .map { paint.measureText(it) }
        .maxOrNull()
        ?.let { Units.DP.toPx(it + padding) }
        ?.toInt()
        ?: ViewGroup.LayoutParams.WRAP_CONTENT
}

fun Context.safeIntent(intent: Intent, withToast: Boolean = true): Boolean {
    try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
            return true
        } else {
            if (withToast) {
                val message = R.string.error_activity_not_found
                showToast(message)
            }
        }
    } catch (th: Throwable) {
        if (withToast) {
            val message = getString(R.string.error_activity_not_found, intent.action)
            showToast(message)
        }
    }
    return false
}


fun Context.isContextDestroyed(): Boolean {
    if (this is FragmentActivity) {
        val fm = supportFragmentManager
        return isFinishing || isDestroyed || fm.isDestroyed || fm.isStateSaved
    }
    return false
}

private var toastRef: WeakReference<Toast>? = null
fun Context.showToast(messageRes: Int): Toast? = showToast(getString(messageRes))
fun Context.showToast(message: CharSequence): Toast? {
    if (!isContextDestroyed()) {
        return try {
            toastRef?.get()?.cancel()
            val toast = Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT)
            toastRef = WeakReference(toast)
            toast.show()
            toast
        } catch (th: Throwable) {
            null
        }
    }
    return null
}

fun Context.openInChromeTab(url: String): Boolean {
    val builder = CustomTabsIntent.Builder()
    val customTabsIntent = builder.build()
    return try {
        customTabsIntent.launchUrl(this, Uri.parse(url))
        true
    } catch (e: Exception) {
        false
    }
}

fun Context.getSpanCount(displaySize: Point = AndroidUtils.getDisplaySize(this)): Int {
    val spanCount = (displaySize.x / Units.DP.toPx(220f)).toInt()
    return max(2, spanCount)
}

fun Context.findActivity(): FragmentActivity? {
    return when {
        this is FragmentActivity -> this
        this is Fragment -> this.activity
        this is ContextThemeWrapper && baseContext is FragmentActivity -> {
            val ctx = baseContext
            if (ctx is FragmentActivity) {
                ctx
            } else {
                null
            }
        }
        this is ContextWrapper -> {
            val ctx = baseContext
            if (ctx is FragmentActivity) {
                ctx
            } else {
                null
            }
        }
        else -> null
    }
}

fun Context.withSafeFragmentManager(): FragmentManager? = findActivity()?.takeIf { !it.isContextDestroyed() }?.supportFragmentManager

fun Context.hasPermission(permission: String): Boolean {
    return try {
        PermissionChecker.checkSelfPermission(this, permission) == PermissionChecker.PERMISSION_GRANTED
    } catch (e: Exception) {
        false
    }
}

fun Context.canDrawOverlayViews(): Boolean {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        true
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        Settings.canDrawOverlays(this)
    } else {
        if (Settings.canDrawOverlays(this)) {
            return true
        }
        try {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val viewToAdd = View(this)
            val type =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                }
            val params = WindowManager.LayoutParams(
                type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            viewToAdd.layoutParams = params
                .also {
                    it.height = 1
                    it.width = 1
                }
            windowManager.addView(viewToAdd, params)
            windowManager.removeView(viewToAdd)
            return true
        } catch (e: Exception) {
            // ignore
        }
        false
    }
}

fun Context.isAccessibilityEnabled(type: Class<out AccessibilityService>): Boolean {
    val context = this
    var accessibilityEnabled = 0
    var enabled = false
    val service = context.packageName.toString() + "/" + type.canonicalName
    try {
        accessibilityEnabled = Settings.Secure.getInt(
            context.applicationContext.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED
        )
    } catch (e: Exception) {
        //
    }
    val stringSplitter = TextUtils.SimpleStringSplitter(':')
    if (accessibilityEnabled == 1) {
        val settingValue = Settings.Secure.getString(
            context.applicationContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (!settingValue.isNullOrBlank()) {
            stringSplitter.setString(settingValue)
            while (stringSplitter.hasNext()) {
                val accessibilityService: String = stringSplitter.next()
                if (accessibilityService.equals(service, ignoreCase = true)) {
                    enabled = true
                }
            }
        }
    }
    return enabled
}

fun Context.hasIntentAction(action: String): Boolean {
    return try {
        Intent(action).resolveActivity(packageManager) != null
    } catch (e: Exception) {
        false
    }
}

fun Context.canOpenDocument(): Boolean = hasIntentAction(Intent.ACTION_OPEN_DOCUMENT)
fun Context.canCreateDocument(): Boolean = hasIntentAction(Intent.ACTION_CREATE_DOCUMENT)
fun Context.canTakePhoto(): Boolean = hasIntentAction(MediaStore.ACTION_IMAGE_CAPTURE)
fun Context.canRecordVideo(): Boolean = hasIntentAction(MediaStore.ACTION_VIDEO_CAPTURE)

fun Context.takePersistableUriPermission(uri: Uri): Uri? {
    return try {
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, takeFlags)
        L.log(this, "FileRepository :: takePersistableUriPermission :: success :: {}", uri)
        uri
    } catch (th: Throwable) {
        L.log(this, "FileRepository :: takePersistableUriPermission :: error :: {} - {}", uri, th.message)
        uri
    }
}

fun Context.grantUriPermissions(uri: Uri): Uri? {
    return try {
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
        grantUriPermission(packageName, uri, takeFlags)
        L.log(this, "FileRepository :: grantUriPermissions :: success :: {}", uri)
        uri
    } catch (th: Throwable) {
        L.log(this, "FileRepository :: grantUriPermissions :: error :: {} - {}", uri, th.message)
        null
    }
}

fun Context.getPersistableUri(uri: String): Uri? = runCatching { grantUriPermissions(uri.toUri()) }.getOrNull()