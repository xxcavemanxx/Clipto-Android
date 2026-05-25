package clipto.analytics

import clipto.common.analytics.A

class FirebaseAnalyticsTracker(val tracker: Any?, val isEnabled: () -> Boolean) : A.ITracker {

    override fun event(event: String, params: Map<*, *>) {
        // No-op since Firebase Analytics is disabled
    }

    override fun error(event: String, th: Throwable, params: Map<*, *>) {
        // No-op since Firebase Crashlytics is disabled
    }

}
