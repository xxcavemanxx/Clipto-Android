package clipto.presentation.common.dialog.blocks

import android.app.Application
import clipto.presentation.common.fragment.blocks.BlocksViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BlocksDialogViewModel @Inject constructor(
    app: Application
) : BlocksViewModel(app)