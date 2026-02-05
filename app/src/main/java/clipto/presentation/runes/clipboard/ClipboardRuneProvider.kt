package clipto.presentation.runes.clipboard

import android.graphics.Color
import androidx.fragment.app.Fragment
import clipto.PermissionsManager
import clipto.analytics.Analytics
import clipto.common.extensions.navigateTo
import clipto.domain.ClientSession
import clipto.domain.IRune
import clipto.domain.NotificationStyle
import clipto.extensions.getColorHint
import clipto.extensions.getTitleRes
import clipto.extensions.isAvailable
import clipto.presentation.blocks.DialogBlock
import clipto.presentation.blocks.PopupBlock
import clipto.presentation.blocks.SeekBarBlock
import clipto.presentation.blocks.SwitchBlock
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.blocks.ux.WarningBlock
import clipto.presentation.clip.fastactions.FastActionsFragment
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.runes.RuneSettingsProvider
import clipto.store.clipboard.ClipboardState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wb.clipboard.R
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class ClipboardRuneProvider @Inject constructor(
    private val dialogState: DialogState,
    private val clipboardState: ClipboardState,
    private val permissionsManager: PermissionsManager,
) : RuneSettingsProvider(
    IRune.RUNE_CLIPBOARD,
    R.drawable.rune_clipboard,
    R.string.runes_clipboard_title,
    R.string.runes_clipboard_description
) {

    override fun getDefaultColor(): String = "#82B1FF"

    override fun isActive(): Boolean = !appState.getSettings().doNotTrackClipboardChanges

    override fun hasWarning(): Boolean = isActive() && !clipboardState.isClipboardSupportedBySomehow(true)

    override fun createSettings(fragment: Fragment, flat: Boolean): List<BlockItem<Fragment>> {
        val settings = appState.getSettings()
        val ctx = fragment.requireContext()
        val act = fragment.requireActivity()
        val canTrackClipboard = !settings.doNotTrackClipboardChanges
        val supportClipboardTracking = clipboardState.isClipboardSupportedBySomehow(true)
        val clipboardFilter = appState.getFilterByClipboard()
        val limitRange = appConfig.limitClipboardNotesRange()
        val limitDefault = appConfig.limitClipboardNotesDefault()
        val currentLimit = clipboardFilter.limit?.takeIf { it != 0 } ?: limitDefault
        if (!supportClipboardTracking) {
            if (settings.notificationStyle == NotificationStyle.NULL) {
                settings.notificationStyle = NotificationStyle.HISTORY
            }
        }
        val limits = limitRange
            .plus(limitDefault)
            .plus(currentLimit)
            .distinct()
            .sorted()
            .let {
                if (it.first() == ClientSession.UNLIMITED) {
                    it.minus(ClientSession.UNLIMITED).plus(ClientSession.UNLIMITED)
                } else {
                    it
                }
            }

        val list = mutableListOf<BlockItem<Fragment>>()
        if (!settings.doNotTrackClipboardChanges) {
            if (!supportClipboardTracking) {
                if (!permissionsManager.canDrawOverlayViews()) {
                    list.add(WarningBlock(
                        titleRes = R.string.runes_texpander_required_floating,
                        clickListener = { permissionsManager.requestOverlayViews(act) }
                    ))
                } else if (!permissionsManager.canReadLogs()) {
                    list.add(
                        WarningBlock(
                            titleRes = R.string.settings_track_clipboard_q_message,
                            clickListener = {
                                fragment.navigateTo(R.id.action_settings_q_warning)
                            },
                            textColor = Color.BLACK,
                            backgroundColor = app.getColorHint()
                        )
                    )
                }
                list.add(SwitchBlock(
                    titleRes = R.string.settings_track_clipboard_q_emulate_title,
                    descriptionRes = R.string.settings_track_clipboard_q_emulate_description,
                    checked = settings.emulateCopyAction,
                    clickListener = { _, isChecked ->
                        if (settings.emulateCopyAction != isChecked) {
                            settings.emulateCopyAction = isChecked
                            if (settings.emulateCopyAction) {
                                Analytics.initEmulateCopyAction()
                            }
                        }
                    }
                ))
                list.add(SeparatorVerticalBlock())
            }
        }
        list.add(SwitchBlock(
            titleRes = R.string.settings_track_clipboard_title,
            descriptionRes = R.string.settings_track_clipboard_description,
            checked = !settings.doNotTrackClipboardChanges,
            clickListener = { _, isChecked ->
                if (settings.doNotTrackClipboardChanges != !isChecked) {
                    settings.doNotTrackClipboardChanges = !isChecked
                    if (isChecked) {
                        if (userState.isAuthorized()) {
                            settings.universalClipboard = true
                        }
                        Analytics.initTrackClipboard()
                    } else {
                        settings.launchOnStartup = false
                        settings.universalClipboard = false
                    }
                    appState.refreshSettings()
                }
            }
        ))
        list.add(SeparatorVerticalBlock())
        list.add(SwitchBlock(
            titleRes = R.string.settings_universal_clipboard_title,
            descriptionRes = R.string.settings_universal_clipboard_description,
            checked = settings.universalClipboard,
            enabled = canTrackClipboard,
            clickListener = { view, isChecked ->
                val data = ConfirmDialogData(
                    iconRes = R.drawable.ic_sync_enabled,
                    title = app.getString(R.string.sync_warning_title),
                    description = app.getString(R.string.sync_warning_description),
                    onConfirmed = {
                        userState.withAuth(withWarning = false) {
                            if (settings.universalClipboard != isChecked) {
                                settings.universalClipboard = isChecked
                                if (settings.universalClipboard) {
                                    Analytics.initUniversalClipboard()
                                }
                                appState.refreshSettings()
                            }
                        }
                    },
                    autoConfirm = { settings.universalClipboard || userState.isAuthorized() }
                )
                dialogState.showConfirm(data)
                if (!data.autoConfirm.invoke()) view.isChecked = !isChecked
            }
        ))
        list.add(SeparatorVerticalBlock())
        list.add(SwitchBlock(
            titleRes = R.string.settings_startup_title,
            descriptionRes = R.string.settings_startup_description,
            checked = settings.launchOnStartup,
            enabled = canTrackClipboard,
            clickListener = { _, isChecked ->
                if (settings.launchOnStartup != isChecked) {
                    settings.launchOnStartup = isChecked
                    if (settings.launchOnStartup) {
                        permissionsManager.requestAutoStart(act)
                        Analytics.initLaunchOnStartup()
                    }
                }
            }
        ))
        list.add(SeparatorVerticalBlock())
        list.add(SwitchBlock(
            titleRes = R.string.settings_clipboard_exclude_custom_attrs_title,
            descriptionRes = R.string.settings_clipboard_exclude_custom_attrs_description,
            checked = clipboardFilter.excludeWithCustomAttributes,
            enabled = canTrackClipboard,
            clickListener = { _, isChecked ->
                if (clipboardFilter.excludeWithCustomAttributes != isChecked) {
                    clipboardFilter.excludeWithCustomAttributes = isChecked
                    if (clipboardFilter.excludeWithCustomAttributes) {
                        Analytics.initExcludeCustomAttrs()
                    }
                    mainState.requestApplyFilter(appState.getFilterByLast(), force = true, closeNavigation = false)
                }
            }
        ))
        list.add(SeparatorVerticalBlock())
        list.add(DialogBlock(
            titleRes = R.string.settings_notification_style_title,
            descriptionRes = R.string.settings_notification_style_description,
            enabled = canTrackClipboard,
            value = app.getString(settings.notificationStyle.getTitleRes()),
            clickListener = {
                val values = NotificationStyle.values().filter { it.isAvailable() }
                val value = settings.notificationStyle.get()
                val valueIndex = values.indexOf(value)
                val valueItems = values.map { app.getString(it.getTitleRes()) }

                val builder = MaterialAlertDialogBuilder(ctx)
                builder.setTitle(R.string.settings_notification_style_title)
                builder.setSingleChoiceItems(valueItems.toTypedArray(), valueIndex) { dialog, which ->
                    dialog.dismiss()
                    val selected = values[which]
                    settings.notificationStyle = selected
                    clipboardState.refreshNotification()
                    appState.refreshSettings()
                }
                builder.show()
            }
        ))
        if (settings.notificationStyle == NotificationStyle.ACTIONS) {
            list.add(PopupBlock(
                enabled = canTrackClipboard,
                value = app.getString(NotificationStyle.ACTIONS.getTitleRes()),
                clickListener = {
                    FastActionsFragment.show(fragment, R.attr.colorContext, R.string.notification_style_actions, true)
                }
            ))
        }
        list.add(SeparatorVerticalBlock())
        list.add(SwitchBlock(
            titleRes = R.string.settings_notification_sound_title,
            descriptionRes = R.string.settings_notification_sound_description,
            checked = !settings.clipboardUseDefaultNotificationSound,
            enabled = canTrackClipboard,
            clickListener = { _, isChecked ->
                settings.clipboardUseDefaultNotificationSound = !isChecked
            }
        ))
        list.add(SeparatorVerticalBlock())
        list.add(SeekBarBlock(
            titleRes = R.string.filter_limit_title,
            descriptionRes = R.string.filter_limit_description,
            enabled = canTrackClipboard,
            progress = limits.indexOf(currentLimit),
            maxValue = limits.size - 1,
            changeProvider = {
                val newLimit = limits[it]
                if (clipboardFilter.limit != newLimit) {
                    clipboardFilter.limit = newLimit
                }
                newLimit
            }
        ))
        if (!settings.doNotTrackClipboardChanges && (!clipboardState.isClipboardSupportedNatively() && !clipboardState.isClipboardSupportedByAdb())) {
            list.add(SpaceBlock(16))
            list.add(SeparatorVerticalBlock())
            list.add(SwitchBlock(
                titleRes = R.string.settings_clipboard_ignore_q_warning_title,
                descriptionRes = R.string.settings_clipboard_ignore_q_warning_description,
                checked = settings.doNotDisplayAndroidQWarning,
                clickListener = { _, isChecked ->
                    if (settings.doNotDisplayAndroidQWarning != isChecked) {
                        settings.doNotDisplayAndroidQWarning = isChecked
                        if (settings.doNotDisplayAndroidQWarning) {
                            Analytics.initIgnoreQWarning()
                        }
                        clipboardState.refreshNotification()
                        appState.refreshSettings()
                    }
                }
            ))
        }
        return list
    }
}