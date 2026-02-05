package clipto.presentation.notification

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.fragment.app.viewModels
import clipto.analytics.Analytics
import clipto.common.extensions.withSafeFragmentManager
import clipto.common.misc.AnimationUtils
import clipto.common.misc.ThemeUtils
import clipto.common.presentation.mvvm.base.BaseFragment
import clipto.common.presentation.text.SimpleSpanBuilder
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_new_version_available_banner.*

@AndroidEntryPoint
class NewVersionBannerFragment : BaseFragment() {

    override val layoutResId: Int = R.layout.fragment_new_version_available_banner
    val viewModel: NewVersionBannerViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Analytics.onNewVersionAvailable()
        val ctx = requireContext()
        val latestVersion = viewModel.latestVersion
        titleView?.text = SimpleSpanBuilder()
                .append(ctx.getText(R.string.desktop_update_title))
                .append(" (")
                .append(latestVersion)
                .append(")")
                .build()
        val contentHeight = ThemeUtils.getDimensionPixelSize(ctx, R.attr.actionBarSize)
        contentView?.translationY = -contentHeight
        contentView?.setOnClickListener { viewModel.onClicked() }
        viewModel.dismissLive.observe(viewLifecycleOwner) { hide() }
        show()
    }

    internal fun hide() {
        context?.let { ctx ->
            val contentHeight = ThemeUtils.getDimensionPixelSize(ctx, R.attr.actionBarSize)
            AnimationUtils.translationY(contentView, 0f, -contentHeight, null)?.start()
        }
    }

    internal fun show() {
        context?.let { ctx ->
            contentView?.let {
                val contentHeight = ThemeUtils.getDimensionPixelSize(ctx, R.attr.actionBarSize)
                AnimationUtils.translationY(it, -contentHeight, 0f, null)?.start()
            }
        }
    }

    companion object {
        private const val TAG = "NewVersionBannerFragment"

        fun show(context: Context) {
            context.withSafeFragmentManager()?.let { fm ->
                var fragment = fm.findFragmentByTag(TAG)
                if (fragment == null) {
                    fragment = NewVersionBannerFragment()
                    fm.beginTransaction().add(Window.ID_ANDROID_CONTENT, fragment, TAG).commitNow()
                } else {
                    fragment as NewVersionBannerFragment
                    fragment.show()
                }
            }
        }

        fun hide(context: Context) {
            context.withSafeFragmentManager()?.let { fm ->
                fm.findFragmentByTag(TAG)?.let {
                    it as NewVersionBannerFragment
                    it.hide()
                }
            }
        }

    }

}