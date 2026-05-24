package clipto.presentation.common.fragment.blocks

import android.view.View
import android.os.Bundle
import com.wb.clipboard.databinding.FragmentBlocksBinding
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import clipto.common.extensions.hideKeyboard
import clipto.common.presentation.mvvm.MvvmFragment
import clipto.common.presentation.mvvm.base.FragmentBackButtonListener
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.presentation.common.recyclerview.BlockListAdapter
import com.wb.clipboard.R

abstract class BlocksFragment<VM : BlocksViewModel> : MvvmFragment<VM>(),
    FragmentBackButtonListener {

    private var _binding: FragmentBlocksBinding? = null
    val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentBlocksBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
    }

    override val layoutResId: Int = R.layout.fragment_blocks

    protected open fun getBackConfirmTitle(): Int = 0
    protected open fun getBackConfirmMessage(): Int = 0
    protected open fun getTitle(): String? = null

    override fun onFragmentBackPressed(): Boolean {
        val titleRes = getBackConfirmTitle()
        val messageRes = getBackConfirmMessage()
        if (titleRes != 0 && messageRes != 0) {
            viewModel.dialogState.showConfirm(
                ConfirmDialogData(
                    iconRes = R.drawable.ic_attention,
                    title = getString(titleRes),
                    description = getString(messageRes),
                    confirmActionTextRes = R.string.button_yes,
                    onConfirmed = { navigateUp() },
                    cancelActionTextRes = R.string.button_no
                )
            )
            return true
        }
        return false
    }

    @CallSuper
    override fun bind(viewModel: VM) {
        binding.toolbar.title = getTitle()
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbar.setNavigationOnClickListener {
            activity?.currentFocus.hideKeyboard()
            onFragmentBackPressed()
        }
        binding.rvBlocks.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val blocksAdapter = BlockListAdapter<Fragment>(this)
        binding.rvBlocks.adapter = blocksAdapter


        viewModel.dismissLive.observe(viewLifecycleOwner) {
            navigateUp()
        }

        viewModel.getBlocksLive().observe(viewLifecycleOwner) { data ->
            blocksAdapter.submitList(data.blocks) {
                if (data.scrollToTop) {
                    binding.rvBlocks.scrollToPosition(0)
                }
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
