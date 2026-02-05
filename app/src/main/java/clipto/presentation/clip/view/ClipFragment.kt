package clipto.presentation.clip.view

import android.content.res.ColorStateList
import android.view.View
import android.widget.EditTextExt
import android.widget.ImageView
import androidx.fragment.app.viewModels
import clipto.common.extensions.debounce
import clipto.common.presentation.mvvm.base.StatefulFragment
import clipto.common.presentation.state.ViewState
import clipto.domain.Clip
import clipto.domain.FocusMode
import clipto.domain.isEditable
import clipto.domain.isReadOnly
import clipto.extensions.getColorNegative
import clipto.extensions.getTextColorPrimary
import clipto.extensions.isNew
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.presentation.common.fragment.attributed.AttributedObjectFragment
import clipto.presentation.common.view.RuneIconView
import clipto.presentation.usecases.data.ShowNoteDetailsRequest
import clipto.store.clip.ClipScreenState
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_attributed_object.*

@AndroidEntryPoint
class ClipFragment : AttributedObjectFragment<Clip, ClipScreenState, ClipViewModel>(), StatefulFragment {

    override val viewModel: ClipViewModel by viewModels()
    override fun getFitViewId(): Int = R.id.etClipText

    override fun onFragmentBackPressed(): Boolean {
        if (onBackPressConsumed()) {
            return true
        }
        if (viewModel.isMergeMode() && viewModel.isContentChanged()) {
            storeActiveFieldState()
            viewModel.dialogState.showConfirm(
                ConfirmDialogData(
                    iconRes = R.drawable.ic_attention,
                    title = getString(R.string.clip_multiple_exit_without_save_title),
                    description = getString(R.string.clip_multiple_exit_without_save_description),
                    confirmActionTextRes = R.string.button_yes,
                    onConfirmed = { navigateUp() },
                    cancelActionTextRes = R.string.button_no
                )
            )
            return true
        }
        return false
    }

    override fun bind(viewModel: ClipViewModel) {
        super.bind(viewModel)
        viewModel.maxNotesCountLive.observe(viewLifecycleOwner) {
            rebuildNavigator()
        }
    }

