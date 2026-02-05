package clipto.presentation.runes.keyboard_companion.panel

import clipto.common.extensions.gone
import clipto.common.extensions.visible
import clipto.domain.Clip
import clipto.domain.ObjectType
import clipto.extensions.*
import clipto.presentation.runes.keyboard_companion.CompanionEventInfo
import clipto.presentation.runes.keyboard_companion.CompanionMode
import com.wb.clipboard.R
import java.util.*

class EditTextPanelState(panel: CompanionPanel) : AbstractPanelState(panel) {

    private val inputStates = mutableMapOf<Int, InputState>()
    private var eventInfo: CompanionEventInfo? = null

    override fun clear() {
        inputStates.takeIf { it.size > panel.viewModel.undoRedoInputMemory() }?.clear()
        eventInfo = null
    }

    override fun doTest(eventInfo: CompanionEventInfo): Boolean {
        val nodeInfo = eventInfo.nodeInfo
//        panel.log("check event: isEditable={}, isVisibleToUser={}, isFocused={}, inputType={}, autoComplete={}",
//                nodeInfo?.isEditable,
//                nodeInfo?.isVisibleToUser,
//                nodeInfo?.isFocused,
//                nodeInfo?.inputType,
//                nodeInfo.isEditOrAutoComplete()
//
//        )
        return nodeInfo != null &&
                nodeInfo.isEditable &&
                nodeInfo.isVisibleToUser &&
                nodeInfo.isFocused &&
                nodeInfo.isEditOrAutoComplete()
    }

    override fun doApply(eventInfo: CompanionEventInfo) {
        val eventId = eventInfo.getId()

        this.eventInfo = eventInfo

        val nodeInfo = eventInfo.nodeInfo

        state.mode.setValue(CompanionMode.AUTO_DETECTED)
        panel.showClipsActionView.setText(R.string.runes_texpander_action_insert)
        panel.counterView.visible()

        val newText = eventInfo.getText()?.toString()
        val inputState = inputStates.getOrPut(eventId) { InputState(newText) }

        if (eventInfo.isViewTextChanged() || eventInfo.isViewTextSelectionChanged()) {
            val prevText = inputState.currentText
            inputState.currentText = newText
            panel.log("compare: id={}, {} - {} -> equals({})", eventId, prevText, newText, prevText == newText)
            if (prevText != newText) {
                val undoState = UndoRedoState(
                        prevText,
                        eventInfo.getTextSelectionStart(),
                        eventInfo.getTextSelectionEnd()
                )
                inputState.undoList.add(undoState)
                inputState.redoList.clear()
                panel.log("undo_redo stack: {}", inputState.undoList.size)
            }
        }

        panel.undoView.setIconResource(R.drawable.clip_edit_undo)
        panel.redoView.setIconResource(R.drawable.clip_edit_redo)
        if (nodeInfo.isPassword()) {
            panel.undoView.gone()
            panel.redoView.gone()
        } else {
            panel.undoView.visible()?.isEnabled = inputState.canUndo() == true
            panel.redoView.visible()?.isEnabled = inputState.canRedo() == true
        }
    }

    override fun onUndo() {
        val eventInfoRef = eventInfo ?: return
        val eventNode = eventInfoRef.nodeInfo ?: return
        eventInfoRef.getId().let { inputStates[it] }?.let { inputState ->
            inputState.undoList.removeLastOrNull()?.let {
                panel.log("undo_redo onUndo: {}", it.text)
                val redoState = UndoRedoState(
                        inputState.currentText,
                        eventInfoRef.getTextSelectionStart(),
                        eventInfoRef.getTextSelectionEnd()
                )
                inputState.redoList.add(redoState)
                inputState.currentText = it.text
                eventNode.performSetText(it.text, it.textSelectionStart, it.textSelectionEnd)
            }
        }
    }

    override fun onRedo() {
        val eventInfoRef = eventInfo ?: return
        val eventNode = eventInfoRef.nodeInfo ?: return
        eventInfoRef.getId().let { inputStates[it] }?.let { inputState ->
            inputState.redoList.removeLastOrNull()?.let {
                panel.log("undo_redo onRedo: {}", it.text)
                val undoState = UndoRedoState(
                        inputState.currentText,
                        eventInfoRef.getTextSelectionStart(),
                        eventInfoRef.getTextSelectionEnd()
                )
                inputState.undoList.add(undoState)
                inputState.currentText = it.text
                eventNode.performSetText(it.text, it.textSelectionStart, it.textSelectionEnd)
            }
        }
    }

    override fun doClick(clip: Clip, eventInfo: CompanionEventInfo, callback: () -> Unit) {
        panel.viewModel.withText(clip) {
            val result = eventInfo.nodeInfo?.performInsertText(it)
            if (result != true) {
                val newClip = Clip.from(it.toString(), false, ObjectType.INTERNAL_GENERATED)
                panel.viewModel.onCopy(newClip)
            }
            panel.showHideDetails(false)
            callback.invoke()
        }
    }

    data class InputState(
            var currentText: String?,
            val undoList: LinkedList<UndoRedoState> = LinkedList(),
            val redoList: LinkedList<UndoRedoState> = LinkedList()) {
        fun canUndo() = undoList.isNotEmpty()
        fun canRedo() = redoList.isNotEmpty()
    }

    data class UndoRedoState(
            val text: String?,
            val textSelectionStart: Int,
            val textSelectionEnd: Int
    )

}