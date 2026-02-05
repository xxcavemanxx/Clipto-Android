package clipto.presentation.file.view

import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import clipto.common.extensions.debounce
import clipto.common.presentation.state.ViewState
import clipto.domain.FileRef
import clipto.domain.isEditable
import clipto.domain.isReadOnly
import clipto.presentation.common.fragment.attributed.AttributedObjectFragment
import clipto.presentation.common.view.RuneIconView
import clipto.store.files.FileScreenState
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_attributed_object.*

@AndroidEntryPoint
class FileFragment : AttributedObjectFragment<FileRef, FileScreenState, FileViewModel>() {

    override val viewModel: FileViewModel by viewModels()
    override fun getFitViewId(): Int = R.id.tvFilePreview
    override fun hasConfig(state: FileScreenState): Boolean = !state.isReadOnly()

    override fun createViewState(): ViewState<FileScreenState> =
        ViewState(
            object : ViewState.Layer<FileScreenState, ImageView>(iv1, "action_close") {
                override fun canApply(state: FileScreenState): Boolean = state.isViewMode()
                override fun doApply(state: FileScreenState) {
                    bindAction(layerView, R.drawable.action_arrow_back, R.string.content_description_back) {
                        navigateUp()
                    }
                }
            },
            object : ViewState.Layer<FileScreenState, ImageView>(iv1, "action_cancel_edit") {
                override fun canApply(state: FileScreenState): Boolean = state.isEditMode()
                override fun doApply(state: FileScreenState) {
                    bindAction(layerView, R.drawable.action_cancel, R.string.menu_cancel) {
                        viewModel.onCancel()
                    }
                }
            },
            object : ViewState.Layer<FileScreenState, ImageView>(iv5, "action_edit") {
                override fun canApply(state: FileScreenState): Boolean = !state.isEditMode() && state.isEditable()
                override fun doApply(state: FileScreenState) {
                    bindAction(layerView, R.drawable.action_edit, R.string.menu_edit) {
                        viewModel.onEdit()
                    }
                }
            },
            object : ViewState.Layer<FileScreenState, ImageView>(iv5, "action_save") {
                override fun canApply(state: FileScreenState): Boolean = state.isEditMode() && !viewModel.getSettings().autoSave
                override fun doApply(state: FileScreenState) {
                    bindAction(layerView, R.drawable.action_save, R.string.button_save) {
                        viewModel.onSave()
                    }
                }
            },
            object : ViewState.Layer<FileScreenState, RuneIconView>(autoSaveIconView, "action_auto_save") {
                override fun canApply(state: FileScreenState): Boolean = state.isEditMode() && viewModel.getSettings().autoSave
                override fun canBind(state: FileScreenState): Boolean = true
                override fun doApply(state: FileScreenState) {
                    layerView.setOnClickListener(View.OnClickListener { viewModel.onSave() }.debounce())
                    layerView.contentDescription = viewModel.string(R.string.runes_auto_save_title)
                    layerView.setOnLongClickListener(onContentDescriptionListener)
                }
            },
            object : ViewState.Layer<FileScreenState, ImageView>(iv9, "action_share") {
                override fun canApply(state: FileScreenState): Boolean = !state.isReadOnly()
                override fun doApply(state: FileScreenState) {
                    bindAction(layerView, R.drawable.ic_share, R.string.menu_share_link) {
                        viewModel.onShare()
                    }
                }
            }
        )

}