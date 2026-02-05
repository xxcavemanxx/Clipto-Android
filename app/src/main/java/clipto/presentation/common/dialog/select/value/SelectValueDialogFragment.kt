package clipto.presentation.common.dialog.select.value

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import clipto.common.extensions.setBottomSheetHeight
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.setVisibleOrGone
import clipto.common.extensions.withSafeFragmentManager
import clipto.common.misc.AndroidUtils
import clipto.common.misc.Units
import clipto.common.presentation.mvvm.MvvmBottomSheetDialogFragment
import clipto.extensions.getActionIconColorHighlight
import clipto.extensions.getTextColorSecondary
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.common.recyclerview.BlockListAdapter
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.dialog_select.*

@AndroidEntryPoint
class SelectValueDialogFragment : MvvmBottomSheetDialogFragment<SelectValueDialogViewModel>() {

    override val layoutResId: Int = R.layout.dialog_select
    override val viewModel: SelectValueDialogViewModel by activityViewModels()

    private val data: SelectValueDialogRequest<*>? by lazy {
        val id = arguments?.getInt(ATTR_DATA_ID) ?: 0
        viewModel.dataMap.get(id)
    }

    override fun bind(viewModel: SelectValueDialogViewModel) {
        val dataRef = data
        if (dataRef == null) {
            dismissAllowingStateLoss()
            return
        }

        val ctx = requireContext()
        tvTitle.text = dataRef.title
        flContent.setBottomSheetHeight(noBackground = true)
        mbClearAll.setVisibleOrGone(dataRef.withClearAll)
        if (dataRef.withClearAllCustomTitleRes != 0) {
            mbClearAll.setText(dataRef.withClearAllCustomTitleRes)
        } else {
            mbClearAll.setText(if (dataRef.single) R.string.menu_clear else R.string.menu_clear_all)
        }
        mbClearAll.setDebounceClickListener { viewModel.onClearValues(dataRef) }

        rvBlocks.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val blocksAdapter = BlockListAdapter(this)

        val blocksLive = viewModel.createBlocksLive(dataRef)

        if (dataRef.withManualInput) {
            val actionsLive = viewModel.createActionsLive(dataRef)
            val inputsLive = viewModel.createInputsLive(dataRef, actionsLive, blocksLive)
            val inputsAdapter = BlockListAdapter(this)
            val actionsAdapter = BlockListAdapter(this)
            val bottomSpaceAdapter = BlockListAdapter(this)
            val height = AndroidUtils.getDisplaySize(ctx).y - Units.DP.toPx(212f)

            rvBlocks.adapter = ConcatAdapter(inputsAdapter, actionsAdapter, blocksAdapter, bottomSpaceAdapter)

            inputsLive.observe(viewLifecycleOwner) {
                inputsAdapter.submitList(it)
                bottomSpaceAdapter.submitList(listOf(SpaceBlock(Units.PX.toDp(height).toInt())))
            }
            actionsLive.observe(viewLifecycleOwner) { actionsAdapter.submitList(it) }

            dataRef.requestRefresh = {
                viewModel.createBlocksLive(dataRef, blocksLive)
                viewModel.createActionsLive(dataRef, actionsLive)
                viewModel.createInputsLive(dataRef, actionsLive, blocksLive, inputsLive)
            }
        } else {
            rvBlocks.adapter = blocksAdapter

            dataRef.requestRefresh = {
                viewModel.createBlocksLive(dataRef, blocksLive)
            }
        }

        blocksLive.observe(viewLifecycleOwner) { blocks ->
            if (!dataRef.withClearAllAlternativeLogic) {
                val color = if (blocks.any { it is OptionBlock<*> && it.option.checked }) ctx.getActionIconColorHighlight() else ctx.getTextColorSecondary()
                mbClearAll?.setTextColor(color)
            }
            blocksAdapter.submitList(blocks)
        }
    }

    override fun onDestroyView() {
        data?.let { viewModel.onClosed(it) }
        super.onDestroyView()
    }

    companion object {

        private const val ATTR_DATA_ID = "attr_data_id"

        fun <T> show(activity: FragmentActivity, data: SelectValueDialogRequest<T>) {
            activity.withSafeFragmentManager()?.let { fm ->
                val viewModel = activity.viewModels<SelectValueDialogViewModel>().value
                viewModel.dataMap.put(data.id, data)
                SelectValueDialogFragment()
                    .apply {
                        arguments = Bundle().apply {
                            putInt(ATTR_DATA_ID, data.id)
                        }
                    }
                    .show(fm, "SelectValueDialogFragment")
            }
        }
    }

}