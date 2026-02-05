package clipto.presentation.runes.keyboard_companion.panel

import clipto.domain.Clip
import clipto.presentation.runes.keyboard_companion.CompanionEventInfo

interface IPanelState {

    fun apply(eventInfo: CompanionEventInfo)

    fun test(eventInfo: CompanionEventInfo): Boolean

    fun onClick(clip: Clip, eventInfo: CompanionEventInfo, callback: () -> Unit = {})

    fun onShowDetails()

    fun onHideDetails()

    fun onUndo()

    fun onRedo()

    fun clear()

    fun unwrap(): IPanelState?

}