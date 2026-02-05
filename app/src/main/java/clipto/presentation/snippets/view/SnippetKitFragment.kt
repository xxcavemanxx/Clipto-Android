package clipto.presentation.snippets.view

import android.widget.ImageView
import androidx.fragment.app.viewModels
import clipto.common.presentation.mvvm.base.FragmentBackButtonListener
import clipto.common.presentation.mvvm.base.StatefulFragment
import clipto.common.presentation.state.ViewState
import clipto.domain.Clip
import clipto.presentation.common.fragment.attributed.AttributedObjectFragment
import clipto.store.clip.ClipScreenState
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_attributed_object.*

@AndroidEntryPoint
class SnippetKitFragment : AttributedObjectFragment<Clip, ClipScreenState, SnippetKitViewModel>(), FragmentBackButtonListener, StatefulFragment {

    override val viewModel: SnippetKitViewModel by viewModels()
    override fun getFitViewId(): Int = R.id.etClipText

    override fun createViewState(): ViewState<ClipScreenState> =
        ViewState(
            object : ViewState.Layer<ClipScreenState, ImageView>(iv1, "action_close") {
                override fun canApply(state: ClipScreenState): Boolean = true
                override fun doApply(state: ClipScreenState) {
                    bindAction(layerView, R.drawable.action_arrow_back, R.string.content_description_back) {
                        navigateUp()
                    }
                }
            },
            object : ViewState.Layer<ClipScreenState, ImageView>(iv8, "action_copy") {
                override fun canApply(state: ClipScreenState): Boolean = state.isViewMode()
                override fun doApply(state: ClipScreenState) {
                    bindAction(layerView, R.drawable.ic_copy, R.string.menu_copy) {
                        viewModel.onCopy()
                    }
                }
            }
        )

}