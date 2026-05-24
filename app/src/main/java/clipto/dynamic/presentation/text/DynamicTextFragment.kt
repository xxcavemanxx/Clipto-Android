package clipto.dynamic.presentation.text

import com.wb.clipboard.databinding.FragmentDynamicTextBinding
import android.app.Dialog
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clipto.common.extensions.*
import clipto.common.presentation.mvvm.MvvmBottomSheetDialogFragment
import clipto.dynamic.presentation.text.model.ViewMode
import clipto.extensions.getActionLabelRes
import clipto.extensions.getTextColorPrimary
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.presentation.common.recyclerview.BlockListAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DynamicTextFragment : MvvmBottomSheetDialogFragment<DynamicTextViewModel>() {

    
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDynamicTextBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
    }
private var _binding: FragmentDynamicTextBinding? = null
    private val binding get() = _binding!!
override val layoutResId: Int = R.layout.fragment_dynamic_text
    override val viewModel: DynamicTextViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialogExt()
    }

    override fun bind(viewModel: DynamicTextViewModel) {
        val ctx = requireContext()
        binding.flContent.setBottomSheetHeight(hideable = false, noBackground = true)
        binding.mbApply.setDebounceClickListener { viewModel.onApply() }
        binding.tvTitle.setDebounceClickListener { viewModel.onShowHint() }
        binding.rvBlocks.layoutManager = object : LinearLayoutManager(context, VERTICAL, false) {
            override fun requestChildRectangleOnScreen(
                parent: RecyclerView,
                child: View,
                rect: Rect,
                immediate: Boolean,
                focusedChildVisible: Boolean
            ): Boolean {
                return false
            }
        }
        val blocksAdapter = BlockListAdapter<Fragment>(this)
        binding.rvBlocks.adapter = blocksAdapter

        viewModel.configLive.observe(viewLifecycleOwner) {
            binding.mbApply.setText(it.request.config.actionType.getActionLabelRes())
            val title = it.request.config.title.toNullIfEmpty()
            val textColor = if (title != null) ctx.getTextColorPrimary() else ctx.getTextColorSecondary()
            binding.tvTitle?.text = title ?: ctx.getString(R.string.clip_hint_title)
            binding.tvTitle?.setTextColor(textColor)
            if (it.viewMode == ViewMode.TEXT) {
                binding.flContent?.hideKeyboard()
            }
        }

        viewModel.blocksLive.observe(viewLifecycleOwner) {
            blocksAdapter.submitList(it)
        }
    }

    override fun onDestroyView() {
        _binding = null
        viewModel.onClosed()
        super.onDestroyView()
    }

    inner class BottomSheetDialogExt : BottomSheetDialog(requireContext(), theme) {
        override fun onBackPressed() {
            val titleRes = R.string.clip_multiple_exit_without_save_title
            val messageRes = R.string.confirm_exit_edit_mode_description
            if (titleRes != 0 || messageRes != 0) {
                viewModel.dialogState.showConfirm(
                    ConfirmDialogData(
                        iconRes = R.drawable.ic_attention,
                        title = getString(titleRes),
                        description = getString(messageRes),
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

    companion object {
        fun show(activity: FragmentActivity) {
            activity.withSafeFragmentManager()?.let { fm ->
                DynamicTextFragment().show(fm, "DynamicTextFragment")
            }
        }
    }

}
