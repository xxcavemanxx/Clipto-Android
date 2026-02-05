package clipto.presentation.common.dialog.select.options

import android.app.Application
import android.util.SparseArray
import androidx.lifecycle.MutableLiveData
import clipto.common.presentation.mvvm.ViewModel
import clipto.common.presentation.mvvm.lifecycle.SingleLiveData
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.dialog.select.value.SelectValueDialogRequest
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SelectOptionsDialogViewModel @Inject constructor(
        private val dialogState: DialogState,
        app: Application
) : ViewModel(app) {

    val dataMap: SparseArray<SelectOptionsDialogRequest> = SparseArray()

    fun getBlocksLive(data: SelectOptionsDialogRequest): SingleLiveData<List<BlockItem<SelectOptionsDialogFragment>>> {
        val blocksLive = SingleLiveData<List<BlockItem<SelectOptionsDialogFragment>>>()
        if (data.options.isEmpty()) {
            data.options = mutableListOf(SelectOptionsDialogRequest.Option().apply { editMode = true })
        }
        blocksLive.value = data.options.map { createBlock(data, it, blocksLive) }
        return blocksLive
    }

    fun onClosed(data: SelectOptionsDialogRequest) {
        val selected = data.options
                .filter { it.value != null || it.title != null }
                .map { it }
        data.onSelected.invoke(selected)
        dataMap.remove(data.id)
    }

    fun onAddOption(
            data: SelectOptionsDialogRequest,
            live: MutableLiveData<List<BlockItem<SelectOptionsDialogFragment>>>) {
        data.options.forEach { it.editMode = false }
        data.options = listOf(SelectOptionsDialogRequest.Option().apply { editMode = true }).plus(data.options)
        live.postValue(data.options.map { createBlock(data, it, live) })
    }

    fun onEditOption(
            data: SelectOptionsDialogRequest,
            option: SelectOptionsDialogRequest.Option,
            live: MutableLiveData<List<BlockItem<SelectOptionsDialogFragment>>>) {
        data.options.forEach { it.editMode = false }
        option.editMode = true
        live.postValue(data.options.map { createBlock(data, it, live) })
    }

    fun onSaveOption(
            data: SelectOptionsDialogRequest,
            option: SelectOptionsDialogRequest.Option,
            live: MutableLiveData<List<BlockItem<SelectOptionsDialogFragment>>>) {
        option.editMode = false
        live.postValue(data.options.map { createBlock(data, it, live) })
    }

    fun onDeleteOption(
            data: SelectOptionsDialogRequest,
            option: SelectOptionsDialogRequest.Option,
            live: MutableLiveData<List<BlockItem<SelectOptionsDialogFragment>>>) {
        var options = data.options.minus(option)
        if (options.isEmpty()) {
            options = options.plus(SelectOptionsDialogRequest.Option())
        }
        data.options = options
        live.postValue(options.map { createBlock(data, it, live) })
    }

    fun onSort(
            data: SelectOptionsDialogRequest,
            live: MutableLiveData<List<BlockItem<SelectOptionsDialogFragment>>>
    ) {
        val orderingRules =
                if (data.withTitle) {
                    listOf(
                            OrderingType.VALUE_ASC,
                            OrderingType.VALUE_DESC,
                            OrderingType.LABEL_ASC,
                            OrderingType.LABEL_DESC
                    )
                } else {
                    listOf(
                            OrderingType.VALUE_ASC,
                            OrderingType.VALUE_DESC
                    )
                }
        val options = mutableListOf<SelectValueDialogRequest.Option<OrderingType>>()
        orderingRules.forEach { type ->
            options.add(SelectValueDialogRequest.Option(
                    checked = false,
                    title = string(type.titleRes),
                    model = type
            ))
        }
        val request = SelectValueDialogRequest(
                title = string(R.string.select_option_ordering),
                withImmediateNotify = true,
                single = true,
                options = options,
                onSelected = {
                    val selectedType = it.firstOrNull() ?: return@SelectValueDialogRequest
                    data.options = when (selectedType) {
                        OrderingType.LABEL_ASC -> data.options.sortedBy { opt -> opt.title }
                        OrderingType.LABEL_DESC -> data.options.sortedByDescending { opt -> opt.title }
                        OrderingType.VALUE_ASC -> data.options.sortedBy { opt -> opt.value }
                        OrderingType.VALUE_DESC -> data.options.sortedByDescending { opt -> opt.value }
                    }
                    live.postValue(data.options.map { opt -> createBlock(data, opt, live) })
                }
        )
        dialogState.requestSelectValueDialog(request)
    }

    private fun createBlock(
            data: SelectOptionsDialogRequest,
            option: SelectOptionsDialogRequest.Option,
            live: MutableLiveData<List<BlockItem<SelectOptionsDialogFragment>>>
    ): BlockItem<SelectOptionsDialogFragment> {
        return if (option.editMode) {
            SelectOptionEditBlock(
                    live = live,
                    viewModel = this,
                    option = option,
                    data = data
            )
        } else {
            SelectOptionViewBlock(
                    live = live,
                    viewModel = this,
                    option = option,
                    data = data
            )
        }
    }

}