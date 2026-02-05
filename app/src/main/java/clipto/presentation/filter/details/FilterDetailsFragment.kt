package clipto.presentation.filter.details

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import clipto.common.extensions.withSafeFragmentManager
import clipto.presentation.common.fragment.blocks.BlocksWithHintBottomSheetFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FilterDetailsFragment : BlocksWithHintBottomSheetFragment<FilterDetailsViewModel>() {

    override val viewModel: FilterDetailsViewModel by viewModels()

    companion object {
        private const val TAG = "FilterDetailsFragment"

        fun show(activity: FragmentActivity) {
            activity.withSafeFragmentManager()?.let { fm ->
                FilterDetailsFragment().show(fm, TAG)
            }
        }
    }

}