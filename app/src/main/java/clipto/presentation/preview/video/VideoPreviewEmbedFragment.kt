package clipto.presentation.preview.video

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.activityViewModels
import clipto.common.extensions.setBottomSheetHeight
import clipto.common.extensions.withSafeFragmentManager
import clipto.common.logging.L
import clipto.common.presentation.mvvm.base.BaseBottomSheetDialogFragment
import clipto.common.presentation.mvvm.base.hideSystemUI
import clipto.common.presentation.mvvm.base.setBackgroundColor
import clipto.common.presentation.mvvm.base.showSystemUI
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_preview_video_embed.*
import kotlin.math.max

@AndroidEntryPoint
class VideoPreviewEmbedFragment : BaseBottomSheetDialogFragment() {

    val viewModel: VideoPreviewEmbedViewModel by activityViewModels()

    override val layoutResId: Int = R.layout.fragment_preview_video_embed

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewModel.dataLiveData.postValue(VideoPreviewEmbedViewModel.Data(bottomSheetState = BottomSheetBehavior.STATE_EXPANDED))
            viewModel.onEnterFullscreen()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (activity == null) {
            dismissAllowingStateLoss()
            return
        }
        val url = arguments?.getString(ATTR_URL)
        if (url != null) {
            contentView.setBottomSheetHeight(0.4f) { sheet, initialHeight, _ ->
                // SIZE
                webView.layoutParams?.height = initialHeight

                // SWIPE
                sheet.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        viewModel.dataLiveData.postValue(VideoPreviewEmbedViewModel.Data(slideOffset = slideOffset))
                    }

                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        viewModel.dataLiveData.postValue(VideoPreviewEmbedViewModel.Data(bottomSheetState = newState))
                        if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                            webView?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
                            viewModel.onEnterFullscreen()
                            webView?.requestLayout()
                            L.log(this@VideoPreviewEmbedFragment, "Player: height=full")
                        } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                            L.log(this@VideoPreviewEmbedFragment, "Player: height={}", initialHeight)
                            webView?.layoutParams?.height = initialHeight
                            viewModel.onExitFullscreen()
                            webView?.requestLayout()
                        }
                    }
                })

                // TOOLBAR

                viewModel.fullScreenLiveData.observe(this) {
                    if (it) {
                        sheet.state = BottomSheetBehavior.STATE_EXPANDED
                    } else {
                        sheet.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }

                viewModel.dataLiveData.observe(this) {
                    val slideOffset = it.slideOffset
                    val level = 0.8f
                    if ((slideOffset != null && slideOffset >= level) || it.bottomSheetState == BottomSheetBehavior.STATE_EXPANDED) {
                        activity?.hideSystemUI()
                        webView?.hideSystemUI()
                    } else {
                        activity?.showSystemUI()
                        webView?.showSystemUI()
                    }
                    if (slideOffset != null) {
                        val alphaLevel = if (slideOffset >= level) 1f else level
                        val alpha = max(0, (255 * alphaLevel).toInt())
                        val color = ColorUtils.setAlphaComponent(Color.BLACK, alpha)
                        activity?.setBackgroundColor(color)
                    }
                }

                viewModel.errorLiveData.observe(this) {
                    activity?.finish()
                }
            }

            // WEB VIEW
            webView.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progressView?.progress = newProgress
                    progressView?.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
                }
            }
            webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            webView.settings.domStorageEnabled = true
            webView.settings.allowContentAccess = true
            webView.settings.javaScriptEnabled = true
            webView.settings.databaseEnabled = true
            webView.settings.allowFileAccess = true
            webView.settings.allowFileAccessFromFileURLs = true
            webView.settings.allowUniversalAccessFromFileURLs = true
            webView.loadDataWithBaseURL(
                url,
                """
                        <html>
                            <head><meta charset="utf-8"></head>
                            <body style="margin: 0; padding: 0; background-color: black">
                                <iframe type="text/html" width="100%" height="100%" src="$url" frameborder="0"></iframe>
                            </body>
                        </html>
                    """.trimIndent(),
                "text/html",
                "utf-8",
                null
            )
        } else {
            dismissAllowingStateLoss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.finish()
    }

    companion object {

        const val ATTR_URL = "attr_url"
        const val ATTR_TITLE = "attr_title"

        fun open(context: Context, url: String, title: String? = null) {
            context.withSafeFragmentManager()?.let { fm ->
                VideoPreviewEmbedFragment()
                    .apply {
                        arguments = Bundle().apply {
                            putString(ATTR_URL, url)
                            putString(ATTR_TITLE, title)
                        }
                    }
                    .show(fm, "PlayerEmbedFragment")
            }
        }
    }

}