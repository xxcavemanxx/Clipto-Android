package clipto.presentation.common.dialog.select.options

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import clipto.common.extensions.setBottomSheetHeight
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.setVisibleOrGone
import clipto.common.extensions.withSafeFragmentManager
import clipto.common.presentation.mvvm.MvvmBottomSheetDialogFragment
import clipto.presentation.common.recyclerview.BlockListAdapter
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.dialog_select_options.*

@AndroidEntryPoint
class SelectOptionsDialogFragment : MvvmBottomSheetDialogFragment<SelectOptionsDialogViewModel>() {

    override val layoutResId: Int = R.layout.dialog_select_options
    override val viewModel: SelectOptionsDialogViewModel by activityViewModels()

    private val data: SelectOptionsDialogRequest? by lazy {
        val id = arguments?.getInt(ATTR_DATA_ID) ?: 0
        viewModel.dataMap.get(id)
    }

    lateinit var touchHelper: ItemTouchHelper

    override fun bind(viewModel: SelectOptionsDialogViewModel) {
        val dataRef = data
        if (dataRef == null) {
            dismissAllowingStateLoss()
            return
        }
        tvTitle.text = dataRef.title
        flContent.setBottomSheetHeight(noBackground = true)
        rvBlocks.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val blocksAdapter = BlockListAdapter(this)
        touchHelper = blocksAdapter.createTouchHelper(
            dataToSwap = { dataRef.options },
            canMove = { dataRef.enabled }
        )
        rvBlocks.adapter = blocksAdapter
        touchHelper.attachToRecyclerView(rvBlocks)

        val blocksLive = viewModel.getBlocksLive(dataRef)
        blocksLive.observe(viewLifecycleOwner) {
            ivAdd.setDebounceClickListener { viewModel.onAddOption(dataRef, blocksLive) }
            ivAdd.setVisibleOrGone(dataRef.enabled)

            ivSort.setVisibleOrGone(dataRef.enabled)
            ivSort.setDebounceClickListener { viewModel.onSort(dataRef, blocksLive) }

            blocksAdapter.submitList(it) {
                val indexOfEditBlock = it.indexOfFirst { it is SelectOptionEditBlock }
                if (indexOfEditBlock != -1) {
                    rvBlocks.scrollToPosition(indexOfEditBlock)
                } else {
                    rvBlocks.scrollToPosition(0)
                }
            }
        }

    }

    override fun onDestroyView() {
        data?.let { viewModel.onClosed(it) }
        super.onDestroyView()
    }

    companion object {

        private const val ATTR_DATA_ID = "attr_data_id"

        fun show(activity: FragmentActivity, data: SelectOptionsDialogRequest) {
            activity.withSafeFragmentManager()?.let { fm ->
                val viewModel = activity.viewModels<SelectOptionsDialogViewModel>().value
                viewModel.dataMap.put(data.id, data)
                SelectOptionsDialogFragment()
                    .apply {
                        arguments = Bundle().apply {
                            putInt(ATTR_DATA_ID, data.id)
                        }
                    }
                    .show(fm, "SelectOptionsDialogFragment")
            }
        }
    }

}