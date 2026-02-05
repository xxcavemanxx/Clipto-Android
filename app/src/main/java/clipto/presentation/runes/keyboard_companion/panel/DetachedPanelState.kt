package clipto.presentation.runes.keyboard_companion.panel

import clipto.common.extensions.gone
import clipto.domain.Clip
import clipto.presentation.runes.keyboard_companion.CompanionEventInfo
import com.wb.clipboard.R

class DetachedPanelState(panel: CompanionPanel) : AbstractPanelState(panel) {

    private var appTitle: String? = null

    override fun test(eventInfo: CompanionEventInfo): Boolean {
        return when {
            state.isDetached() -> {
                panel.log("detached :: mode is forced")
                true
            }
            eventInfo.isViewClicked() && eventInfo.nodeInfo == null && eventInfo.isEditText() -> {
                panel.log("detached :: clicked edit text")
                true
            }
            eventInfo.isViewFocused() && eventInfo.nodeInfo == null && eventInfo.isEditText() -> {
                panel.log("detached :: focused edit text")
                true
            }
            else -> false
        }
    }

    override fun getTitle(eventInfo: CompanionEventInfo): CharSequence? {
        return appTitle
    }

    override fun doApply(eventInfo: CompanionEventInfo) {
        panel.showClipsActionView.setText(R.string.runes_texpander_action_copy)
        panel.counterView.gone()
        panel.undoView.gone()
        panel.redoView.gone()
        panel.bindClipsAdapter()
    }

    override fun doClick(clip: Clip, eventInfo: CompanionEventInfo, callback: () -> Unit) {
        panel.viewModel.onCopy(clip, callback)
    }

}