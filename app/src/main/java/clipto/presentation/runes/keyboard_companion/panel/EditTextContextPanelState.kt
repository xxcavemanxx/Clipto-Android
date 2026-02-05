package clipto.presentation.runes.keyboard_companion.panel

import clipto.common.extensions.gone
import clipto.common.extensions.visible
import clipto.presentation.runes.keyboard_companion.CompanionEventInfo
import clipto.presentation.runes.keyboard_companion.CompanionMode
import com.wb.clipboard.R
import clipto.domain.Clip
import clipto.domain.ObjectType
import clipto.extensions.from
import clipto.extensions.isEditOrAutoComplete
import clipto.extensions.performInsertText
import kotlin.math.abs

class EditTextContextPanelState(panel: CompanionPanel) : AbstractPanelState(panel) {

    override fun doTest(eventInfo: CompanionEventInfo): Boolean {
        val nodeInfo = eventInfo.nodeInfo
        return nodeInfo != null &&
                nodeInfo.isEditable &&
                nodeInfo.isVisibleToUser &&
                nodeInfo.isFocused &&
                nodeInfo.textSelectionStart != nodeInfo.textSelectionEnd &&
                nodeInfo.isEditOrAutoComplete()
    }

    override fun getTextLength(eventInfo: CompanionEventInfo): Int {
        val nodeInfo = eventInfo.nodeInfo
        if (nodeInfo != null) {
            return abs(nodeInfo.textSelectionEnd - nodeInfo.textSelectionStart)
        }
        return super.getTextLength(eventInfo)
    }

    override fun doApply(eventInfo: CompanionEventInfo) {
        state.mode.setValue(CompanionMode.AUTO_DETECTED)
        panel.showClipsActionView.setText(R.string.runes_texpander_action_replace)
        panel.counterView.visible()
        panel.undoView.gone()
        panel.redoView.gone()
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

}