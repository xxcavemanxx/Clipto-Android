package clipto.common.extensions

import android.graphics.Bitmap

fun Bitmap.use(block: (bitmap: Bitmap) -> Unit) {
    try {
        block.invoke(this)
    } finally {
        runCatching { recycle() }
    }
}