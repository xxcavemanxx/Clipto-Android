package clipto.common.misc

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import clipto.common.R
import clipto.common.analytics.A
import clipto.common.logging.L
import java.lang.ref.SoftReference
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

object ActivityResultUtils {

    private val seed = AtomicInteger()

    private val onActivityResultCallbacks = mutableMapOf<Int, SoftReference<(resultCode: Int, data: Intent?) -> Unit>>()
    private val onRequestPermissionsResults = mutableMapOf<Int, SoftReference<(grantResults: Map<String, Boolean>) -> Unit>>()

    fun nextId() = seed.incrementAndGet()

    fun onActivityResult(
            activity: FragmentActivity,
            intent: Intent,
            callback: (resultCode: Int, data: Intent?) -> Unit) {
        if (intent.resolveActivity(activity.packageManager) == null) {
            val error = activity.getString(R.string.error_activity_not_found, intent.action)
            val toast = Toast.makeText(activity, error, Toast.LENGTH_SHORT)
            A.event("error_intent_not_found", "action", intent.action)
            toast.show()
            return
        }
        val manager = activity.supportFragmentManager
        var fragment = manager.findFragmentByTag(ActivityResultFragment.TAG) as ActivityResultFragment?
        if (fragment == null) {
            fragment = ActivityResultFragment()
            L.log(this, "attach new fragment to {}", activity)
            manager.beginTransaction().add(fragment, ActivityResultFragment.TAG).commitNow()
        }

        if (fragment.arguments == null) {
            fragment.arguments = Bundle()
        }
        val requestCode = nextId()
        fragment.requireArguments().apply { putInt(ActivityResultFragment.ATTR_REQUEST_CODE, requestCode) }
        L.log(this, "start new request \nactivity={}\nrequestCode={}", activity::class.java.simpleName, requestCode)
        onActivityResultCallbacks[requestCode] = SoftReference(callback)
        fragment.startActivityForResult(intent, requestCode)
    }

    internal fun onActivityResult(
            fragment: ActivityResultFragment,
            requestCode: Int,
            resultCode: Int,
            data: Intent?) {
        val fragmentRequestCode = fragment.arguments?.getInt(ActivityResultFragment.ATTR_REQUEST_CODE)
        L.log(this, "check result :: \nfragment={}\nrequestCode={}\nresultCode={}\ndata={}\nfragment.requestCode={}",
                fragment,
                requestCode,
                resultCode,
                data,
                fragmentRequestCode)
        val callback = onActivityResultCallbacks.remove(requestCode)
        if (requestCode == fragmentRequestCode) {
            L.log(this, "handled result {} - {}", resultCode, callback?.get())
            callback?.get()?.invoke(resultCode, data)
        }
    }

    fun onRequestPermissionsResult(
            activity: FragmentActivity,
            vararg permissions: String,
            callback: (grantResults: Map<String, Boolean>) -> Unit) {
        val granted = PermissionChecker.PERMISSION_GRANTED
        val permissionsToGrant = permissions
                .filter {
                    val notGranted = PermissionChecker.checkSelfPermission(activity, it) != granted
                    L.log(this, "check if granted: {} -> {}", it, !notGranted)
                    notGranted
                }
                .toTypedArray()
        if (permissionsToGrant.isNotEmpty()) {
            val manager = activity.supportFragmentManager
            var fragment = manager.findFragmentByTag(ActivityResultFragment.TAG) as ActivityResultFragment?
            if (fragment == null) {
                fragment = ActivityResultFragment()
                L.log(this, "attach new fragment to {}", activity)
                manager.beginTransaction().add(fragment, ActivityResultFragment.TAG).commitNow()
            }
            val requestCode = seed.incrementAndGet()
            if (fragment.arguments == null) {
                fragment.arguments = Bundle()
            }
            fragment.requireArguments().apply { putInt(ActivityResultFragment.ATTR_REQUEST_CODE, requestCode) }
            L.log(this, "start new request {} -> {}", activity::class.java.simpleName, requestCode)
            onRequestPermissionsResults[requestCode] = SoftReference(callback)
            fragment.requestPermissions(permissionsToGrant, requestCode)
        } else {
            L.log(this, "permissions have already been granted: {}", Arrays.toString(permissions))
            callback.invoke(emptyMap())
        }
    }

    internal fun onRequestPermissionsResult(fragment: ActivityResultFragment, requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        val fragmentRequestCode = fragment.arguments?.getInt(ActivityResultFragment.ATTR_REQUEST_CODE)
        val callback = onRequestPermissionsResults.remove(requestCode)
        if (requestCode == fragmentRequestCode) {
            val granted = PermissionChecker.PERMISSION_GRANTED
            val results = permissions.mapIndexed { index, value -> Pair(value, grantResults[index] == granted) }.toMap()
            L.log(this, "granted: {}", results)
            results.filter { !it.value }.forEach { A.event("Permission is not granted", "permission", it.key) }
            callback?.get()?.invoke(results)
        }
    }

}

internal class ActivityResultFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        ActivityResultUtils.onActivityResult(this, requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        ActivityResultUtils.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    companion object {
        const val TAG = "__ActivityResultFragment__"
        const val ATTR_REQUEST_CODE = "__attr_request_code__"
    }

}