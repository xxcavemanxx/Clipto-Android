package clipto.common.extensions

import android.content.res.Resources
import clipto.common.misc.FormatUtils
import java.util.*

fun Resources.setLocale(locale: Locale) {
    if (Locale.getDefault() != locale) {
        runCatching { Locale.setDefault(locale) }
    }
    val systemConfiguration = Resources.getSystem().configuration
    if (systemConfiguration.locale != locale) {
        runCatching {
            systemConfiguration.setLocale(locale)
            Resources.getSystem().updateConfiguration(systemConfiguration, displayMetrics)
            FormatUtils.clearCache()
        }
    }
    if (configuration.locale != locale) {
        runCatching {
            configuration.setLocale(locale)
            updateConfiguration(configuration, displayMetrics)
        }
    }
}