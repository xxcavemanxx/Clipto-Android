package clipto.presentation.runes.swipeactions

import androidx.fragment.app.Fragment
import clipto.domain.IRune
import clipto.domain.SwipeAction
import clipto.presentation.blocks.domain.SwipeActionsBlock
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.runes.RuneSettingsProvider
import com.wb.clipboard.R
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class SwipeActionsRuneProvider @Inject constructor() : RuneSettingsProvider(
    IRune.RUNE_SWIPE_ACTIONS,
    R.drawable.rune_swipe_actions,
    R.string.runes_swipe_actions_title,
    R.string.runes_swipe_actions_description
) {
    override fun getDefaultColor(): String = "#1DE9B6"

    override fun isActive(): Boolean = appState.getSettings().swipeActionLeft != SwipeAction.NONE ||
            appState.getSettings().swipeActionRight !== SwipeAction.NONE

    override fun createSettings(fragment: Fragment, flat: Boolean): List<BlockItem<Fragment>> {
        val list = mutableListOf<BlockItem<Fragment>>()
        list.add(SwipeActionsBlock(appState, mainState))
        return list
    }
}