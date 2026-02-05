package clipto.common.presentation.view

import android.graphics.drawable.Drawable
import android.view.View
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition

class DrawableViewTarget<T : View>(
    view: T,
    val onFailed: (drawable: Drawable?) -> Unit = {},
    val onReady: (drawable: Drawable) -> Unit
) : CustomViewTarget<T, Drawable>(view) {
    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) = onReady(resource)
    override fun onLoadFailed(errorDrawable: Drawable?) = onFailed(errorDrawable)
    override fun onResourceCleared(placeholder: Drawable?) = Unit
}