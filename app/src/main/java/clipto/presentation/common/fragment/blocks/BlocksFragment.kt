package clipto.presentation.common.fragment.blocks

import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import clipto.common.extensions.hideKeyboard
import clipto.common.presentation.mvvm.MvvmFragment
import clipto.common.presentation.mvvm.base.FragmentBackButtonListener
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.presentation.common.recyclerview.BlockListAdapter
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.fragment_blocks.*

abstract class BlocksFragment<VM : BlocksViewModel> : MvvmFragment<VM>(),
    FragmentBackButtonListener {

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
        toolbar.title = getTitle()
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener {
            activity?.currentFocus.hideKeyboard()
            onFragmentBackPressed()
        }
        rvBlocks.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val blocksAdapter = BlockListAdapter<Fragment>(this)
        rvBlocks.adapter = blocksAdapter


        viewModel.dismissLive.observe(viewLifecycleOwner) {
            navigateUp()
        }

        viewModel.getBlocksLive().observe(viewLifecycleOwner) { data ->
            blocksAdapter.submitList(data.blocks) {
                if (data.scrollToTop) {
                    rvBlocks.scrollToPosition(0)
                }
            }
        }
    }


}