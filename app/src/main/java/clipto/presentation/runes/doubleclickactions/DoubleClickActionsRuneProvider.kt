package clipto.presentation.runes.doubleclickactions

import androidx.fragment.app.Fragment
import clipto.domain.IRune
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.SwitchBlock
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.runes.RuneSettingsProvider
import com.wb.clipboard.R
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class DoubleClickActionsRuneProvider @Inject constructor() : RuneSettingsProvider(
    IRune.RUNE_DOUBLE_CLICK_ACTIONS,
    R.drawable.rune_double_click_actions,
    R.string.runes_double_click_actions_title,
    R.string.runes_double_click_actions_description
) {
    override fun getDefaultColor(): String = "#FFC6FF00"

    override fun isActive(): Boolean = appState.getSettings().doubleClickToDelete ||
            appState.getSettings().doubleClickToEdit ||
            appState.getSettings().doubleClickToExit

    override fun createSettings(fragment: Fragment, flat: Boolean): List<BlockItem<Fragment>> {
        val list = mutableListOf<BlockItem<Fragment>>()
        val settings = appState.getSettings()
        list.add(SwitchBlock(
            titleRes = R.string.settings_double_click_action_exit,
            checked = settings.doubleClickToExit,
            clickListener = { _, isChecked ->
                if (settings.doubleClickToExit != isChecked) {
                    settings.doubleClickToExit = isChecked
                    appState.refreshSettings()
                }
            }
        ))
        list.add(SeparatorVerticalBlock())
        list.add(SwitchBlock(
            titleRes = R.string.settings_double_click_action_delete,
            checked = settings.doubleClickToDelete,
            clickListener = { _, isChecked ->
                if (settings.doubleClickToDelete != isChecked) {
                    settings.doubleClickToDelete = isChecked
                    appState.refreshSettings()
                }
            }
        ))
        list.add(SeparatorVerticalBlock())
        list.add(SwitchBlock(
            titleRes = R.string.settings_double_click_action_edit,
            checked = settings.doubleClickToEdit,
            clickListener = { _, isChecked ->
                if (settings.doubleClickToEdit != isChecked) {
                    settings.doubleClickToEdit = isChecked
                    appState.refreshSettings()
                }
            }
        ))
        if (!flat) {
            list.add(SeparatorVerticalBlock())
        }
        return list
    }
}