package clipto.presentation.common.fragment.blocks

import com.wb.clipboard.databinding.FragmentBlocksBottomSheetBinding
import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.annotation.CallSuper
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clipto.common.extensions.addOnKeyboardStateListener
import clipto.common.extensions.hideKeyboard
import clipto.common.extensions.setBottomSheetHeight
import clipto.common.presentation.mvvm.MvvmBottomSheetDialogFragment
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.presentation.common.recyclerview.BlockListAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.wb.clipboard.R

abstract class BlocksBottomSheetFragment<VM : BlocksViewModel> : MvvmBottomSheetDialogFragment<VM>() {

    protected var _binding: Any? = null
    protected open val binding: androidx.viewbinding.ViewBinding get() {
        val b = _binding
        if (b is androidx.viewbinding.ViewBinding) return b
        val newBinding = FragmentBlocksBottomSheetBinding.bind(requireView())
        _binding = newBinding
        return newBinding
    }

    protected val rvBlocks: androidx.recyclerview.widget.RecyclerViewExt get() = requireView().findViewById(R.id.rvBlocks)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (layoutResId == R.layout.fragment_blocks_bottom_sheet) {
            _binding = FragmentBlocksBottomSheetBinding.bind(view)
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override val layoutResId: Int = R.layout.fragment_blocks_bottom_sheet

    protected open fun createLayoutManager(): RecyclerView.LayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    protected open fun createAdapter(adapter: RecyclerView.Adapter<*>): RecyclerView.Adapter<*> = adapter
    protected open fun getBackConfirmRequired(): Boolean = true
    protected open fun isBackPressConsumed(): Boolean = false
    protected open fun getBackConfirmTitle(): Int = 0
    protected open fun getBackConfirmMessage(): Int = 0
    protected open fun canBeSwiped(): Boolean = true
    protected open fun getPeekHeight(): Float = 0.75f
    protected open fun getTitle(): String? = null
    protected open fun getContentView(): View = requireView().findViewById(R.id.flContent)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialogExt()
    }

    inner class BottomSheetDialogExt : BottomSheetDialog(requireContext(), theme) {

        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            return try {
                super.dispatchKeyEvent(event)
            } catch (e: Exception) {
                false
            }
        }

        override fun onBackPressed() {
            val titleRes = getBackConfirmTitle()
            val messageRes = getBackConfirmMessage()
            if (isBackPressConsumed()) {
                // do nothing?
            } else if (getBackConfirmRequired() && (titleRes != 0 || messageRes != 0)) {
                viewModel.dialogState.showConfirm(
                    ConfirmDialogData(
                        iconRes = R.drawable.ic_attention,
                        title = getString(titleRes),
                        description = messageRes.takeIf { it != 0 }?.let { getString(it) } ?: "",
                        confirmActionTextRes = R.string.button_yes,
                        onConfirmed = { cancel() },
                        cancelActionTextRes = R.string.button_no
                    )
                )
            } else {
                super.onBackPressed()
            }
        }
    }

    @CallSuper
    override fun bind(viewModel: VM) {
        getContentView().setBottomSheetHeight(
            noBackground = true,
            hideable = canBeSwiped(),
            height = getPeekHeight()
        )

        rvBlocks.layoutManager = createLayoutManager()
        val blocksAdapter = BlockListAdapter<Fragment>(this)
        rvBlocks.adapter = createAdapter(blocksAdapter)

        val defaultPadding = rvBlocks.paddingBottom
        rvBlocks.addOnKeyboardStateListener { visible, _, keyboardHeight ->
            viewModel.onShowHideKeyboard(visible)
            val padding = if (visible) defaultPadding + keyboardHeight else rvBlocks.paddingBottom
            rvBlocks.updatePadding(bottom = padding)
        }

        viewModel.showHideKeyboard.observe(viewLifecycleOwner) {
            if (!it) {
                rvBlocks.hideKeyboard()
            }
        }

        viewModel.dismissLive.observe(viewLifecycleOwner) {
            dismissAllowingStateLoss()
        }

        viewModel.getBlocksLive().observe(viewLifecycleOwner) { data ->
            blocksAdapter.submitList(data.blocks) {
                if (data.scrollToTop) {
                    rvBlocks.scrollToPosition(0)
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        viewModel.onClosed()
        super.onDestroyView()
    }


}
