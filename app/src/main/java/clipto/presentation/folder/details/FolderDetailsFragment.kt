package clipto.presentation.folder.details

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import clipto.common.extensions.withSafeFragmentManager
import clipto.presentation.common.fragment.blocks.BlocksWithHintBottomSheetFragment
import clipto.store.folder.FolderRequest
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FolderDetailsFragment : BlocksWithHintBottomSheetFragment<FolderDetailsViewModel>() {

    override val viewModel: FolderDetailsViewModel by viewModels()

    companion object {
        private const val TAG = "FolderDetailsFragment"

        fun show(activity: FragmentActivity, request: FolderRequest) {
            activity.withSafeFragmentManager()?.let { fm ->
                FolderDetailsFragment()
                    .apply {
                        arguments = FolderDetailsViewModel.buildArgs(request)
                    }
                    .show(fm, TAG)
            }
        }
    }

}