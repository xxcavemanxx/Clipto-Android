package clipto.common.misc

import java.io.Closeable
import java.io.IOException

object IoUtils {
    fun close(vararg streams: Closeable?) {
        for (stream in streams) {
            if (stream != null) {
                try {
                    stream.close()
                } catch (e: IOException) {
                    //
                }
            }
        }
    }
}