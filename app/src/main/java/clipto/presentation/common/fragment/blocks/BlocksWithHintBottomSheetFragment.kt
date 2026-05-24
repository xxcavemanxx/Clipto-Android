package clipto.presentation.common.fragment.blocks

import com.wb.clipboard.databinding.FragmentBlocksWithHintBottomSheetBinding
import android.text.Editable
import android.text.InputFilter
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import clipto.common.extensions.*
import clipto.common.presentation.text.TextWatcherAdapter
import clipto.extensions.TextTypeExt
import com.wb.clipboard.R

abstract class BlocksWithHintBottomSheetFragment<VM : BlocksWithHintViewModel> : BlocksBottomSheetFragment<VM>() {

    override val binding: FragmentBlocksWithHintBottomSheetBinding get() {
        val b = _binding
        if (b is FragmentBlocksWithHintBottomSheetBinding) return b
        return FragmentBlocksWithHintBottomSheetBinding.bind(requireView())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentBlocksWithHintBottomSheetBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
    }

    override val layoutResId: Int = R.layout.fragment_blocks_with_hint_bottom_sheet
    override fun getContentView(): View = binding.llContent

    @CallSuper
    override fun bind(viewModel: VM) {
        super.bind(viewModel)
        viewModel.getHintLive().observe(viewLifecycleOwner) { hint ->
            val editMode = hint.editMode
            val hideHint = hint.hideHint
            binding.tvHint?.editableMultiLine(editMode)
            binding.tvHint?.isCursorVisible = editMode
            binding.tvHint?.isReadOnly = !editMode
            binding.tvHint?.weakTextWatcherOne?.watcher = null
            binding.mbHint?.setVisibleOrGone(!editMode)
            if (editMode) {
                TextTypeExt.TEXT_PLAIN.apply(binding.tvHint, hint.value)
                binding.tvHint?.maxLines = 20
                binding.llHint?.animateScale(true)
                binding.tvHint?.weakTextWatcherOne?.watcher = object : TextWatcherAdapter() {
                    override fun afterTextChanged(s: Editable?) {
                        hint.onChanged.invoke(s?.toString())
                    }
                }
            } else {
                if (!hideHint) {
                    val tip = hint.title.trimSpaces()
                    if (tip != null) {
                        binding.tvHint?.maxLines = Integer.MAX_VALUE
                        binding.tvHint?.scrollTo(0, 0)
                        TextTypeExt.MARKDOWN.apply(binding.tvHint, tip)
                        binding.llHint?.animateScale(true)
                    } else {
                        binding.llHint?.animateScale(false)
                    }
                } else {
                    binding.llHint?.animateScale(false)
                }
            }
        }
        binding.tvHint.filters = arrayOf(InputFilter.LengthFilter(viewModel.appConfig.maxLengthDescription()))
        binding.mbHint.setDebounceClickListener { viewModel.onHideHint() }
    }

    override fun onDestroyView() {
        _binding = null
        viewModel.onClosed()
        super.onDestroyView()
    }

}
