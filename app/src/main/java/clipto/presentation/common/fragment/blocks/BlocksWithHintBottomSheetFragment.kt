package clipto.presentation.common.fragment.blocks

import android.text.Editable
import android.text.InputFilter
import android.view.View
import androidx.annotation.CallSuper
import clipto.common.extensions.*
import clipto.common.presentation.text.TextWatcherAdapter
import clipto.extensions.TextTypeExt
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.fragment_blocks_with_hint_bottom_sheet.*

abstract class BlocksWithHintBottomSheetFragment<VM : BlocksWithHintViewModel> : BlocksBottomSheetFragment<VM>() {

    override val layoutResId: Int = R.layout.fragment_blocks_with_hint_bottom_sheet
    override fun getContentView(): View = llContent

    @CallSuper
    override fun bind(viewModel: VM) {
        super.bind(viewModel)
        viewModel.getHintLive().observe(viewLifecycleOwner) { hint ->
            val editMode = hint.editMode
            val hideHint = hint.hideHint
            tvHint?.editableMultiLine(editMode)
            tvHint?.isCursorVisible = editMode
            tvHint?.isReadOnly = !editMode
            tvHint?.weakTextWatcherOne?.watcher = null
            mbHint?.setVisibleOrGone(!editMode)
            if (editMode) {
                TextTypeExt.TEXT_PLAIN.apply(tvHint, hint.value)
                tvHint?.maxLines = 20
                llHint?.animateScale(true)
                tvHint?.weakTextWatcherOne?.watcher = object : TextWatcherAdapter() {
                    override fun afterTextChanged(s: Editable?) {
                        hint.onChanged.invoke(s?.toString())
                    }
                }
            } else {
                if (!hideHint) {
                    val tip = hint.title.trimSpaces()
                    if (tip != null) {
                        tvHint?.maxLines = Integer.MAX_VALUE
                        tvHint?.scrollTo(0, 0)
                        TextTypeExt.MARKDOWN.apply(tvHint, tip)
                        llHint?.animateScale(true)
                    } else {
                        llHint?.animateScale(false)
                    }
                } else {
                    llHint?.animateScale(false)
                }
            }
        }
        tvHint.filters = arrayOf(InputFilter.LengthFilter(viewModel.appConfig.maxLengthDescription()))
        mbHint.setDebounceClickListener { viewModel.onHideHint() }
    }

    override fun onDestroyView() {
        viewModel.onClosed()
        super.onDestroyView()
    }

}