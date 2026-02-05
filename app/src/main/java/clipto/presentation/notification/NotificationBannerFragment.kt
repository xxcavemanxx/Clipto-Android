package clipto.presentation.notification

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import clipto.AppContext
import clipto.AppUtils
import clipto.analytics.Analytics
import clipto.common.extensions.withSafeFragmentManager
import clipto.common.misc.AnimationUtils
import clipto.common.misc.IntentUtils
import clipto.common.misc.ThemeUtils
import clipto.common.presentation.mvvm.base.BaseFragment
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.fragment_notification_banner.*

class NotificationBannerFragment : BaseFragment() {

    override val layoutResId: Int = R.layout.fragment_notification_banner

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        arguments?.let {
            val code = it.getString(ATTR_CODE)
            val title = it.getString(ATTR_TITLE)
            val message = it.getString(ATTR_MESSAGE)
            titleView?.text = title
            descriptionView?.text = message
            val state = AppContext.get().appConfig
            val contentHeight = ThemeUtils.getDimensionPixelSize(ctx, R.attr.actionBarSize)
            contentView?.translationY = -contentHeight
            contentView?.setOnClickListener {
                IntentUtils.open(ctx, state.getUnexpectedErrorInstructionUrl())
                Analytics.onBugInstructionRead()
                hide()
            }
            actionView?.setOnClickListener {
                if (state.canReportUnexpectedErrorDirectly()) {
                    AppUtils.sendRequest(code, null, error)
                    Analytics.onBugReport()
                } else {
                    IntentUtils.open(ctx, state.getUnexpectedErrorInstructionUrl())
                    Analytics.onBugInstructionRead()
                }
                hide()
            }
            show()
        } ?: run {
            hide()
        }
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
        private const val TAG = "NotificationBannerFragment"

        private const val ATTR_CODE = "attr_code"
        private const val ATTR_TITLE = "attr_title"
        private const val ATTR_MESSAGE = "attr_message"

        private var error: Throwable? = null

        fun show(
                context: Context,
                code: String?,
                title: String,
                message: String,
                error: Throwable? = null) {
            NotificationBannerFragment.error = error
            context.withSafeFragmentManager()?.let { fm ->
                var fragment = fm.findFragmentByTag(TAG)
                if (fragment == null) {
                    fragment = NotificationBannerFragment().apply {
                        arguments = Bundle().also {
                            it.putString(ATTR_CODE, code)
                            it.putString(ATTR_TITLE, title)
                            it.putString(ATTR_MESSAGE, message)
                        }
                    }
                    fm.beginTransaction().add(Window.ID_ANDROID_CONTENT, fragment, TAG).commitNow()
                } else {
                    fragment as NotificationBannerFragment
                    fragment.show()
                }
            }
        }

        fun hide(context: Context) {
            context.withSafeFragmentManager()?.let { fm ->
                fm.findFragmentByTag(TAG)?.let {
                    it as NotificationBannerFragment
                    it.hide()
                }
            }
        }

    }

}