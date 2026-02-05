package clipto.presentation.clip.add

import android.content.Context
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import clipto.common.extensions.findActivity
import clipto.common.extensions.withSafeFragmentManager
import clipto.presentation.clip.add.data.AddClipRequest
import clipto.presentation.clip.list.ClipListAdapter
import clipto.presentation.common.fragment.attributed.AttributedObjectLayoutManager
import clipto.presentation.common.fragment.blocks.BlocksBottomSheetFragment
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddClipFragment : BlocksBottomSheetFragment<AddClipViewModel>() {

    override val viewModel: AddClipViewModel by viewModels()

    private val clipsAdapter by lazy {
        ClipListAdapter(
            context = requireContext(),
            withMainState = viewModel.mainState,
            withClickHandler = { it, _ -> viewModel.onInsertInto(it, requestSettings = false) },
            withLongClickHandler = { it, _ -> viewModel.onInsertInto(it, requestSettings = true) },
            withTextConstraint = { viewModel.getClipsSearchByText() }
        ).withListConfig(viewModel.getClipsListConfig())
    }

    override fun createLayoutManager(): RecyclerView.LayoutManager = AttributedObjectLayoutManager(context, viewModel::getScreenState)
    override fun createAdapter(adapter: RecyclerView.Adapter<*>): RecyclerView.Adapter<*> = ConcatAdapter(adapter, clipsAdapter)
    override fun getPeekHeight(): Float = AddClipViewModel.PEEK_HEIGHT
    override fun getBackConfirmTitle(): Int = R.string.clip_multiple_exit_without_save_title
    override fun getBackConfirmMessage(): Int = R.string.confirm_exit_edit_mode_description
    override fun getBackConfirmRequired(): Boolean = viewModel.isClipChanged()
    override fun canBeSwiped(): Boolean = false

    override fun bind(viewModel: AddClipViewModel) {
        super.bind(viewModel)
        viewModel.getClipsLive().observe(viewLifecycleOwner) {
            clipsAdapter.submitList(it)
        }
    }

    companion object {
        private const val TAG = "AddClipFragment"

        const val ATTR_REQUEST = "ATTR_REQUEST"

        fun show(activity: FragmentActivity, request: AddClipRequest) {
            activity.withSafeFragmentManager()?.let {
                AddClipFragment()
                    .apply {
                        arguments = bundleOf(
                            ATTR_REQUEST to request
                        )
                    }
                    .show(it, TAG)
            }
        }

        fun show(context: Context, request: AddClipRequest) {
            context.findActivity()?.let { show(it, request) }
        }
    }
}