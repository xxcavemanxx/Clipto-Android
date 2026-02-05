package clipto.presentation.common.dialog.confirm

import android.util.SparseArray
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ConfirmDialogViewModel @Inject constructor() : ViewModel() {

    val dataMap: SparseArray<ConfirmDialogData> = SparseArray()

}