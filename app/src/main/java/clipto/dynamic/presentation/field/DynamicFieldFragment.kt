package clipto.dynamic.presentation.field
import android.view.View
import android.os.Bundle

import com.wb.clipboard.databinding.FragmentDynamicFieldBinding
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

@AndroidEntryPoint
class DynamicFieldFragment : MvvmBottomSheetDialogFragment<DynamicFieldViewModel>() {

    
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDynamicFieldBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
    }
private var _binding: FragmentDynamicFieldBinding? = null
    private val binding get() = _binding!!
override val layoutResId: Int = R.layout.fragment_dynamic_field

    override val viewModel: DynamicFieldViewModel by viewModels()

    override fun bind(viewModel: DynamicFieldViewModel) {
        binding.contentView.setBottomSheetHeight(noBackground = true)

        // blocks
        binding.rvBlocks.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val blocksAdapter = BlockListAdapter<Fragment>(this)
        binding.rvBlocks.adapter = blocksAdapter

        viewModel.blocksLive.observe(viewLifecycleOwner) {
            blocksAdapter.submitList(it)
        }
    }

    override fun onDestroyView() {
        _binding = null
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
