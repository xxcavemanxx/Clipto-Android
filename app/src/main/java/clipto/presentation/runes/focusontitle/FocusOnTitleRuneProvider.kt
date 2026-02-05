package clipto.presentation.runes.focusontitle

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
class FocusOnTitleRuneProvider @Inject constructor() : RuneSettingsProvider(
    IRune.RUNE_FOCUS_ON_TITLE,
    R.drawable.rune_focus_on_title,
    R.string.runes_focus_on_title_title,
    R.string.runes_focus_on_title_description
) {
    override fun getDefaultColor(): String = "#C6FF00"

    override fun isActive(): Boolean = appState.getSettings().focusOnTitle

    override fun createSettings(fragment: Fragment, flat: Boolean): List<BlockItem<Fragment>> {
        val list = mutableListOf<BlockItem<Fragment>>()
        val settings = appState.getSettings()
        list.add(SwitchBlock(
            titleRes = R.string.settings_focus_on_title_title,
            checked = settings.focusOnTitle,
            clickListener = { _, isChecked ->
                if (settings.focusOnTitle != isChecked) {
                    settings.focusOnTitle = isChecked
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