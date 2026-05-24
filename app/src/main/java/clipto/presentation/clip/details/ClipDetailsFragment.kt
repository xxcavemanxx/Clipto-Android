package clipto.presentation.clip.details
import android.view.View
import android.os.Bundle

import com.wb.clipboard.databinding.FragmentClipDetailsBinding
import androidx.fragment.app.viewModels
import clipto.analytics.Analytics
import clipto.common.extensions.setBottomSheetHeight
import clipto.common.presentation.mvvm.MvvmBottomSheetDialogFragment
import clipto.common.presentation.state.ViewPagerState
import clipto.domain.ClipDetailsTab
import clipto.extensions.log
import clipto.presentation.clip.details.pages.PageFragment
import clipto.presentation.clip.details.pages.attachments.AttachmentsPageFragment
import clipto.presentation.clip.details.pages.attributes.AttributesPageFragment
import clipto.presentation.clip.details.pages.dynamic.DynamicValuesPageFragment
import clipto.presentation.clip.details.pages.general.GeneralPageFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ClipDetailsFragment : MvvmBottomSheetDialogFragment<ClipDetailsViewModel>() {

    
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentClipDetailsBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
    }
private var _binding: FragmentClipDetailsBinding? = null
    private val binding get() = _binding!!
override val layoutResId: Int = R.layout.fragment_clip_details
    override val viewModel: ClipDetailsViewModel by viewModels()

    override fun bind(viewModel: ClipDetailsViewModel) {
        binding.contentLayout.setBottomSheetHeight(noBackground = true) { bottomSheet, _, parentView ->
            val selectedTab = viewModel.state.selectedTab.requireValue()

            ViewPagerState()
                .withTabLayout(binding.tabLayout)
                .withPages(
                    childFragmentManager,
                    PageFragment.page(this, ClipDetailsTab.GENERAL, parentView) { GeneralPageFragment() },
                    PageFragment.page(this, ClipDetailsTab.ATTRIBUTES, parentView) { AttributesPageFragment() },
                    PageFragment.page(this, ClipDetailsTab.ATTACHMENTS, parentView) { AttachmentsPageFragment() },
                    PageFragment.page(this, ClipDetailsTab.DYNAMIC_VALUES, parentView) { DynamicValuesPageFragment() }
                )
                .apply(binding.viewPager)

            val viewMode = ViewMode.valueOf(selectedTab)
            binding.tabLayout.getTabAt(viewMode.position)
                ?.takeIf { !it.isSelected }
                ?.let { binding.tabLayout.selectTab(it) }

            viewModel.state.dismiss.getLiveData().observe(viewLifecycleOwner) {
                it?.let { viewModel.dismiss() }
            }

            viewModel.state.expand.getLiveData().observe(viewLifecycleOwner) {
                bottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
            }

            viewModel.state.attributesCount.getLiveData().observe(viewLifecycleOwner) { count ->
                binding.tabLayout.getTabAt(ViewMode.ATTRIBUTES.position)?.let { tab ->
                    updateTab(tab, count)
                }
            }

            viewModel.state.files.getLiveData().observe(viewLifecycleOwner) {
                val count = it.size
                binding.tabLayout.getTabAt(ViewMode.ATTACHMENTS.position)?.let { tab ->
                    updateTab(tab, count)
                }
            }

            viewModel.getDynamicFieldsCountLive().observe(viewLifecycleOwner) { count ->
                binding.tabLayout.getTabAt(ViewMode.DYNAMIC_VALUES.position)?.let { tab ->
                    updateTab(tab, count)
                }
            }
        }
        Analytics.screenClipDetails()
    }

    private fun updateTab(tab: TabLayout.Tab?, count: Int) {
        tab?.let {
            log("files :: updateTab :: {} -> {}", tab.text, count)
            if (count > 0) {
                tab.orCreateBadge.number = count
            } else {
                tab.removeBadge()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
        viewModel.onClose()
    }

    companion object {
        internal const val TAG = "ClipDetailsFragment"
    }
}
