package clipto.common.extensions

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import clipto.common.analytics.A
import clipto.common.misc.ActivityResultUtils
import clipto.common.misc.FormatUtils
import java.io.File

fun FragmentActivity.withResult(intent: Intent, callback: (resultCode: Int, data: Intent?) -> Unit) {
    ActivityResultUtils.onActivityResult(this, intent, callback)
}

fun FragmentActivity.withPermissions(vararg permissions: String, callback: (grantResults: Map<String, Boolean>) -> Unit) {
    ActivityResultUtils.onRequestPermissionsResult(this, *permissions) { callback.invoke(it) }
}

fun FragmentActivity.withFile(mediaType: String = "*/*", persistable: Boolean = true, callback: (uri: Uri) -> Unit) {
    val request = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        if (persistable) {
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        addCategory(Intent.CATEGORY_OPENABLE)
        type = mediaType
    }
    withResult(request) { _, intent ->
        intent?.data?.let { uri ->
            if (persistable) {
                takePersistableUriPermission(uri)
            }
            callback.invoke(uri)
        }
    }
}

fun FragmentActivity.withNewFile(name: String?, persistable: Boolean = true, callback: (uri: Uri) -> Unit) {
    val request = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        if (persistable) {
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        addCategory(Intent.CATEGORY_OPENABLE)
        putExtra(Intent.EXTRA_TITLE, name)
        type = "*/*"
    }
    withResult(request) { _, intent ->
        intent?.data?.let { uri ->
            if (persistable) {
                takePersistableUriPermission(uri)
            }
            callback.invoke(uri)
        }
    }
}

fun FragmentActivity.withPhoto(callback: (uri: Uri) -> Unit) {
    withPermissions(Manifest.permission.CAMERA) {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val dir = getExternalFilesDir(Environment.DIRECTORY_DCIM)!!
            val file = File(dir, "${FormatUtils.buildUniqueName("PHOTO")}.jpg")
            val uri = FileProvider.getUriForFile(this, "${packageName}.file_provider", file)
            grantUriPermissions(uri)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            withResult(intent) { resultCode, _ ->
                if (resultCode == Activity.RESULT_OK) {
                    callback.invoke(uri)
                }
            }
        } catch (th: Throwable) {
            A.error("error_take_photo", th)
        }
    }
}

fun FragmentActivity.withVideoRecord(callback: (uri: Uri) -> Unit) {
    withPermissions(Manifest.permission.CAMERA) {
        try {
            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            val dir = getExternalFilesDir(Environment.DIRECTORY_DCIM)!!
            val file = File(dir, "${FormatUtils.buildUniqueName("RECORD")}.mp4")
            val uri = FileProvider.getUriForFile(this, "${packageName}.file_provider", file)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            withResult(intent) { resultCode, _ ->
                if (resultCode == Activity.RESULT_OK) {
                    grantUriPermissions(uri)
                    callback.invoke(uri)
                }
            }
        } catch (th: Throwable) {
            A.error("error_take_video", th)
        }
    }
}