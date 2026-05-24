package clipto.presentation.main.actions
import android.view.View
import android.os.Bundle

import com.wb.clipboard.databinding.FragmentMainActionsBinding
import androidx.fragment.app.viewModels
import clipto.common.extensions.setDebounceClickListener
import clipto.presentation.common.fragment.blocks.BlocksBottomSheetFragment
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActionsFragment : BlocksBottomSheetFragment<MainActionsViewModel>() {

    
    
    override val binding: FragmentMainActionsBinding get() {
        val b = _binding
        if (b is FragmentMainActionsBinding) return b
        return FragmentMainActionsBinding.bind(requireView())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentMainActionsBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
    }

    override val layoutResId: Int = R.layout.fragment_main_actions

    override val viewModel: MainActionsViewModel by viewModels()

    override fun bind(viewModel: MainActionsViewModel) {
        super.bind(viewModel)
        binding.ivMore.setDebounceClickListener {
            viewModel.onSettings()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
