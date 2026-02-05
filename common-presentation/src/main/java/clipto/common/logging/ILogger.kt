package clipto.common.logging

interface ILogger {

    fun log(instance: Any, message: String, vararg params: Any?)
    fun log(instanceClass: Class<*>, message: String, vararg params: Any?)

    fun log(instanceClass: Class<*>, message: String, th: Throwable)
    fun log(instance: Any, message: String, th: Throwable)

    companion object {
        val EMPTY: ILogger = LoggerEmpty()
        val LOGCAT: ILogger = LoggerLogcat()
    }
}