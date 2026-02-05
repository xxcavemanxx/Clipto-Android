package clipto.common.extensions

import clipto.common.analytics.A
import java.io.InputStream

fun InputStream?.closeSilently() {
    try {
        this?.close()
    } catch (e: Exception) {
        A.error("closeSilently", e)
    }
}