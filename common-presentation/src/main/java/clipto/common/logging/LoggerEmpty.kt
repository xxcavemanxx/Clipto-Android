package clipto.common.logging

internal class LoggerEmpty : ILogger {
    override fun log(instance: Any, message: String, vararg params: Any?) = Unit
    override fun log(instanceClass: Class<*>, message: String, vararg params: Any?) = Unit
    override fun log(instance: Any, message: String, th: Throwable) = Unit
    override fun log(instanceClass: Class<*>, message: String, th: Throwable) = Unit
}