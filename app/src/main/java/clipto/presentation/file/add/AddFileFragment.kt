package clipto.presentation.file.add

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import clipto.common.extensions.withSafeFragmentManager
import clipto.domain.FileType
import clipto.presentation.clip.list.ClipListAdapter
import clipto.presentation.common.fragment.attributed.AttributedObjectLayoutManager
import clipto.presentation.common.fragment.blocks.BlocksBottomSheetFragment
import clipto.presentation.file.add.data.AddFilesRequest
import clipto.presentation.file.add.data.FileData
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddFileFragment : BlocksBottomSheetFragment<AddFileViewModel>() {

    override val viewModel: AddFileViewModel by viewModels()

    private val clipsAdapter by lazy {
        ClipListAdapter(
            context = requireContext(),
            withMainState = viewModel.mainState,
            withClickHandler = { it, _ -> viewModel.onSaveAsAttachment(it) },
            withTextConstraint = { viewModel.getClipsSearchByText() }
        ).withListConfig(viewModel.getClipsListConfig())
    }

    override fun createLayoutManager(): RecyclerView.LayoutManager = AttributedObjectLayoutManager(context, viewModel::getScreenState)
    override fun createAdapter(adapter: RecyclerView.Adapter<*>): RecyclerView.Adapter<*> = ConcatAdapter(adapter, clipsAdapter)
    override fun getPeekHeight(): Float = AddFileViewModel.PEEK_HEIGHT
    override fun getBackConfirmTitle(): Int = R.string.clip_multiple_exit_without_save_title
    override fun getBackConfirmMessage(): Int = R.string.confirm_exit_edit_mode_description
    override fun getBackConfirmRequired(): Boolean = viewModel.getBackConfirmRequired()
    override fun canBeSwiped(): Boolean = false

    override fun bind(viewModel: AddFileViewModel) {
        super.bind(viewModel)
        viewModel.getClipsLive().observe(viewLifecycleOwner) {
            clipsAdapter.submitList(it)
        }
    }

    companion object {
        private const val TAG = "AddFileFragment"

        const val ATTR_FILES = "ATTR_FILES"

        fun show(activity: FragmentActivity, uri: Uri, fileType: FileType) {
            show(activity, listOf(FileData(uri, fileType)))
        }

        fun show(activity: FragmentActivity, files: List<FileData>) {
            activity.withSafeFragmentManager()?.let {
                AddFileFragment()
                    .apply {
                        arguments = Bundle().apply {
                            putSerializable(
                                ATTR_FILES, AddFilesRequest(
                                    files = files
                                )
                            )
                        }
                    }
                    .show(it, TAG)
            }
        }
    }
}