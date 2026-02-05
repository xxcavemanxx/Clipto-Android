package clipto.presentation.common.dialog.blocks

import android.util.SparseArray
import androidx.lifecycle.ViewModel

class BlocksDialogRequestViewModel : ViewModel() {

    private val dataMap: SparseArray<BlocksDialogRequest> = SparseArray()

    fun getRequest(id: Int): BlocksDialogRequest? {
        return dataMap[id]
    }

    fun setRequest(request: BlocksDialogRequest) {
        dataMap.put(request.id, request)
    }

    fun removeRequest(id: Int) {
        dataMap.delete(id)
    }
}