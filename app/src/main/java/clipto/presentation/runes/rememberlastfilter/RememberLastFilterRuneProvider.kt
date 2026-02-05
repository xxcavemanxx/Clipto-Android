package clipto.presentation.runes.rememberlastfilter

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
class RememberLastFilterRuneProvider @Inject constructor() : RuneSettingsProvider(
    IRune.RUNE_REMEMBER_LAST_FILTER,
    R.drawable.rune_remember_last_filter,
    R.string.runes_remember_last_filter_title,
    R.string.runes_remember_last_filter_description
) {
    override fun getDefaultColor(): String = "#FF3D00"

    override fun isActive(): Boolean = appState.getSettings().restoreFilterOnStart

    override fun createSettings(fragment: Fragment, flat: Boolean): List<BlockItem<Fragment>> {
        val list = mutableListOf<BlockItem<Fragment>>()
        val settings = appState.getSettings()
        list.add(SwitchBlock(
            titleRes = R.string.settings_restore_filter_title,
            checked = settings.restoreFilterOnStart,
            clickListener = { _, isChecked ->
                if (settings.restoreFilterOnStart != isChecked) {
                    settings.restoreFilterOnStart = isChecked
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