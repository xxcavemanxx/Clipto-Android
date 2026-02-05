package clipto.presentation.runes.autosave

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
class AutoSaveRuneProvider @Inject constructor() : RuneSettingsProvider(
    IRune.RUNE_AUTO_SAVE,
    R.drawable.rune_auto_save,
    R.string.runes_auto_save_title,
    R.string.runes_auto_save_description
) {
    override fun getDefaultColor(): String = "#3D5AFE"

    override fun isActive(): Boolean = appState.getSettings().autoSave

    override fun createSettings(fragment: Fragment, flat: Boolean): List<BlockItem<Fragment>> {
        val list = mutableListOf<BlockItem<Fragment>>()
        val settings = appState.getSettings()
        list.add(SwitchBlock(
            titleRes = R.string.settings_auto_save_title,
            checked = settings.autoSave,
            clickListener = { _, isChecked ->
                if (settings.autoSave != isChecked) {
                    settings.autoSave = isChecked
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