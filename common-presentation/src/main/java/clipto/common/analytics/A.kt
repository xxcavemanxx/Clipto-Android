package clipto.common.analytics

import clipto.common.logging.L

object A {

    private var tracker: ITracker? = null

    fun init(tracker: ITracker?): A {
        A.tracker = tracker
        return this
    }

    fun event(event: String, vararg keyValues: Any?) {
        try {
            L.log(this, "track event: event={}, params={}, tracker={}", event, keyValues, tracker)
            if (tracker != null) {
                val params = map(*keyValues)
                tracker!!.event(event, params)
            }
        } catch (e: Exception) {
            L.log(this, "error", e)
        }
    }

    fun error(event: String, th: Throwable, vararg keyValues: Any) {
        try {
            L.log(this, "track error: event={}, error={}, params={}, tracker: {}", event, th, keyValues, tracker)
            if (tracker != null) {
                val params = map(*keyValues)
                tracker!!.error(event, th, params)
            } else {
                th.printStackTrace()
            }
        } catch (e: Exception) {
            L.log(this, "error", e)
        }
    }

    private fun map(vararg values: Any?): Map<*, *> {
        if (values.isEmpty()) {
            return emptyMap<String, String>()
        }
        if (values.size % 2 != 0) {
            throw RuntimeException("Usage - (key, value, key, value, ...)")
        }
        val result = HashMap<String, String>(values.size / 2)
        var i = 0
        while (i < values.size) {
            result[values[i].toString()] = values[i + 1].toString()
            i += 2
        }
        return result
    }

    interface ITracker {

        fun event(event: String, params: Map<*, *>)

        fun error(event: String, th: Throwable, params: Map<*, *>)

    }
}
