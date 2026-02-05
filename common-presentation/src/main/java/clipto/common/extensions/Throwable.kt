package clipto.common.extensions

import java.io.PrintWriter
import java.io.StringWriter

fun Throwable.toStackTrace(): String {
    val writer = StringWriter()
    printStackTrace(PrintWriter(writer))
    return writer.toString()
}