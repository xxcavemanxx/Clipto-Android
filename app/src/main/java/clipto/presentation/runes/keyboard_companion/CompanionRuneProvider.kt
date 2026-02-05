package clipto.presentation.runes.keyboard_companion

import android.graphics.Color
import androidx.fragment.app.Fragment
import clipto.PermissionsManager
import clipto.common.misc.ThemeUtils
import clipto.domain.IRune
import clipto.extensions.getColorHint
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.SwitchBlock
import clipto.presentation.blocks.ux.WarningBlock
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.runes.RuneSettingsProvider
import com.wb.clipboard.R
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class CompanionRuneProvider @Inject constructor(
    private val companionState: CompanionState,
    private val permissionsHelper: PermissionsManager,
    private val notificationManager: CompanionNotificationManager
) : RuneSettingsProvider(
    IRune.RUNE_TEXPANDER,
    R.drawable.rune_keyboard_companion,
    R.string.runes_texpander_title,
    R.string.runes_texpander_description
) {

    override fun getDefaultColor(): String = "#FF9100"

    override fun isActive(): Boolean = companionState.isEnabled()

    override fun hasWarning(): Boolean = isActive() &&
            (!permissionsHelper.isAccessibilityEnabled(CompanionService::class.java) || !permissionsHelper.canDrawOverlayViews())

    override fun getDescription(): String = app.getString(descriptionRes, appConfig.texpanderAdvancedConfigUrl())

    override fun createSettings(fragment: Fragment, flat: Boolean): List<BlockItem<Fragment>> {
        val list = mutableListOf<BlockItem<Fragment>>()
        val settings = appState.getSettings()
        if (companionState.isEnabled()) {
            if (!permissionsHelper.canDrawOverlayViews()) {
                list.add(WarningBlock(
                    titleRes = R.string.runes_texpander_required_floating,
                    clickListener = { fragment.activity?.let { permissionsHelper.requestOverlayViews(it) } }
                ))
                companionState.isAvailable.setValue(false)
            } else if (!permissionsHelper.isAccessibilityEnabled(CompanionService::class.java)) {
                list.add(WarningBlock(
                    titleRes = R.string.runes_texpander_required_accessibility,
                    textColor = Color.BLACK,
                    backgroundColor = app.getColorHint(),
                    clickListener = {
                        fragment.activity?.let { permissionsHelper.requestAccessibility(it) }
                    }
                ))
                companionState.isAvailable.setValue(false)
            } else {
                list.add(WarningBlock(
                    titleRes = R.string.runes_texpander_action_show,
                    backgroundColor = ThemeUtils.getColor(app, R.attr.swipeActionCopy),
                    clickListener = { notificationManager.start() }
                ))
                companionState.isAvailable.setValue(true)
            }
        } else {
            companionState.isAvailable.setValue(false)
        }
        list.add(SwitchBlock(
            titleRes = R.string.runes_texpander_toggle_activate_label,
            checked = isActive(),
            clickListener = { _, isChecked ->
                if (settings.texpanderRuneEnabled != isChecked) {
                    settings.texpanderRuneEnabled = isChecked
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