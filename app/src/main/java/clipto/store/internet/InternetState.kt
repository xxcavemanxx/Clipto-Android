package clipto.store.internet

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.widget.Toast
import clipto.common.R
import clipto.common.misc.AndroidUtils
import clipto.config.IAppConfig
import clipto.store.StoreObject
import clipto.store.StoreState
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class InternetState @Inject constructor(
        val app: Application,
        appConfig: IAppConfig
) : StoreState(appConfig) {

    val liveConnect by lazy {
        val isConnected = isConnected()
        val storeObject = StoreObject("live_connect", initialValue = isConnected)
        app.registerReceiver(
                NetworkBroadcastReceiver(storeObject),
                IntentFilter().apply {
                    addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                    addAction(ConnectivityManager.CONNECTIVITY_ACTION)
                }
        )
        storeObject
    }

    fun isConnected(): Boolean = AndroidUtils.isConnected(app, appConfig.getInternetStateCheckInterval())

    fun withInternet(success: () -> Unit, failed: () -> Unit = {}) {
        if (isConnected()) {
            success.invoke()
        } else {
            Toast.makeText(app, app.getText(R.string.error_internet_required), Toast.LENGTH_SHORT).show()
            failed.invoke()
        }
    }

    inner class NetworkBroadcastReceiver(
            private val liveConnect: StoreObject<Boolean>
    ) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            log("onReceive: {}", intent)
            if (intent == null) {
                return
            }
            when (intent.action) {
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                    if (intent.hasExtra("state")) {
                        val airplaneMode = intent.getBooleanExtra("state", false)
                        log("airplaneMode={}", airplaneMode)
                        if (airplaneMode) {
                            liveConnect.setValue(!airplaneMode)
                        }
                    }
                }
                ConnectivityManager.CONNECTIVITY_ACTION -> {
                    val isConnected = isConnected()
                    log("connected={}", liveConnect)
                    liveConnect.setValue(isConnected)
                }
            }
        }
    }

}