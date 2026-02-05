package clipto.dynamic.presentation.field

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import clipto.common.extensions.setBottomSheetHeight
import clipto.common.extensions.withSafeFragmentManager
import clipto.common.presentation.mvvm.MvvmBottomSheetDialogFragment
import clipto.presentation.common.recyclerview.BlockListAdapter
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_dynamic_field.*

@AndroidEntryPoint
class DynamicFieldFragment : MvvmBottomSheetDialogFragment<DynamicFieldViewModel>() {

    override val layoutResId: Int = R.layout.fragment_dynamic_field

    override val viewModel: DynamicFieldViewModel by viewModels()

    override fun bind(viewModel: DynamicFieldViewModel) {
        contentView.setBottomSheetHeight(noBackground = true)

        // blocks
        rvBlocks.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val blocksAdapter = BlockListAdapter<Fragment>(this)
        rvBlocks.adapter = blocksAdapter

        viewModel.blocksLive.observe(viewLifecycleOwner) {
            blocksAdapter.submitList(it)
        }
    }

    override fun onDestroyView() {
        viewModel.onClosed()
        super.onDestroyView()
    }

    companion object {
        fun show(activity: FragmentActivity) {
            activity.withSafeFragmentManager()?.let { fm ->
                DynamicFieldFragment().show(fm, "DynamicFieldFragment")
            }
        }
    }

}