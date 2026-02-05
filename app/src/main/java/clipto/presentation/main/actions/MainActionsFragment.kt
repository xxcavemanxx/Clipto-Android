package clipto.presentation.main.actions

import androidx.fragment.app.viewModels
import clipto.common.extensions.setDebounceClickListener
import clipto.presentation.common.fragment.blocks.BlocksBottomSheetFragment
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_main_actions.*

@AndroidEntryPoint
class MainActionsFragment : BlocksBottomSheetFragment<MainActionsViewModel>() {

    override val layoutResId: Int = R.layout.fragment_main_actions

    override val viewModel: MainActionsViewModel by viewModels()

    override fun bind(viewModel: MainActionsViewModel) {
        super.bind(viewModel)
        ivMore.setDebounceClickListener {
            viewModel.onSettings()
        }
    }

}