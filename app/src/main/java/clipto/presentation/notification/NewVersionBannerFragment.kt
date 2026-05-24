package clipto.presentation.notification

import com.wb.clipboard.databinding.FragmentNewVersionAvailableBannerBinding
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

@AndroidEntryPoint
class NewVersionBannerFragment : BaseFragment() {

    
    private var _binding: FragmentNewVersionAvailableBannerBinding? = null
    private val binding get() = _binding!!
override val layoutResId: Int = R.layout.fragment_new_version_available_banner
    val viewModel: NewVersionBannerViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentNewVersionAvailableBannerBinding.bind(view)
        Analytics.onNewVersionAvailable()
        val ctx = requireContext()
        val latestVersion = viewModel.latestVersion
        binding.titleView?.text = SimpleSpanBuilder()
                .append(ctx.getText(R.string.desktop_update_title))
                .append(" (")
                .append(latestVersion)
                .append(")")
                .build()
        val contentHeight = ThemeUtils.getDimensionPixelSize(ctx, R.attr.actionBarSize)
        binding.contentView?.translationY = -contentHeight
        binding.contentView?.setOnClickListener { viewModel.onClicked() }
        viewModel.dismissLive.observe(viewLifecycleOwner) { hide() }
        show()
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
