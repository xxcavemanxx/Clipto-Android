package clipto.presentation.preview.link

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.*
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.NonNull
import androidx.annotation.Px
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import clipto.AppContext
import clipto.common.R
import clipto.common.extensions.cutString
import clipto.common.extensions.findActivity
import clipto.common.extensions.isContextDestroyed
import clipto.common.misc.Units
import clipto.common.presentation.text.AnimatableDrawableCallback
import clipto.common.presentation.text.BetterLinkMovementMethod
import clipto.common.presentation.text.MyClickableSpan
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.extensions.getActionIconColorHighlight
import clipto.extensions.getTextColorSecondary
import clipto.extensions.log
import clipto.utils.GlideUtils
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import java.lang.ref.WeakReference

class LinkPreviewSpannable(
    val withTextView: TextView,
    val withUrl: CharSequence,
    val withWidth: Int,
    var withHeight: Int,
    val withPreviewCache: LinkPreviewCache? = null,
    val withForcePreviews: Boolean = false,
    val withLinkPreviewState: LinkPreviewState,
    val withLinkPreviewFetcher: ILinkPreviewFetcher,
    val context: Context = withTextView.context.findActivity() ?: withTextView.context
) : SpannableStringBuilder() {

    var canPreviewLinks = withForcePreviews || withLinkPreviewState.canPreviewLinks()
    val urlString = withUrl.toString()

    private val colorAccent = context.getActionIconColorHighlight()
    private val colorDisabled = context.getTextColorSecondary()
    private var withShowHideAction = false
    private var loadingText = SimpleSpanBuilder()
        .append("\n")
        .append("\n", RelativeSizeSpan(0.2f))
        .append(
            context.getString(R.string.link_preview_label),
            MyQuoteSpan(colorAccent),
            RelativeSizeSpan(0.8f)
        )
        .build()
    private var replaceIn: SpannableStringBuilder? = null
    private var replaceInSpan: URLSpan? = null

    fun defaultState() {
        replace(withUrl)
        tryToReplaceIn(this)
    }

    fun previewState() {
        replace(withUrl)
        append(loadingText)
        tryToReplaceIn(this)
    }

    fun loadPreview(replaceIn: SpannableStringBuilder, replaceInSpan: URLSpan) {
        this.replaceInSpan = replaceInSpan
        this.replaceIn = replaceIn
        loadPreview()
    }

    fun loadPreview() {
        withLinkPreviewFetcher.fetchPreview(this) { linkPreview ->
            withShowHideAction = !withForcePreviews && linkPreview.isValid()
            val isPreviewHidden = !canPreviewLinks
            val quoteColor = if (isPreviewHidden) colorDisabled else colorAccent
            val content = SimpleSpanBuilder()
            content.append(withUrl, ForegroundColorSpan(colorAccent))
            content.append("\n")
            linkPreview.getSiteName()?.takeIf { it.isNotBlank() }?.let { siteName ->
                content.append(
                    "\n",
                    RelativeSizeSpan(0.2f)
                )
                content.append(
                    siteName,
                    ForegroundColorSpan(quoteColor),
                    StyleSpan(Typeface.BOLD),
                    RelativeSizeSpan(0.8f)
                )
                if (withShowHideAction) {
                    content.append(
                        "\n",
                        SimpleSpanBuilder.LineOverlapSpan()
                    )
                    val actionLabel =
                        if (canPreviewLinks) {
                            context.getString(R.string.link_preview_hide)
                        } else {
                            context.getString(R.string.link_preview_show)
                        }
                    content.append(
                        actionLabel,
                        AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE),
                        ShowHidePreviewSpan(!canPreviewLinks),
                        ForegroundColorSpan(colorDisabled),
                        RelativeSizeSpan(0.8f)
                    )
                }

                content.append(
                    "\n",
                    RelativeSizeSpan(0.2f)
                )
                if (withShowHideAction) {
                    content.append(
                        "\n",
                        RelativeSizeSpan(0.2f),
                        AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE)
                    )
                }
            }
            if (!isPreviewHidden) {
                linkPreview.title?.takeIf { it.isNotBlank() }?.let { title ->
                    content.append("\n", RelativeSizeSpan(0.2f))
                    content.append(title, StyleSpan(Typeface.BOLD), RelativeSizeSpan(0.8f))
                }
                linkPreview.description?.takeIf { it.isNotBlank() }?.cutString()?.let { description ->
                    content.append("\n")
                    content.append(description, RelativeSizeSpan(0.8f))
                }
            }
            val imageUrl = if (linkPreview.imageUrl == null && linkPreview.isPreviewable()) linkPreview.url else linkPreview.imageUrl
            if (imageUrl != null && !isPreviewHidden) {
                if (!context.isContextDestroyed()) {
                    LinkPreviewLoader(imageUrl, linkPreview, content).load()
                }
            } else {
                val span = content.build()
                span.setSpan(MyQuoteSpan(quoteColor), withUrl.length + 2, span.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                tryToReplaceIn(span)
            }
        }
    }

    private fun tryToReplaceIn(spannable: Spannable) {
        val textViewText = withTextView.text
        val replaceInRef = replaceIn
        if (replaceInRef != null) {
            if (textViewText === replaceInRef) {
                if (withTextView.movementMethod !is BetterLinkMovementMethod) {
                    withTextView.movementMethod = BetterLinkMovementMethod.getInstance()
                }
                runCatching {
                    val startLength = withUrl.length
                    val replaceInEnd = replaceInRef.getSpanEnd(replaceInSpan)
                    val loadingEndIndex = replaceInEnd + loadingText.length
                    val isLoading = replaceInRef.startsWith(loadingText, replaceInEnd)
                    log("tryToReplaceIn :: [{}, {}]", replaceInEnd, loadingEndIndex)
                    if (replaceInEnd != -1) {
                        if (isLoading) {
                            replaceInRef.replace(replaceInEnd, loadingEndIndex, spannable.subSequence(startLength, spannable.length))
                        } else if (spannable.length > startLength) {
                            replaceInRef.insert(replaceInEnd, spannable.subSequence(startLength, spannable.length))
                        }
                    }
                }
            }
        } else {
            replace(spannable)
        }
    }

    private fun replace(text: CharSequence?) {
        if (text !== this) {
            log("replace :: {} -> {}", length, text?.length)
            replace(0, length, text)
        }
    }

    private fun updateLayerBounds(drawable: Drawable?, width: Int, height: Int) {
        if (drawable != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val layerSize = Units.DP.toPx(52f).toInt()
            val posX = (width / 2 - layerSize / 2)
            val posY = (height / 2 - layerSize / 2)
            drawable.setBounds(posX, posY, posX + layerSize, posY + layerSize)
        }
    }

    inner class LinkPreviewSpan(private val linkPreview: LinkPreview) : ClickableSpan(), MyClickableSpan {
        override fun onClick(widget: View) {
            widget.cancelLongPress()
            widget.cancelPendingInputEvents()
            AppContext.get().onShowPreview(linkPreview)
        }
    }

    inner class LinkPreviewLoader(
        private val linkPreviewUrl: Any,
        private val linkPreview: LinkPreview,
        private val linkPreviewContent: SimpleSpanBuilder
    ) {

        private val previewWidth = withWidth
        private val previewType = linkPreview.getType()
        private val previewHeight =
            if (previewType == LinkPreview.Type.AUDIO) {
                previewWidth / 3
            } else {
                withHeight
            }
        private var withClickableImage = linkPreview.withPreviewPlaceholder || withShowHideAction
        private val previewTransformation = GlideUtils.createTransformation(linkPreview)

        fun load() {
            val request = GlideUtils.loadDrawable(context, linkPreviewUrl)
                .apply(
                    RequestOptions()
                        .override(withWidth, withHeight)
                        .fitCenter()
                        .timeout(withLinkPreviewState.getTimeout())
                        .transform(previewTransformation)
                )
            log("preview full media :: {}", linkPreviewUrl)
            request.into(object : SimpleTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) = doOnResourceReady(resource, transition)
                override fun onLoadFailed(errorDrawable: Drawable?) = doOnLoadFailed(errorDrawable)
            })
        }

        private fun getPreviewPlaceholder(placeholder: Drawable?, transformation: Transformation<Bitmap>): Drawable? {
            if (!withClickableImage) return null
            val placeholderRef = placeholder ?: GlideUtils.createPlaceholder()
            placeholderRef.setBounds(0, 0, withWidth, withHeight)
            val glide = GlideUtils.get(context)
            try {
                val bitmapResource = BitmapResource.obtain(placeholderRef.toBitmap(withWidth, withHeight), glide.bitmapPool)
                    ?: return null
                return transformation
                    .transform(context, bitmapResource, withWidth, withHeight)
                    .get()
                    .toDrawable(context.resources)
            } catch (th: Throwable) {
                glide.clearMemory()
                return null
            }
        }

        private fun doOnLoadFailed(errorDrawable: Drawable?) {
            runCatching {
                if (context.isContextDestroyed()) return
                val contentCopy = linkPreviewContent.copy()
                val placeholderWithTransformation = getPreviewPlaceholder(errorDrawable, previewTransformation)
                if (previewType != null && previewType.isMedia() && withClickableImage && placeholderWithTransformation != null) {
                    val preview = previewType.toDrawable(context, previewWidth)
                    val drawable = LayerDrawable(
                        arrayOf(
                            placeholderWithTransformation,
                            preview
                        )
                    )
                    drawable.setBounds(0, 0, previewWidth, previewHeight)
                    updateLayerBounds(preview, previewWidth, previewHeight)
                    val imageSpan = ImageSpan(drawable, DynamicDrawableSpan.ALIGN_BOTTOM)
                    val spans = arrayOf(imageSpan, LinkPreviewSpan(linkPreview))
                    contentCopy.append("\n")
                    contentCopy.append("\n", RelativeSizeSpan(0.2f))
                    contentCopy.append(linkPreviewUrl.toString(), *spans)
                }

                val span = contentCopy.build()
                span.setSpan(MyQuoteSpan(colorAccent), withUrl.length + 2, span.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                tryToReplaceIn(span)
            }
        }

        private fun doOnResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
            runCatching {
                if (context.isContextDestroyed()) return
                var drawable = resource
                if (drawable is Animatable) {
                    withClickableImage = false
                    drawable = resource.mutate()
                    (drawable as Animatable).start()
                }
                var preview: Drawable? = null
                if (withClickableImage) {
                    if (previewType != null) {
                        preview = previewType.toDrawable(context, previewWidth)
                        drawable = LayerDrawable(
                            arrayOf(
                                resource.mutate(),
                                preview
                            )
                        )
                    }
                }
                drawable.setBounds(0, 0, resource.intrinsicWidth, resource.intrinsicHeight)
                updateLayerBounds(preview, resource.intrinsicWidth, resource.intrinsicHeight)
                val imageSpan = ImageSpan(drawable, DynamicDrawableSpan.ALIGN_BOTTOM)
                val spans =
                    if (withClickableImage) {
                        arrayOf(imageSpan, LinkPreviewSpan(linkPreview))
                    } else {
                        arrayOf(imageSpan)
                    }

                val contentCopy = linkPreviewContent.copy()
                contentCopy.append("\n")
                contentCopy.append("\n", RelativeSizeSpan(0.2f))
                contentCopy.append(linkPreviewUrl.toString(), *spans)

                val span = contentCopy.build()
                span.setSpan(MyQuoteSpan(colorAccent), withUrl.length + 2, span.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                tryToReplaceIn(span)
                if (drawable is Animatable) {
                    val callback = AnimatableDrawableCallback(WeakReference(withTextView), imageSpan)
                    drawable.callback = callback
                }
            }
        }

        private fun doOnResourceLoading(placeholder: Drawable?) {
            if (context.isContextDestroyed()) return
            val placeholderWithTransformation = getPreviewPlaceholder(placeholder, previewTransformation)
            if (previewType != null && previewType.isMedia() && withClickableImage && placeholderWithTransformation != null) {
                val contentCopy = linkPreviewContent.copy()
                val preview = previewType.toDrawable(context, previewWidth)
                val drawable = LayerDrawable(
                    arrayOf(
                        placeholderWithTransformation,
                        preview
                    )
                )
                drawable.setBounds(0, 0, previewWidth, previewHeight)
                updateLayerBounds(preview, previewWidth, previewHeight)
                val imageSpan = ImageSpan(drawable, DynamicDrawableSpan.ALIGN_BOTTOM)
                val spans =
                    if (withClickableImage) {
                        arrayOf(imageSpan, LinkPreviewSpan(linkPreview))
                    } else {
                        arrayOf(imageSpan)
                    }
                contentCopy.append("\n")
                contentCopy.append("\n", RelativeSizeSpan(0.2f))
                contentCopy.append(linkPreviewUrl.toString(), *spans)

                val span = contentCopy.build()
                span.setSpan(MyQuoteSpan(colorAccent), withUrl.length + 2, span.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                tryToReplaceIn(span)
            }
        }
    }

    private inner class ShowHidePreviewSpan(val show: Boolean) : ClickableSpan(), MyClickableSpan {
        override fun onClick(widget: View) {
            widget.cancelLongPress()
            widget.cancelPendingInputEvents()
            if (!show) withLinkPreviewFetcher.clearPreview(this@LinkPreviewSpannable, urlString)
            withLinkPreviewState.canShowPreview.setValue(show)
        }
    }

    private class MyQuoteSpan @JvmOverloads constructor(
        @ColorInt val color: Int = STANDARD_COLOR,
        @Px val stripeWidth: Int = STANDARD_STRIPE_WIDTH_PX,
        @Px val gapWidth: Int = STANDARD_GAP_WIDTH_PX
    ) : LeadingMarginSpan {

        override fun getLeadingMargin(first: Boolean): Int {
            return stripeWidth + gapWidth
        }

        override fun drawLeadingMargin(
            @NonNull c: Canvas, @NonNull p: Paint, x: Int, dir: Int,
            top: Int, baseline: Int, bottom: Int,
            @NonNull text: CharSequence, start: Int, end: Int,
            first: Boolean, @NonNull layout: Layout
        ) {
            val style = p.style
            val color = p.color

            p.style = Paint.Style.FILL
            p.color = this.color

            c.drawRect(x.toFloat(), top.toFloat(), (x + dir * stripeWidth).toFloat(), bottom.toFloat(), p)

            p.style = style
            p.color = color
        }

        companion object {
            private val STANDARD_STRIPE_WIDTH_PX = Units.DP.toPx(2f).toInt()
            private val STANDARD_GAP_WIDTH_PX = Units.DP.toPx(8f).toInt()
            private const val STANDARD_COLOR = -0xffff01
        }
    }

}