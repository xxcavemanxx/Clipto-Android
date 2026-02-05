package clipto.dynamic.presentation.text

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
import kotlinx.android.synthetic.main.fragment_dynamic_text.*

@AndroidEntryPoint
class DynamicTextFragment : MvvmBottomSheetDialogFragment<DynamicTextViewModel>() {

    override val layoutResId: Int = R.layout.fragment_dynamic_text
    override val viewModel: DynamicTextViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialogExt()
    }

    override fun bind(viewModel: DynamicTextViewModel) {
        val ctx = requireContext()
        flContent.setBottomSheetHeight(hideable = false, noBackground = true)
        mbApply.setDebounceClickListener { viewModel.onApply() }
        tvTitle.setDebounceClickListener { viewModel.onShowHint() }
        rvBlocks.layoutManager = object : LinearLayoutManager(context, VERTICAL, false) {
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
        rvBlocks.adapter = blocksAdapter

        viewModel.configLive.observe(viewLifecycleOwner) {
            mbApply.setText(it.request.config.actionType.getActionLabelRes())
            val title = it.request.config.title.toNullIfEmpty()
            val textColor = if (title != null) ctx.getTextColorPrimary() else ctx.getTextColorSecondary()
            tvTitle?.text = title ?: ctx.getString(R.string.clip_hint_title)
            tvTitle?.setTextColor(textColor)
            if (it.viewMode == ViewMode.TEXT) {
                flContent?.hideKeyboard()
            }
        }

        viewModel.blocksLive.observe(viewLifecycleOwner) {
            blocksAdapter.submitList(it)
        }
    }

    override fun onDestroyView() {
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