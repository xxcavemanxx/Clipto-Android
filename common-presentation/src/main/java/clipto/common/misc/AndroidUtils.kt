package clipto.common.misc

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import clipto.common.logging.L
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.log
import kotlin.math.max
import kotlin.math.min


object AndroidUtils {

    private val nextId = AtomicInteger(Random().nextInt())
    private var lastConnectState: Boolean = true
    private var lastConnectCheck = 0L

    fun nextId(): Int = nextId.incrementAndGet()
    fun isPreQ(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    fun isPreMarshmallow(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.M

    fun isConnected(context: Context, checkInterval: Long = TimeUnit.MINUTES.toMillis(1)): Boolean {
        return try {
            if (System.currentTimeMillis() - lastConnectCheck >= checkInterval) {
                lastConnectState = isNetworkAvailable(context) && !isAirplaneModeOn(context)
                lastConnectCheck = System.currentTimeMillis()
            }
            return lastConnectState
        } catch (e: Exception) {
            true
        }
    }

    private fun isAirplaneModeOn(context: Context): Boolean {
        return Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                val cap = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
                return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                val networks: Array<Network> = cm.allNetworks
                for (n in networks) {
                    val nInfo: NetworkInfo? = cm.getNetworkInfo(n)
                    if (nInfo != null && nInfo.isConnected) return true
                }
            }
            else -> {
                val networks = cm.allNetworkInfo
                for (nInfo in networks) {
                    if (nInfo != null && nInfo.isConnected) return true
                }
            }
        }
        return false
    }

    fun getIpAddress(useIPv4: Boolean): String {
        try {
            val interfaces: List<NetworkInterface> = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs: List<InetAddress> = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        val isIPv4 = sAddr.indexOf(':') < 0
                        if (useIPv4) {
                            if (isIPv4) return sAddr
                        } else {
                            if (!isIPv4) {
                                val delim = sAddr.indexOf('%') // drop ip6 zone suffix
                                return if (delim < 0) sAddr.toUpperCase() else sAddr.substring(0, delim).toUpperCase()
                            }
                        }
                    }
                }
            }
        } catch (ignored: Exception) {
        } // for now eat exceptions
        return "0.0.0.0"
    }

    fun getDisplaySize(context: Context): Point {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)
        return Point(metrics.widthPixels, metrics.heightPixels)
    }

    fun getPreferredDisplaySize(context: Context): Point {
        val size = getDisplaySize(context)
        val width = size.x
        val height = size.y
        val newWidth = min(width, height)
        val newHeight = max(width, height)
        size.x = newWidth
        size.y = newHeight
        return size
    }

}