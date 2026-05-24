package clipto.presentation.preview.image

import com.wb.clipboard.databinding.ActivityPreviewImageBinding
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import clipto.common.extensions.gone
import clipto.common.extensions.safeIntent
import clipto.common.extensions.setVisibleOrGone
import clipto.common.logging.L
import clipto.common.presentation.mvvm.base.BaseActivity
import clipto.common.presentation.mvvm.base.hideSystemUI
import clipto.common.presentation.mvvm.base.showSystemUI
import clipto.common.presentation.view.BitmapViewTarget
import clipto.extensions.onCreateWithLocale
import clipto.utils.GlideUtils
import com.davemorrissey.labs.subscaleview.ImageSource
import com.wb.clipboard.R

class ImagePreviewActivity : BaseActivity() {

    
    private lateinit var binding: ActivityPreviewImageBinding
override val layoutResId: Int = R.layout.activity_preview_image

    override fun onCreate(savedInstanceState: Bundle?) {
        onCreateWithLocale()
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewImageBinding.bind(window.decorView.findViewById(android.R.id.content))
        showSystemUI()
        val urlRef = imageUrl
        if (urlRef != null) {
            binding.toolbar.setNavigationIcon(R.drawable.ic_link_preview_zoom_out)
            binding.toolbar.setNavigationOnClickListener { finish() }
            binding.toolbar.title = imageTitle
            GlideUtils.loadBitmap(this, urlRef)
                .let {
                    val thumbRef = imageThumbUrl
                    if (thumbRef != null) {
                        it.thumbnail(
                            GlideUtils.loadBitmap(this, thumbRef)
                        )
                    } else {
                        it
                    }
                }
                .into(
                    BitmapViewTarget(
                        binding.imageView,
                        onReady = {
                            binding.cpiIndicator.gone()
                            binding.imageView.setImage(ImageSource.bitmap(it))
                        })
                )
            binding.imageView.setOnClickListener {
                val isVisible = binding.toolbar.isVisible
                if (isVisible) {
                    hideSystemUI()
                } else {
                    showSystemUI()
                }
                binding.vStatusBar.setVisibleOrGone(!isVisible)
                binding.toolbar.setVisibleOrGone(!isVisible)
            }
            binding.imageView.maxScale = 30f
            binding.imageView.minScale = 0.1f
        } else {
            finish()
        }
    }

    override fun onBackPressed() {
        finish()
    }

    override fun finish() {
        imageThumbUrl = null
        imageTitle = null
        imageUrl = null
        super.finish()
        overridePendingTransition(0, 0)
    }

    companion object {

        private var imageUrl: Any? = null
        private var imageThumbUrl: Any? = null
        private var imageTitle: String? = null

        fun preview(
            context: Context,
            url: Any?,
            thumbUrl: Any? = null,
            title: String? = null
        ) {
            L.log(this, "preview :: uri={}", url)
            val intent = Intent(context, ImagePreviewActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            imageThumbUrl = thumbUrl
            imageTitle = title
            imageUrl = url
            context.safeIntent(intent)
        }
    }

}
