package clipto.analytics

import android.os.Bundle
import clipto.common.analytics.A
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicBoolean

class FirebaseAnalyticsTracker(val tracker: FirebaseAnalytics, val isEnabled: () -> Boolean) : A.ITracker {

    private val enabledAtomic = AtomicBoolean(isEnabled.invoke())

    init {
        tracker.setAnalyticsCollectionEnabled(enabledAtomic.get())
    }

    private fun isAnalyticsEnabled(): Boolean {
        val enabled = isEnabled.invoke()
        try {
            if (enabled != enabledAtomic.get()) {
                if (enabledAtomic.compareAndSet(!enabled, enabled)) {
                    tracker.setAnalyticsCollectionEnabled(enabled)
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return enabled
    }

    override fun event(event: String, params: Map<*, *>) {
        try {
            if (isAnalyticsEnabled()) {
                tracker.logEvent(event, Bundle().also { bundle ->
                    params.forEach { bundle.putString(it.key.toString(), it.value?.toString()) }
                })
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun error(event: String, th: Throwable, params: Map<*, *>) {
        try {
            if (isAnalyticsEnabled()) {
                FirebaseCrashlytics.getInstance().recordException(th)
                tracker.logEvent(event, Bundle().also { bundle ->
                    params.forEach { bundle.putString(it.key.toString(), it.value?.toString()) }
                    val writer = StringWriter()
                    th.printStackTrace(PrintWriter(writer))
                    bundle.putString("unexpected_error", writer.toString())
                })
            }
        } catch (e: Exception) {
            // ignore
        }
    }

}
