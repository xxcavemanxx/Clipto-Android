package clipto.presentation.runes.backup_restore

import androidx.fragment.app.Fragment
import clipto.backup.BackupItemType
import clipto.backup.BackupManager
import clipto.domain.IRune
import clipto.presentation.blocks.SeparateScreenBlock
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.dialog.select.value.SelectValueDialogRequest
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.runes.RuneSettingsProvider
import com.wb.clipboard.R
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class BackupRestoreRuneProvider @Inject constructor(
    private val dialogState: DialogState,
    private val backupManager: BackupManager
) : RuneSettingsProvider(
    IRune.RUNE_BACKUP_RESTORE,
    R.drawable.rune_backup_restore,
    R.string.runes_backup_restore_title,
    R.string.runes_backup_restore_description
) {
    override fun getDefaultColor(): String = "#3D5AFE"

    override fun isActive(): Boolean = !userState.isAuthorized()

    override fun createSettings(fragment: Fragment, flat: Boolean): List<BlockItem<Fragment>> {
        val act = fragment.requireActivity()
        val list = mutableListOf<BlockItem<Fragment>>()
        list.add(SeparateScreenBlock(
            titleRes = R.string.settings_backup_title,
            descriptionRes = R.string.settings_backup_description,
            clickListener = {
                val options = mutableListOf<SelectValueDialogRequest.Option<BackupItemType>>()
                BackupItemType.values().forEach { type ->
                    options.add(
                        SelectValueDialogRequest.Option(
                            checked = true,
                            title = app.getString(type.titleRes),
                            model = type
                        )
                    )
                }
                val request = SelectValueDialogRequest(
                    title = app.getString(R.string.settings_backup_title),
                    withImmediateNotify = false,
                    withClearAll = true,
                    withClearAllCustomTitleRes = R.string.button_confirm,
                    withClearAllCustomListener = {
                        if (it.isNotEmpty()) {
                            backupManager.backup(act, it)
                        }
                        true
                    },
                    options = options,
                    single = false,
                    onSelected = {}
                )
                dialogState.requestSelectValueDialog(request)
            }
        ))
        list.add(SeparatorVerticalBlock())
        list.add(SeparateScreenBlock(
            titleRes = R.string.settings_restore_title,
            description = act.getString(R.string.settings_restore_description, appConfig.getBackupSupportedImportFormats()),
            clickListener = {
                backupManager.restore(act)
            }
        ))
        if (!flat) {
            list.add(SeparatorVerticalBlock())
        }
        return list
    }
}