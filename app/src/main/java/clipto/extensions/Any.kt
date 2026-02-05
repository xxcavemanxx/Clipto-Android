package clipto.extensions

import clipto.common.logging.L

fun Any.log(message: String, vararg args: Any?) = L.log(this, "${Thread.currentThread()} $message", *args)