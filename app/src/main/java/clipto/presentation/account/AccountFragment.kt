package clipto.presentation.account

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
import kotlinx.android.synthetic.main.fragment_account.*

@AndroidEntryPoint
class AccountFragment : MvvmFragment<AccountViewModel>() {

    override val layoutResId: Int = R.layout.fragment_account
    override val viewModel: AccountViewModel by viewModels()

    override fun bind(viewModel: AccountViewModel) {
        val settings = viewModel.getSettings()

        withDefaults(toolbar, R.string.account_toolbar_title)

        changePlanButton?.setDebounceClickListener {
            navigateTo(R.id.action_select_plan)
        }

        syncSwitch.isChecked = !settings.disableSync
        syncTitleView.isEnabled = !settings.disableSync
        syncAction.setOnClickListener {
            syncSwitch.isChecked = !syncSwitch.isChecked
        }
        syncSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.disableSync = !isChecked
            syncTitleView.isEnabled = isChecked
            viewModel.onSyncAll()
        }

        signOutAction.setOnClickListener {
            viewModel.onSignOut()
        }

        learnMoreAction.setOnClickListener {
            IntentUtils.open(requireContext(), viewModel.appConfig.getInviteFriendRewardUrl())
        }
        shareAction.setOnClickListener {
            viewModel.onShareApp()
        }

        openInBrowser.setOnClickListener {
            Analytics.onOpenBrowserApp()
            IntentUtils.open(requireContext(), BuildConfig.appSiteLink)
        }

        val linux = viewModel.string(R.string.desktop_main_menu_download_linux)
        val win = viewModel.string(R.string.desktop_main_menu_download_windows)
        val mac = viewModel.string(R.string.desktop_main_menu_download_mac)
        downloadDesktop.text = viewModel.string(R.string.desktop_main_menu_download_for, "${win}, ${mac}, $linux")
        downloadDesktop.setOnClickListener {
            Analytics.onDownloadDesktopApp()
            IntentUtils.open(requireContext(), BuildConfig.appDownloadLink)
        }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user.isAuthorized()) {
                user.photoUrl?.let { icon.load(it) }
                titleTextView.text = user.getTitle()
                        ?: getString(R.string.account_title_authorized)
            } else {
                navigateUp()
            }
        }
        viewModel.license.observe(viewLifecycleOwner) { title ->
            if (title != null) {
                descriptionTextView.text = title
                descriptionTextView.setVisibleOrGone(true)
                shareContainer?.setVisibleOrGone(true)
            } else {
                descriptionTextView.setVisibleOrGone(false)
                shareContainer?.setVisibleOrGone(false)
            }
        }
        viewModel.invitations.observe(viewLifecycleOwner) {
            shareStatistics?.text = viewModel.string(R.string.about_label_campaign_share_description, it)
        }

        Analytics.screenAccount()
    }

}
