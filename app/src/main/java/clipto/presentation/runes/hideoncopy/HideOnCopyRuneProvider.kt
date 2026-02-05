package clipto.presentation.runes.hideoncopy

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
class HideOnCopyRuneProvider @Inject constructor() : RuneSettingsProvider(
    IRune.RUNE_HIDE_ON_COPY,
    R.drawable.rune_hide_on_copy,
    R.string.runes_hide_on_copy_title,
    R.string.runes_hide_on_copy_description
) {
    override fun getDefaultColor(): String = "#00E5FF"

    override fun isActive(): Boolean = appState.getSettings().hideOnCopy

    override fun createSettings(fragment: Fragment, flat: Boolean): List<BlockItem<Fragment>> {
        val list = mutableListOf<BlockItem<Fragment>>()
        val settings = appState.getSettings()
        list.add(SwitchBlock(
            titleRes = R.string.settings_hide_on_click_title,
            checked = settings.hideOnCopy,
            clickListener = { _, isChecked ->
                if (settings.hideOnCopy != isChecked) {
                    settings.hideOnCopy = isChecked
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