package clipto.presentation.runes.keyboard_companion.panel

import clipto.extensions.isInternal
import clipto.domain.Clip
import clipto.presentation.runes.keyboard_companion.CompanionEventInfo

abstract class AbstractPanelState(val panel: CompanionPanel) : IPanelState {

    protected val state = panel.viewModel.companionState

    override fun test(eventInfo: CompanionEventInfo): Boolean {
        val nodeInfo = eventInfo.nodeInfo
        if (nodeInfo != null && panel.viewModel.ignoreInternalEvents() && nodeInfo.isInternal(panel.service)) {
            return false
        }
        return doTest(eventInfo)
    }

    override fun apply(eventInfo: CompanionEventInfo) {
        panel.titleView.text = getTitle(eventInfo)
        val count = getTextLength(eventInfo)
        panel.counterView.text = "($count)"
        doApply(eventInfo)
    }

    override fun clear() = Unit
    override fun onUndo() = Unit
    override fun onRedo() = Unit

    final override fun onClick(clip: Clip, eventInfo: CompanionEventInfo, callback: () -> Unit) = doClick(clip, eventInfo, callback)
    final override fun onShowDetails() = doShowDetails()
    final override fun onHideDetails() = doHideDetails()

    protected open fun getTitle(eventInfo: CompanionEventInfo): CharSequence? = eventInfo.getTitle()
    protected open fun getTextLength(eventInfo: CompanionEventInfo): Int = eventInfo.getTextLength()

    protected open fun doClick(clip: Clip, eventInfo: CompanionEventInfo, callback: () -> Unit = {}) = Unit
    protected open fun doTest(eventInfo: CompanionEventInfo): Boolean = false
    protected open fun doApply(eventInfo: CompanionEventInfo) = Unit
    protected open fun doShowDetails() = Unit
    protected open fun doHideDetails() = Unit
    override fun unwrap(): IPanelState? = this

}