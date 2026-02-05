package clipto.common.logging

object L {

    private var logger = ILogger.EMPTY

    fun init(logger: ILogger) {
        L.logger = logger
    }

    fun log(instance: Any, message: String, vararg params: Any?) {
        try {
            logger.log(instance, message, *params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun log(instanceClass: Class<*>, message: String, vararg params: Any?) {
        try {
            logger.log(instanceClass, message, *params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun log(instance: Any, message: String, th: Throwable) {
        try {
            th.printStackTrace()
            logger.log(instance, message, th)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun log(instanceClass: Class<*>, message: String, th: Throwable) {
        try {
            th.printStackTrace()
            logger.log(instanceClass, message, th)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}