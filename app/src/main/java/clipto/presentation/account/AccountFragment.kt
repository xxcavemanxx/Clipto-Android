package clipto.presentation.account
import android.view.View
import android.os.Bundle

import com.wb.clipboard.databinding.FragmentAccountBinding
import androidx.fragment.app.viewModels
import clipto.analytics.Analytics
import clipto.common.extensions.load
import clipto.common.extensions.navigateTo
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.setVisibleOrGone
import clipto.common.misc.IntentUtils
import clipto.common.presentation.mvvm.MvvmFragment
import com.wb.clipboard.BuildConfig
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountFragment : MvvmFragment<AccountViewModel>() {

    
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentAccountBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
    }
private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
override val layoutResId: Int = R.layout.fragment_account
    override val viewModel: AccountViewModel by viewModels()

    override fun bind(viewModel: AccountViewModel) {
        val settings = viewModel.getSettings()

        withDefaults(binding.toolbar, R.string.account_toolbar_title)

        binding.changePlanButton?.setDebounceClickListener {
            navigateTo(R.id.action_select_plan)
        }

        binding.syncSwitch.isChecked = !settings.disableSync
        binding.syncTitleView.isEnabled = !settings.disableSync
        binding.syncAction.setOnClickListener {
            binding.syncSwitch.isChecked = !binding.syncSwitch.isChecked
        }
        binding.syncSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.disableSync = !isChecked
            binding.syncTitleView.isEnabled = isChecked
            viewModel.onSyncAll()
        }

        binding.signOutAction.setOnClickListener {
            viewModel.onSignOut()
        }

        binding.learnMoreAction.setOnClickListener {
            IntentUtils.open(requireContext(), viewModel.appConfig.getInviteFriendRewardUrl())
        }
        binding.shareAction.setOnClickListener {
            viewModel.onShareApp()
        }

        binding.openInBrowser.setOnClickListener {
            Analytics.onOpenBrowserApp()
            IntentUtils.open(requireContext(), BuildConfig.appSiteLink)
        }

        val linux = viewModel.string(R.string.desktop_main_menu_download_linux)
        val win = viewModel.string(R.string.desktop_main_menu_download_windows)
        val mac = viewModel.string(R.string.desktop_main_menu_download_mac)
        binding.downloadDesktop.text = viewModel.string(R.string.desktop_main_menu_download_for, "${win}, ${mac}, $linux")
        binding.downloadDesktop.setOnClickListener {
            Analytics.onDownloadDesktopApp()
            IntentUtils.open(requireContext(), BuildConfig.appDownloadLink)
        }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user.isAuthorized()) {
                user.photoUrl?.let { binding.icon.load(it) }
                binding.titleTextView.text = user.getTitle()
                        ?: getString(R.string.account_title_authorized)
            } else {
                navigateUp()
            }
        }
        viewModel.license.observe(viewLifecycleOwner) { title ->
            if (title != null) {
                binding.descriptionTextView.text = title
                binding.descriptionTextView.setVisibleOrGone(true)
                binding.shareContainer?.setVisibleOrGone(true)
            } else {
                binding.descriptionTextView.setVisibleOrGone(false)
                binding.shareContainer?.setVisibleOrGone(false)
            }
        }
        viewModel.invitations.observe(viewLifecycleOwner) {
            binding.shareStatistics?.text = viewModel.string(R.string.about_label_campaign_share_description, it)
        }

        Analytics.screenAccount()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