    override fun createViewState(): ViewState<ClipScreenState> =
        ViewState(
            object : ViewState.Layer<ClipScreenState, ImageView>(iv1, "action_close") {
                override fun canApply(state: ClipScreenState): Boolean = !viewModel.isMergeMode() && (state.isViewMode() || (state.isEditMode() && state.value.isNew()))
                override fun doApply(state: ClipScreenState) {
                    bindAction(layerView, R.drawable.action_arrow_back, R.string.content_description_back) {
                        navigateUp()
                    }
                }
            },
            object : ViewState.Layer<ClipScreenState, ImageView>(iv1, "action_cancel_edit") {
                override fun canApply(state: ClipScreenState): Boolean = !viewModel.isMergeMode() && (state.isEditMode() && !state.value.isNew())
                override fun doApply(state: ClipScreenState) {
                    bindAction(layerView, R.drawable.action_cancel, R.string.menu_cancel) {
                        viewModel.onCancel()
                    }
                }
            },
            object : ViewState.Layer<ClipScreenState, ImageView>(iv1, "action_cancel_merge") {
                override fun canApply(state: ClipScreenState): Boolean = viewModel.isMergeMode()
                override fun doApply(state: ClipScreenState) {
                    bindAction(layerView, R.drawable.action_arrow_back, R.string.content_description_back) {
                        viewModel.onCancelMerge()
                    }
                }
            },
//                object : ViewState.Layer<ScreenState, ImageView>(iv2, "action_delete") {
//                    override fun canApply(state: ScreenState): Boolean = state.isViewMode() && !state.value.isDeleted()
//                    override fun doApply(state: ScreenState) {
//                        layerView.contentDescription = viewModel.string(R.string.menu_delete)
//                        bindAction(layerView, R.drawable.action_delete) {
//                            viewModel.onDelete()
//                        }
//                    }
//                },
            object : ViewState.Layer<ClipScreenState, ImageView>(iv3, "action_note_sync") {
                override fun canApply(state: ClipScreenState): Boolean = state.isViewMode() && viewModel.isNotSynced()
                override fun doApply(state: ClipScreenState) {
                    layerView.imageTintList = ColorStateList.valueOf(layerView.context.getColorNegative())
                    bindAction(layerView, R.drawable.ic_clip_not_synced_action, R.string.main_actions_sync_note) {
                        viewModel.onSync()
                    }
                }
            },
            object : ViewState.Layer<ClipScreenState, ImageView>(iv3, "action_new_note") {
                override fun canApply(state: ClipScreenState): Boolean = state.isEditMode() && !viewModel.isMergeMode()
                override fun doApply(state: ClipScreenState) {
                    layerView.imageTintList = ColorStateList.valueOf(layerView.context.getTextColorPrimary())
                    bindAction(layerView, R.drawable.ic_note_add, R.string.main_actions_save_and_start_new_note) {
                        viewModel.onOneMore()
                    }
                }
            },
//                object : ViewState.Layer<ScreenState, ImageView>(iv4, "action_unfav") {
//                    override fun canApply(state: ScreenState): Boolean = state.isViewMode() && state.value.fav
//                    override fun doApply(state: ScreenState) {
//                        layerView.contentDescription = viewModel.string(R.string.menu_fav)
//                        layerView.imageTintList = ColorStateList.valueOf(layerView.context.getActionIconColorHighlight())
//                        bindAction(layerView, R.drawable.ic_fav_true) {
//                            viewModel.onToggleFav()
//                        }
//                    }
//                },
//                object : ViewState.Layer<ScreenState, ImageView>(iv4, "action_fav") {
//                    override fun canApply(state: ScreenState): Boolean = state.isViewMode() && !state.value.fav
//                    override fun doApply(state: ScreenState) {
//                        layerView.contentDescription = viewModel.string(R.string.menu_fav)
//                        layerView.imageTintList = ColorStateList.valueOf(layerView.context.getActionIconColorNormal())
//                        bindAction(layerView, R.drawable.ic_fav_false_inverse) {
//                            viewModel.onToggleFav()
//                        }
//                    }
//                },
            object : ViewState.Layer<ClipScreenState, ImageView>(iv5, "action_restore") {
                override fun canApply(state: ClipScreenState): Boolean = state.isViewMode() && state.value.isDeleted()
                override fun doApply(state: ClipScreenState) {
                    bindAction(layerView, R.drawable.ic_restore, R.string.menu_restore) {
                        viewModel.onUndoDelete()
                    }
                }
            },
            object : ViewState.Layer<ClipScreenState, ImageView>(iv5, "action_save") {
                override fun canApply(state: ClipScreenState): Boolean = !viewModel.isMergeMode() && state.isEditMode() && !viewModel.getSettings().autoSave
                override fun doApply(state: ClipScreenState) {
                    bindAction(layerView, R.drawable.action_save, R.string.button_save) {
                        viewModel.onSave()
                    }
                }
            },
            object : ViewState.Layer<ClipScreenState, ImageView>(iv5, "action_merge") {
                override fun canApply(state: ClipScreenState): Boolean = viewModel.isMergeMode()
                override fun doApply(state: ClipScreenState) {
                    bindAction(layerView, R.drawable.ic_merge, R.string.menu_merge) {
                        viewModel.onMerge()
                    }
                }
            },
            object : ViewState.Layer<ClipScreenState, RuneIconView>(autoSaveIconView, "action_auto_save") {
                override fun canApply(state: ClipScreenState): Boolean = !viewModel.isMergeMode() && state.isEditMode() && viewModel.getSettings().autoSave
                override fun canBind(state: ClipScreenState): Boolean = true
                override fun doApply(state: ClipScreenState) {
                    layerView.setOnClickListener(View.OnClickListener { viewModel.onSave() }.debounce())
                    layerView.contentDescription = viewModel.string(R.string.runes_auto_save_title)
                    layerView.setOnLongClickListener(onContentDescriptionListener)
                }
            },
            object : ViewState.Layer<ClipScreenState, ImageView>(iv5, "action_edit") {
                override fun canApply(state: ClipScreenState): Boolean = !state.isEditMode() && state.isEditable()
                override fun doApply(state: ClipScreenState) {
                    bindAction(layerView, R.drawable.action_edit, R.string.menu_edit) {
                        viewModel.onEdit()
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
            },
            object : ViewState.Layer<ClipScreenState, ImageView>(iv9, "action_clip_details") {
                override fun canApply(state: ClipScreenState): Boolean = true
                override fun doApply(state: ClipScreenState) {
                    bindAction(layerView, R.drawable.ic_more_vert, R.string.fast_actions_more) {
                        viewModel.getClip()?.let { clip ->
                            val clipText = getFitView() as? EditTextExt ?: return@bindAction
                            val activeFieldState = storeActiveFieldState()
                            val isTextFocused = activeFieldState == FocusMode.TEXT
                            viewModel.clipState.activeFocus.setValue(activeFieldState)
                            viewModel.noteUseCases.onShowNoteDetails(this@ClipFragment, ShowNoteDetailsRequest(
                                clip = clip,
                                onChanged = { viewModel.onUpdate(it) },
                                onAttachment = { viewModel.onAddAttachment(it) },
                                onFastAction = { viewModel.dialogState.requestFastAction(it, clip) },
                                onValue = { value ->
                                    when {
                                        isTextFocused -> {
                                            runCatching {
                                                val currentText = clipText.text
                                                val isRangeSelected = activeFieldSelectionEnd > activeFieldSelectionStart
                                                val diff =
                                                    if (isRangeSelected) {
                                                        value.length
                                                    } else {
                                                        value.length - (activeFieldSelectionEnd - activeFieldSelectionStart)
                                                    }
                                                currentText?.replace(activeFieldSelectionStart, activeFieldSelectionEnd, value)
                                                activeFieldSelectionStart += diff
                                                activeFieldSelectionEnd = activeFieldSelectionStart
                                            }
                                        }
                                        else -> viewModel.onCopyPlaceholder(value)
                                    }
                                }
                            ))
                        }
                    }
                }
            }
        )

}