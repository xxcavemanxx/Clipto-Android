package clipto.presentation.preview.video

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.core.view.MenuItemCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.mediarouter.app.MediaRouteActionProvider
import clipto.common.extensions.setBottomSheetHeight
import clipto.common.extensions.setVisibleOrGone
import clipto.common.extensions.withSafeFragmentManager
import clipto.common.logging.L
import clipto.common.presentation.mvvm.base.BaseBottomSheetDialogFragment
import clipto.common.presentation.mvvm.base.hideSystemUI
import clipto.common.presentation.mvvm.base.setBackgroundColor
import clipto.common.presentation.mvvm.base.showSystemUI
import clipto.common.presentation.state.MenuState
import clipto.common.presentation.state.ToolbarState
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.fragment_preview_video.*
import kotlin.math.max

class VideoPreviewFragment : BaseBottomSheetDialogFragment() {

    val viewModel: VideoPreviewViewModel by activityViewModels()

    override val layoutResId: Int = R.layout.fragment_preview_video

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            playerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
            viewModel.onEnterFullscreen()
            playerView?.requestLayout()
        } else {
            playerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            playerView?.requestLayout()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (activity == null) {
            dismissAllowingStateLoss()
            return
        }
        val title = arguments?.getString(ATTR_TITLE)
        val uri = arguments?.getString(ATTR_URL)?.toUri()
        val cache = arguments?.getBoolean(ATTR_CACHE, false) ?: false
        if (uri != null) {
            contentView.setBottomSheetHeight(0.4f) { sheet, initialHeight, _ ->
                // SIZE
                playerView.layoutParams?.height = initialHeight

                // SWIPE
                sheet.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        viewModel.dataLiveData.postValue(VideoPreviewEmbedViewModel.Data(slideOffset = slideOffset))
                    }

                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        viewModel.dataLiveData.postValue(VideoPreviewEmbedViewModel.Data(bottomSheetState = newState))
                        if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                            playerView?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
                            viewModel.onEnterFullscreen()
                            playerView?.requestLayout()
                            L.log(this@VideoPreviewFragment, "Player: height=full")
                        } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                            L.log(this@VideoPreviewFragment, "Player: height={}", initialHeight)
                            playerView?.layoutParams?.height = initialHeight
                            viewModel.onExitFullscreen()
                            playerView?.requestLayout()
                        }
                    }
                })

                // TOOLBAR

                val ctx = requireContext()
                val castId = R.id.cast_notification_id
                val castMenu = toolbar.menu.add(1, castId, 1, "")
                castMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                MenuItemCompat.setActionProvider(castMenu, MediaRouteActionProvider(ctx))
                CastButtonFactory.setUpMediaRouteButton(ctx, toolbar.menu, castId)

                val toolbarState = ToolbarState<Unit>()
                    .withContext(requireContext())
                    .withIgnoredMenuItem(castId)
                    .withMenuItem(MenuState.StatefulMenuItem<Unit>()
                        .withShowAsActionAlways()
                        .withIcon(R.drawable.ic_fullscreen_open)
                        .withTitle(R.string.menu_fullscreen_enter)
                        .withStateAcceptor { !viewModel.isInFullScreen() }
                        .withListener { _, _ ->
                            viewModel.onEnterFullscreen()
                            sheet.state = BottomSheetBehavior.STATE_EXPANDED
                        })
                    .withMenuItem(MenuState.StatefulMenuItem<Unit>()
                        .withShowAsActionAlways()
                        .withIcon(R.drawable.ic_fullscreen_close)
                        .withTitle(R.string.menu_fullscreen_exit)
                        .withStateAcceptor { viewModel.isInFullScreen() }
                        .withListener { _, _ ->
                            viewModel.onExitFullscreen()
                        })
                toolbarState.apply(Unit, toolbar)

                playerView.setControllerVisibilityListener {
                    val visible = it == View.VISIBLE
                    toolbar?.let {
                        if (visible) toolbarState.apply(Unit, toolbar)
                        if (playerView?.useController == true) {
                            it.setVisibleOrGone(visible)
                        }
                        if (visible) {
                            playerView?.showSystemUI()
                        } else if (sheet.state == BottomSheetBehavior.STATE_EXPANDED) {
                            playerView?.hideSystemUI()
                        }
                    }
                }

                viewModel.fullScreenLiveData.observe(this) {
                    if (it) {
                        sheet.state = BottomSheetBehavior.STATE_EXPANDED
                    } else {
                        sheet.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                    toolbarState.apply(Unit, toolbar)
                }

                viewModel.playerLiveData.observe(this) {
                    val isCasting = viewModel.isCasting()
                    castControlView?.setVisibleOrGone(isCasting)
                    toolbar?.setVisibleOrGone(true)
                    playerView?.useController = !isCasting
                }

                viewModel.dataLiveData.observe(this) {
                    val slideOffset = it.slideOffset
                    val level = 0.8f
                    if (!toolbar.isVisible && ((slideOffset != null && slideOffset >= level) || it.bottomSheetState == BottomSheetBehavior.STATE_EXPANDED)) {
                        playerView?.hideSystemUI()
                    } else {
                        playerView?.showSystemUI()
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
            viewModel.onBind(playerView, castControlView)
            viewModel.onPlay(uri, cache, title)
        } else {
            dismissAllowingStateLoss()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPausePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.finish()
    }

    companion object {

        const val ATTR_URL = "attr_url"
        const val ATTR_CACHE = "attr_cache"
        const val ATTR_TITLE = "attr_title"

        fun open(context: Context, url: String, cache: Boolean, title: String? = null) {
            context.withSafeFragmentManager()?.let { fm ->
                VideoPreviewFragment()
                    .apply {
                        arguments = Bundle().apply {
                            putString(ATTR_URL, url)
                            putString(ATTR_TITLE, title)
                            putBoolean(ATTR_CACHE, cache)
                        }
                    }
                    .show(fm, "PlayerFragment")
            }
        }
    }

}