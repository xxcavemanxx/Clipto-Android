package clipto.presentation.runes.pincode

import androidx.fragment.app.Fragment
import clipto.domain.IRune
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.SwitchBlock
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.lockscreen.FingerprintUtils
import clipto.presentation.lockscreen.changepasscode.ChangePassCodeFragment
import clipto.presentation.runes.RuneSettingsProvider
import com.wb.clipboard.R
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class PincodeRuneProvider @Inject constructor() : RuneSettingsProvider(
    IRune.RUNE_PINCODE,
    R.drawable.rune_pincode,
    R.string.runes_pincode_title,
    R.string.runes_pincode_description
) {
    override fun getDefaultColor(): String = "#00E676"

    override fun getDescription(): String = app.getString(descriptionRes, appConfig.autoLockInMinutes())

    override fun createSettings(fragment: Fragment, flat: Boolean): List<BlockItem<Fragment>> {
        val changePasscodeId =
            if (flat) {
                R.id.action_change_passcode_to_runes
            } else {
                R.id.action_change_passcode_to_set_touch_id_runes
            }
        val list = mutableListOf<BlockItem<Fragment>>()
        val settings = appState.getSettings()
        list.add(SwitchBlock(
            titleRes = R.string.settings_security_passcode_title,
            checked = settings.isLocked(),
            clickListener = { view, isChecked ->
                if (settings.isLocked() != isChecked) {
                    appState.requestNavigateTo(
                        R.id.action_rune_settings_to_change_passcode,
                        ChangePassCodeFragment.args(!isChecked, changePasscodeId)
                    )
                    view.isChecked = !isChecked
                }
            }
        ))

        if (FingerprintUtils.isFingerprintAvailable(app)) {
            list.add(SeparatorVerticalBlock())
            list.add(SwitchBlock(
                titleRes = R.string.settings_security_fingerprint_title,
                enabled = settings.isLocked(),
                checked = settings.useFingerprint,
                clickListener = { _, isChecked ->
                    if (settings.useFingerprint != isChecked) {
                        settings.useFingerprint = isChecked
                        appState.refreshSettings()
                    }
                }
            ))
        }

        if (!flat) {
            list.add(SeparatorVerticalBlock())
        }

        return list
    }

    override fun isActive(): Boolean = appState.getSettings().isLocked()
}