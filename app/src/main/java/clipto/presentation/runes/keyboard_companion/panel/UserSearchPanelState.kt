package clipto.presentation.runes.keyboard_companion.panel

import clipto.common.extensions.visible
import clipto.presentation.runes.keyboard_companion.CompanionEventInfo
import clipto.presentation.runes.keyboard_companion.CompanionMode
import com.wb.clipboard.R
import clipto.domain.Clip
import clipto.extensions.performClick

class UserSearchPanelState(
        panel: CompanionPanel,
        private val prevState: IPanelState?,
        private val prevMode: CompanionMode,
        private val prevEvent: CompanionEventInfo,
        val floating: Boolean = false
) : AbstractPanelState(panel) {

    override fun unwrap(): IPanelState? = prevState

    override fun apply(eventInfo: CompanionEventInfo) {
        state.mode.setValue(CompanionMode.USER_SEARCH)
        panel.undoView.setIconResource(R.drawable.action_arrow_back)
        panel.redoView.setIconResource(R.drawable.action_cancel)
        panel.undoView.visible()?.isEnabled = true
        panel.redoView.visible()?.isEnabled = true
        if (!floating) {
            panel.withFocusableLayout()
        }
    }

    override fun onUndo() {
        panel.withFloatingLayout(withCallback = true) {
            prevEvent.nodeInfo.performClick()
            panel.lastPanelState = prevState?.also { it.apply(prevEvent) }
            panel.bindClipsAdapter()
            state.mode.setValue(prevMode)
        }
    }

    override fun onRedo() {
        panel.withFloatingLayout(withCallback = true) {
            prevEvent.nodeInfo.performClick()
            panel.lastPanelState = prevState?.also { it.apply(prevEvent) }
            panel.viewModel.onClearSearch()
            panel.bindClipsAdapter()
            panel.lastPanelState = prevState
            state.mode.setValue(prevMode)
        }
    }

    override fun doHideDetails() {
        onUndo()
    }

    override fun doClick(clip: Clip, eventInfo: CompanionEventInfo, callback: () -> Unit) {
        panel.withFloatingLayout(withCallback = true) {
            prevEvent.nodeInfo.performClick()
            prevState?.onClick(clip, prevEvent) {
                onUndo()
            }
        }
    }

}