package clipto.extensions

import android.content.ClipData
import android.content.Context
import android.os.Build
import clipto.dao.objectbox.model.ClipBox
import clipto.domain.Clip
import clipto.store.clipboard.ClipboardState

fun ClipData?.toClip(context: Context, withMetadata: Boolean = false): ClipBox? =
    try {
        val clipData = this
        if (this != null && itemCount > 0) {
            getItemAt(0).coerceToText(context)?.let {
                if (it.isNotBlank()) {
                    ClipBox().apply {
                        if (withMetadata) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                localId = clipData?.description?.extras?.getLong(ClipboardState.CLIP_ID) ?: 0L
                            }
                        }
                        text = it.toString()
                        isActive = true
                        tracked = true
                    }
                } else {
                    null
                }
            }
        } else {
            null
        }
    } catch (th: Throwable) {
        null
    }