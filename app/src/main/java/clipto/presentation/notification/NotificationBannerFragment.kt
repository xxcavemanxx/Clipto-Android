package clipto.presentation.notification

import com.wb.clipboard.databinding.FragmentNotificationBannerBinding
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

class NotificationBannerFragment : BaseFragment() {

    
    private var _binding: FragmentNotificationBannerBinding? = null
    private val binding get() = _binding!!
override val layoutResId: Int = R.layout.fragment_notification_banner

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentNotificationBannerBinding.bind(view)
        val ctx = requireContext()
        arguments?.let {
            val code = it.getString(ATTR_CODE)
            val title = it.getString(ATTR_TITLE)
            val message = it.getString(ATTR_MESSAGE)
            binding.titleView?.text = title
            binding.descriptionView?.text = message
            val state = AppContext.get().appConfig
            val contentHeight = ThemeUtils.getDimensionPixelSize(ctx, R.attr.actionBarSize)
            binding.contentView?.translationY = -contentHeight
            binding.contentView?.setOnClickListener {
                IntentUtils.open(ctx, state.getUnexpectedErrorInstructionUrl())
                Analytics.onBugInstructionRead()
                hide()
            }
            binding.actionView?.setOnClickListener {
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
            AnimationUtils.translationY(binding.contentView, 0f, -contentHeight, null)?.start()
        }
    }

    internal fun show() {
        context?.let { ctx ->
            binding.contentView?.let {
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
