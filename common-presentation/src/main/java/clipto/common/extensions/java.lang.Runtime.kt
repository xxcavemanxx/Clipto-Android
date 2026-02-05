package clipto.common.extensions

import java.io.BufferedReader
import java.io.InputStreamReader

fun Runtime.getProp(name: String): String? {
    return try {
        val process = exec("getprop $name")
        val osRes = BufferedReader(InputStreamReader(process.inputStream))
        return osRes.use { it.readLine() }.toNullIfEmpty(trim = true)
    } catch (e: Exception) {
        null
    }
}