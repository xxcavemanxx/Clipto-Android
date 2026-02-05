package clipto.presentation.common.dialog.select.date

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import clipto.common.extensions.setBottomSheetHeight
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.withSafeFragmentManager
import clipto.common.presentation.mvvm.MvvmBottomSheetDialogFragment
import clipto.extensions.getActionIconColorHighlight
import clipto.extensions.getTextColorSecondary
import clipto.presentation.common.recyclerview.BlockListAdapter
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.dialog_select.*

@AndroidEntryPoint
class SelectDateDialogFragment : MvvmBottomSheetDialogFragment<SelectDateDialogViewModel>() {

    override val layoutResId: Int = R.layout.dialog_select
    override val viewModel: SelectDateDialogViewModel by activityViewModels()

    private val data: SelectDateDialogRequest? by lazy {
        val id = arguments?.getInt(ATTR_DATA_ID) ?: 0
        viewModel.dataMap.get(id)
    }

    override fun bind(viewModel: SelectDateDialogViewModel) {
        val dataRef = data
        if (dataRef == null) {
            dismissAllowingStateLoss()
            return
        }

        val ctx = requireContext()
        tvTitle.text = dataRef.title
        flContent.setBottomSheetHeight(noBackground = true)
        mbClearAll.setText(R.string.menu_clear)
        mbClearAll.setDebounceClickListener { viewModel.onClearValue(dataRef) }
        rvBlocks.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val blocksAdapter = BlockListAdapter(this)
        rvBlocks.adapter = blocksAdapter

        viewModel.getBlocksLive(dataRef).observe(viewLifecycleOwner) {
            val color = if (it.any { it is OptionBlock && it.option.checked }) ctx.getActionIconColorHighlight() else ctx.getTextColorSecondary()
            mbClearAll?.setTextColor(color)
            blocksAdapter.submitList(it)
        }
    }

    override fun onDestroyView() {
        data?.let { viewModel.onClosed(it) }
        super.onDestroyView()
    }

    companion object {

        private const val ATTR_DATA_ID = "attr_data_id"

        fun show(activity: FragmentActivity, data: SelectDateDialogRequest) {
            activity.withSafeFragmentManager()?.let { fm ->
                val viewModel = activity.viewModels<SelectDateDialogViewModel>().value
                viewModel.dataMap.put(data.id, data)
                SelectDateDialogFragment()
                    .apply {
                        arguments = Bundle().apply {
                            putInt(ATTR_DATA_ID, data.id)
                        }
                    }
                    .show(fm, "SelectDateDialogFragment")
            }
        }
    }

}