package clipto.common.presentation.view

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition

class BitmapViewTarget<T : View>(
    view: T,
    val onReady: (bitmap: Bitmap) -> Unit,
    val onFailed: (drawable: Drawable?) -> Unit = {}
) : CustomViewTarget<T, Bitmap>(view) {
    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) = onReady(resource)
    override fun onLoadFailed(errorDrawable: Drawable?) = onFailed(errorDrawable)
    override fun onResourceCleared(placeholder: Drawable?) = Unit
}