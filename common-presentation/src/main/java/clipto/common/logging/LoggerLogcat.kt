package clipto.common.logging

import android.util.Log

internal class LoggerLogcat : ILogger {

    override fun log(instance: Any, message: String, vararg params: Any?) {
        log(instance.javaClass, message, *params)
    }

    override fun log(instanceClass: Class<*>, message: String, vararg params: Any?) {
        val tag = instanceClass.simpleName
        Log.v(tag, String.format(message.replace("{}", "%s"), *params))
    }

    override fun log(instance: Any, message: String, th: Throwable) {
        log(instance.javaClass, message, th)
    }

    override fun log(instanceClass: Class<*>, message: String, th: Throwable) {
        val tag = instanceClass.simpleName
        Log.v(tag, message, th)
    }
}