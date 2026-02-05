package clipto.presentation.common.dialog.select.date

import android.app.Application
import android.util.SparseArray
import androidx.lifecycle.MutableLiveData
import clipto.common.presentation.mvvm.ViewModel
import clipto.common.presentation.mvvm.lifecycle.SingleLiveData
import clipto.domain.TimePeriod
import clipto.extensions.getTitleRes
import clipto.presentation.blocks.SingleDateBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.common.recyclerview.BlockItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SelectDateDialogViewModel @Inject constructor(
    app: Application
) : ViewModel(app) {

    val dataMap: SparseArray<SelectDateDialogRequest> = SparseArray()

    fun getBlocksLive(data: SelectDateDialogRequest): SingleLiveData<List<BlockItem<SelectDateDialogFragment>>> {
        data.options = data.timePeriods.map {
            SelectDateDialogRequest.Option(
                model = it,
                title = string(it.getTitleRes()),
                checked = data.selection.model == it
            )
        }
        val blocksLive = SingleLiveData<List<BlockItem<SelectDateDialogFragment>>>()
        blocksLive.value = map(data, blocksLive)
        return blocksLive
    }

    fun onClosed(data: SelectDateDialogRequest) {
        data.onSelected.invoke(data.selection.normalize())
        dataMap.remove(data.id)
    }

    fun onClearValue(data: SelectDateDialogRequest) {
        data.selection = SelectDateDialogRequest.Selection()
        dismiss()
    }

    fun onClicked(block: OptionBlock) {
        val live = block.live
        val clickedOption = block.option.copy(checked = !block.option.checked)
        if (clickedOption.checked) {
            block.data.changeSelection(block.data.selection.copy(model = clickedOption.model))
        } else {
            block.data.changeSelection(block.data.selection.copy(model = null))
        }
        val newOptions = block.data.options.map { option ->
            if (option.model == clickedOption.model) {
                clickedOption
            } else {
                option.copy(checked = false)
            }
        }
        block.data.options = newOptions
        live.postValue(map(block.data, live))
    }

    private fun map(
        data: SelectDateDialogRequest,
        live: MutableLiveData<List<BlockItem<SelectDateDialogFragment>>>
    ): List<BlockItem<SelectDateDialogFragment>> {
        val items = mutableListOf<BlockItem<SelectDateDialogFragment>>()
        data.options.forEachIndexed { _, option ->
            if (data.selection.model == TimePeriod.CUSTOM_INTERVAL && option.model == data.selection.model) {
                items.add(
                    RangeBlock(
                        live = live,
                        data = data
                    )
                )
            } else if (data.selection.model == TimePeriod.CUSTOM_DATE && option.model == data.selection.model) {
                items.add(SingleDateBlock(
                    currentDate = { data.selection.dateFrom },
                    onDateChanged = { data.changeSelection(data.selection.copy(dateFrom = it)) }
                ))
            }
            val block = OptionBlock(
                live = live,
                viewModel = this,
                option = option,
                data = data
            )
            items.add(block)
        }
        if (items.last() is RangeBlock || items.last() is SingleDateBlock) {
            items.add(SpaceBlock(8))
        }
        return items
    }

}