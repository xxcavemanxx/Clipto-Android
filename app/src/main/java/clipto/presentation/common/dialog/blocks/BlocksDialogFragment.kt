package clipto.presentation.common.dialog.blocks

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import clipto.common.extensions.withSafeFragmentManager
import clipto.presentation.common.fragment.blocks.BlocksBottomSheetFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlocksDialogFragment : BlocksBottomSheetFragment<BlocksDialogViewModel>() {

    override val viewModel: BlocksDialogViewModel by viewModels()

    private val requestViewModel: BlocksDialogRequestViewModel by activityViewModels()

    private val requestId by lazy { arguments?.getInt(ATTR_DATA_ID) ?: 0 }

    private val request by lazy { requestViewModel.getRequest(requestId) }

    override fun isBackPressConsumed(): Boolean = request?.onBackConsumed?.invoke(viewModel) == true

    override fun createAdapter(adapter: RecyclerView.Adapter<*>): RecyclerView.Adapter<*> =
        request?.onCreateAdapter?.invoke(viewModel, this, adapter) ?: super.createAdapter(adapter)

    override fun bind(viewModel: BlocksDialogViewModel) {
        super.bind(viewModel)
        val requestRef = request
        if (requestRef == null) {
            dismissAllowingStateLoss()
        } else {
            requestRef.onReady(viewModel)
        }
    }

    override fun onDestroyView() {
        requestViewModel.removeRequest(requestId)
        request?.onDestroy?.invoke(viewModel)
        super.onDestroyView()
    }

    companion object {

        private const val TAG = "BlockedDialogFragment"

        private const val ATTR_DATA_ID = "ATTR_DATA_ID"

        fun show(activity: FragmentActivity, request: BlocksDialogRequest) {
            activity.withSafeFragmentManager()?.let { fm ->
                val viewModel = activity.viewModels<BlocksDialogRequestViewModel>().value
                viewModel.setRequest(request)
                BlocksDialogFragment()
                    .apply {
                        arguments = Bundle().apply {
                            putInt(ATTR_DATA_ID, request.id)
                        }
                    }
                    .show(fm, TAG)
            }
        }
    }

}