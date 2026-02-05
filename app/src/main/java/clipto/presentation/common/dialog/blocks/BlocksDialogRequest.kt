package clipto.presentation.common.dialog.blocks

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import clipto.common.misc.AndroidUtils

data class BlocksDialogRequest(
    val id: Int = AndroidUtils.nextId(),
    val onReady: (viewModel: BlocksDialogViewModel) -> Unit,
    val onDestroy: (viewModel: BlocksDialogViewModel) -> Unit = {},
    val onBackConsumed: (viewModel: BlocksDialogViewModel) -> Boolean = { false },
    val onCreateAdapter: ((viewModel: BlocksDialogViewModel, fragment: Fragment, adapter: RecyclerView.Adapter<*>) -> RecyclerView.Adapter<*>)? = null
)