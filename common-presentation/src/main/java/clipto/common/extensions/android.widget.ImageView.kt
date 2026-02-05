package clipto.common.extensions

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target

fun ImageView.load(path: CharSequence?, placeholder: Drawable? = null) {
    load(path, {}, {}, placeholder)
}

fun ImageView.load(path: CharSequence?, onSuccess: () -> Unit, onError: () -> Unit, placeholder: Drawable? = null) {
    val requestManager = Glide.with(this)
    requestManager
            .load(path)
            .apply(RequestOptions().placeholder(placeholder))
            .addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                    onError.invoke()
                    return false
                }

                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    setVisible(true)
                    onSuccess.invoke()
                    return false
                }
            })
            .into(this)
}