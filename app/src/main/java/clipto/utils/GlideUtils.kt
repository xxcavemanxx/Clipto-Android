package clipto.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import clipto.GlideApp
import clipto.common.misc.Units
import clipto.extensions.log
import clipto.presentation.preview.link.LinkPreview
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import com.google.firebase.storage.StorageReference
import jp.wasabeef.glide.transformations.CropSquareTransformation

object GlideUtils {

    fun clear(view: View) = GlideApp.with(view).clear(view)

    fun get(context: Context) = GlideApp.get(context)

    fun thumb(
        preview: LinkPreview?,
        view: ImageView,
        width: Int,
        height: Int,
        placeholder: Int = 0,
        onComplete: (drawable: Drawable?) -> Unit = {}
    ) {
        clear(view)
        if (placeholder == 0) {
            view.setImageDrawable(null)
        }
        if (preview == null || preview.isVideo() || preview.isAudio()) {
            if (preview?.imageUrl is StorageReference) {
                return
            }
        }
        log("preview :: thumb :: size :: {} - {}", width, height)
        val previewUrl = preview?.thumbUrl ?: preview?.imageUrl
        val transformation = createTransformation(preview ?: LinkPreview())
        val listener = RequestListenerCallback(onComplete)
        loadDrawable(view.context, previewUrl ?: "")
            .override(width, height)
            .let {
                val imageUrl = preview?.imageUrl
                if (imageUrl != null && imageUrl !== previewUrl) {
                    log("preview :: thumb :: errorUrl :: {}", imageUrl)
                    it.error(
                        loadDrawable(view.context, imageUrl)
                            .override(width, height)
                            .fitCenter()
                            .placeholder(placeholder)
                            .transform(transformation)
                            .listener(listener)
                    )
                } else {
                    it
                }
            }
            .fitCenter()
            .placeholder(placeholder)
            .transform(transformation)
            .listener(listener)
            .into(view)
    }

    fun preview(
        preview: LinkPreview?,
        view: ImageView,
        width: Int,
        height: Int,
        placeholder: Int,
        onComplete: (drawable: Drawable?) -> Unit = {}
    ) {
        clear(view)
        val listener = RequestListenerCallback(onComplete)
        val transformation = createTransformation(preview ?: LinkPreview())
        if (preview == null || preview.isVideo() || preview.isAudio()) {
            if (preview?.imageUrl is StorageReference) {
                GlideApp.with(view)
                    .load(placeholder)
                    .fitCenter()
                    .transform(transformation)
                    .listener(listener)
                    .into(view)
                return
            }
        }
        val previewUrl = preview?.imageUrl
        log("preview :: size :: {} - {} - {}", width, height, previewUrl)
        loadDrawable(view.context, previewUrl ?: "")
            .override(width, height)
            .let {
                val thumbUrl = preview?.thumbUrl
                if (thumbUrl != null && thumbUrl !== previewUrl) {
                    log("preview :: thumbUrl :: {}", thumbUrl)
                    it.thumbnail(
                        loadDrawable(view.context, thumbUrl)
                            .override(width, height)
                            .fitCenter()
                            .placeholder(placeholder)
                            .transform(transformation)
                            .listener(listener)
                    )
                } else {
                    it
                }
            }
            .fitCenter()
            .placeholder(placeholder)
            .transform(transformation)
            .listener(listener)
            .into(view)
    }

    fun loadDrawable(context: Context, ref: Any): RequestBuilder<Drawable> {
        return GlideApp.with(context)
            .load(ref)
            .signature(ObjectKey(ref))
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
    }

    fun loadBitmap(context: Context, ref: Any): RequestBuilder<Bitmap> {
        return GlideApp.with(context)
            .asBitmap()
            .load(ref)
            .signature(ObjectKey(ref))
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
    }

    fun createPlaceholder(): Drawable = ColorDrawable(Color.BLACK)

    fun createTransformation(linkPreview: LinkPreview): Transformation<Bitmap> {
        val transformation: Transformation<Bitmap> =
            if (linkPreview.withSquarePreview) {
                MultiTransformation(
                    CropSquareTransformation(),
                    RoundedCorners(Units.DP.toPx(linkPreview.cornerRadiusInDp).toInt())
                )
            } else {
                RoundedCorners(Units.DP.toPx(linkPreview.cornerRadiusInDp).toInt())
            }
        return transformation
    }

    class RequestListenerCallback(val onComplete: (drawable: Drawable?) -> Unit = {}) : RequestListener<Drawable> {
        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
            log("GlideUtils :: onLoadFailed :: {} - {}", model, e)
            return false
        }

        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
            log("GlideUtils :: onResourceReady :: {}", model)
            onComplete(resource)
            return false
        }
    }

}