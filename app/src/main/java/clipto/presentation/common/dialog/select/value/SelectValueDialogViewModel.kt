package clipto.presentation.common.dialog.select.value

import android.app.Application
import android.util.SparseArray
import androidx.lifecycle.MutableLiveData
import clipto.common.presentation.mvvm.ViewModel
import clipto.common.presentation.mvvm.lifecycle.SingleLiveData
import clipto.presentation.blocks.PrimaryButtonBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.blocks.TextInputLayoutBlock
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SelectValueDialogViewModel @Inject constructor(
    app: Application
) : ViewModel(app) {

    val dataMap: SparseArray<SelectValueDialogRequest<*>> = SparseArray()

    fun <T> createInputsLive(
        data: SelectValueDialogRequest<T>,
        actionsLive: MutableLiveData<List<BlockItem<SelectValueDialogFragment>>>,
        blocksLive: MutableLiveData<List<BlockItem<SelectValueDialogFragment>>>,
        inputBlocksLive: MutableLiveData<List<BlockItem<SelectValueDialogFragment>>> = SingleLiveData()
    ): MutableLiveData<List<BlockItem<SelectValueDialogFragment>>> {
        val inputBlocks = mutableListOf<BlockItem<SelectValueDialogFragment>>()
        inputBlocks.add(SpaceBlock(16))
        inputBlocks.add(
            TextInputLayoutBlock(
                text = data.filteredByText,
                changedTextProvider = { data.filteredByText },
                hint = string(R.string.clip_add_snippet_new_hint),
                onTextChanged = { text ->
                    val filteredByText = text?.toString()
                    data.filteredByText = filteredByText
                    createActionsLive(data, actionsLive)
                    createBlocksLive(data, blocksLive)
                    null
                }
            )
        )
        inputBlocks.add(SpaceBlock(heightInDp = 4))
        inputBlocksLive.postValue(inputBlocks)
        return inputBlocksLive
    }

    fun <T> createActionsLive(
        data: SelectValueDialogRequest<T>,
        actionsLive: MutableLiveData<List<BlockItem<SelectValueDialogFragment>>> = SingleLiveData()
    ): MutableLiveData<List<BlockItem<SelectValueDialogFragment>>> {
        val actionsBlocks = mutableListOf<BlockItem<SelectValueDialogFragment>>()
        val filteredByText = data.filteredByText
        if (!filteredByText.isNullOrBlank() && !data.options.any { it.uid == filteredByText }) {
            actionsBlocks.add(SpaceBlock(12))
            actionsBlocks.add(
                PrimaryButtonBlock(
                    titleRes = R.string.clip_button_tag_create,
                    clickListener = {
                        data.onManualInput?.invoke(data)
                    }
                )
            )
            actionsBlocks.add(SpaceBlock(4))
        }
        actionsLive.postValue(actionsBlocks)
        return actionsLive
    }

    fun <T> createBlocksLive(
        data: SelectValueDialogRequest<T>,
        blocksLive: MutableLiveData<List<BlockItem<SelectValueDialogFragment>>> = SingleLiveData()
    ): MutableLiveData<List<BlockItem<SelectValueDialogFragment>>> {
        blocksLive.postValue(map(data, blocksLive))
        return blocksLive
    }

    fun <T> onClosed(data: SelectValueDialogRequest<T>) {
        val selected = data.options.filter { it.checked }.map { it.model }
        data.onSelected.invoke(selected)
        dataMap.remove(data.id)
    }

    fun <T> onClearValues(data: SelectValueDialogRequest<T>) {
        var dismiss = true
        if (data.withClearAllCustomListener != null) {
            val selected = data.options.filter { it.checked }.map { it.model }
            dismiss = data.withClearAllCustomListener.invoke(selected)
        } else {
            data.options = data.options.map { it.copy(checked = false) }
        }
        if (dismiss) {
            dismiss()
        }
    }

    fun <T> onClicked(block: OptionBlock<T>) {
        val live = block.live
        val single = block.data.single
        val clickedOption = block.option.copy(checked = single || !block.option.checked)
        val newOptions = block.data.options.map { option ->
            if (option.model == clickedOption.model) {
                clickedOption
            } else {
                if (single && clickedOption.checked) {
                    option.copy(checked = false)
                } else {
                    option
                }
            }
        }
        block.data.options = newOptions
        live.postValue(map(block.data, live))
        if (block.data.withImmediateNotify) {
            val selected = block.data.options.filter { it.checked }.map { it.model }
            if (block.data.single) {
                dismiss()
            } else {
                block.data.onSelected.invoke(selected)
            }
        }
    }

    private fun <T> map(
        data: SelectValueDialogRequest<T>,
        live: MutableLiveData<List<BlockItem<SelectValueDialogFragment>>>
    ): List<BlockItem<SelectValueDialogFragment>> {
        val text = data.filteredByText
        return data
            .options
            .filter { text.isNullOrBlank() || it.uid?.contains(text, ignoreCase = true) == true }
            .map { option ->
                OptionBlock(
                    live = live,
                    viewModel = this,
                    option = option,
                    data = data,
                    highlight = data.filteredByText
                )
            }
    }

}