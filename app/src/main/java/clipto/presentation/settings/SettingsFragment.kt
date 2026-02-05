package clipto.presentation.settings

import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import clipto.AppUtils
import clipto.analytics.Analytics
import clipto.backup.BackupItemType
import clipto.common.extensions.getNavController
import clipto.common.extensions.navigateTo
import clipto.common.misc.IntentUtils
import clipto.common.presentation.mvvm.MvvmFragment
import clipto.common.presentation.state.MenuState
import clipto.common.presentation.state.ToolbarState
import clipto.domain.Theme
import clipto.domain.ClientSession
import clipto.domain.NotificationStyle
import clipto.domain.Settings
import clipto.extensions.getTitleRes
import clipto.extensions.isAvailable
import clipto.presentation.blocks.*
import clipto.presentation.blocks.domain.AccountBlock
import clipto.presentation.blocks.HeaderBlock
import clipto.presentation.blocks.domain.AboutBlock
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.ux.WarningBlock
import clipto.presentation.clip.fastactions.FastActionsFragment
import clipto.presentation.common.dialog.confirm.ConfirmDialogData
import clipto.presentation.common.dialog.select.value.SelectValueDialogRequest
import clipto.presentation.common.recyclerview.BlockListAdapter
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.lockscreen.FingerprintUtils
import clipto.presentation.lockscreen.changepasscode.ChangePassCodeFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_settings.*

@AndroidEntryPoint
class SettingsFragment : MvvmFragment<SettingsViewModel>() {

    override val layoutResId: Int = R.layout.fragment_settings
    override val viewModel: SettingsViewModel by viewModels()

    override fun bind(viewModel: SettingsViewModel) {
        val ctx = requireContext()

        ToolbarState<Unit>()
                .withTitleRes(R.string.settings_toolbar_title)
                .withMenuItem(
                        MenuState.StatefulMenuItem<Unit>()
                                .withShowAsActionNever()
                                .withTitle(R.string.about_label_issue)
                                .withIcon(R.drawable.menu_bug_report)
                                .withStateAcceptor { viewModel.appConfig.canReportIssues() }
                                .withListener { _, _ ->
                                    AppUtils.sendRequest(viewModel.string(R.string.about_label_issue).toString())
                                }
                )
                .withMenuItem(
                        MenuState.StatefulMenuItem<Unit>()
                                .withShowAsActionNever()
                                .withTitle(R.string.menu_faq)
                                .withIcon(R.drawable.ic_faq)
                                .withListener { _, _ ->
                                    viewModel.internetState.withInternet({
                                        IntentUtils.open(ctx, viewModel.appConfig.getFaqUrl())
                                    })
                                }
                )
                .apply(Unit, toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { getNavController().navigateUp() }

        val settingsAdapter = BlockListAdapter(this)
        recyclerView.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = settingsAdapter

        viewModel.settingsLive.observe(viewLifecycleOwner) {
            settingsAdapter.submitList(map(it))
        }

        Analytics.screenSettings()
    }

    private fun map(settings: Settings): List<BlockItem<SettingsFragment>> {
        val ctx = requireContext()
        val act = requireActivity()
        val userState = viewModel.userState
        val appConfig = viewModel.appConfig
        val canTrackClipboard = !settings.doNotTrackClipboardChanges
        val supportClipboardTracking = viewModel.clipboardState.isClipboardSupportedBySomehow()
        val clipboardFilter = viewModel.appState.getFilterByClipboard()
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

        val list = mutableListOf<BlockItem<SettingsFragment>>()

        // ACCOUNT
        list.add(AccountBlock(viewModel.userState, onSignIn = { webAuth, withWarning, callback -> viewModel.onSignIn(webAuth, withWarning, callback) }))
        list.add(SeparatorVerticalBlock(marginHoriz = 0))

        // SECURITY
        list.add(HeaderBlock(R.string.settings_group_security))
        list.add(SwitchBlock(
                titleRes = R.string.settings_security_passcode_title,
                description = viewModel.string(R.string.settings_security_passcode_description, viewModel.appConfig.autoLockInMinutes()).toString(),
                checked = settings.isLocked(),
                clickListener = { view, isChecked ->
                    if (settings.isLocked() != isChecked) {
                        navigateTo(R.id.action_settings_to_change_passcode, ChangePassCodeFragment.args(!isChecked))
                        view.isChecked = !isChecked
                    }
                }
        ))
        list.add(SeparatorVerticalBlock())
        if (settings.isLocked() && FingerprintUtils.isFingerprintAvailable(viewModel.app)) {
            list.add(SwitchBlock(
                    titleRes = R.string.settings_security_fingerprint_title,
                    checked = settings.useFingerprint,
                    clickListener = { _, isChecked ->
                        if (settings.useFingerprint != isChecked) {
                            settings.useFingerprint = isChecked
                        }
                    }
            ))
            list.add(SeparatorVerticalBlock())
        }

        // CLIPBOARD
        list.add(HeaderBlock(R.string.settings_group_clipboard))
        if (!settings.doNotTrackClipboardChanges && !supportClipboardTracking) {
            list.add(WarningBlock(
                    titleRes = R.string.settings_track_clipboard_q_message,
                    clickListener = {
                        navigateTo(R.id.action_settings_q_warning)
                    }
            ))
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
                        viewModel.onSaveSettings()
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
                            title = viewModel.string(R.string.sync_warning_title),
                            description = viewModel.string(R.string.sync_warning_description),
                            onConfirmed = {
                                viewModel.onSignIn {
                                    if (settings.universalClipboard != isChecked) {
                                        settings.universalClipboard = isChecked
                                        if (settings.universalClipboard) {
                                            Analytics.initUniversalClipboard()
                                        }
                                        viewModel.onSaveSettings()
                                    }
                                }
                            },
                            autoConfirm = { settings.universalClipboard || userState.isAuthorized() }
                    )
                    viewModel.dialogState.showConfirm(data)
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
                        viewModel.onApplyLastFilter()
                        viewModel.isClipboardFilterChanged = true
                    }
                }
        ))
        list.add(SeparatorVerticalBlock())
        list.add(DialogBlock(
                titleRes = R.string.settings_notification_style_title,
                descriptionRes = R.string.settings_notification_style_description,
                enabled = canTrackClipboard,
                value = viewModel.string(settings.notificationStyle.getTitleRes()).toString(),
                clickListener = {
                    val values = NotificationStyle.values().filter { it.isAvailable() }
                    val value = settings.notificationStyle
                    val valueIndex = values.indexOf(value)
                    val valueItems = values.map { getString(it.getTitleRes()) }

                    val builder = MaterialAlertDialogBuilder(ctx)
                    builder.setTitle(R.string.settings_notification_style_title)
                    builder.setSingleChoiceItems(valueItems.toTypedArray(), valueIndex) { dialog, which ->
                        dialog.dismiss()
                        val selected = values[which]
                        if (value != selected) {
                            settings.notificationStyle = selected
                            viewModel.clipboardState.refreshNotification()
                            viewModel.onSaveSettings()
                        }
                    }
                    builder.show()
                }
        ))
        if (settings.notificationStyle == NotificationStyle.ACTIONS) {
            list.add(PopupBlock(
                    enabled = canTrackClipboard,
                    value = viewModel.string(NotificationStyle.ACTIONS.getTitleRes()).toString(),
                    clickListener = { FastActionsFragment.show(this, R.attr.colorContext, R.string.notification_style_actions, true) }
            ))
        }
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
                        viewModel.isClipboardFilterChanged = true
                    }
                    newLimit
                }
        ))

        // BEHAVIOUR
        list.add(HeaderBlock(R.string.settings_group_behavior))
        list.add(SwitchBlock(
                titleRes = R.string.settings_hide_on_click_title,
                descriptionRes = R.string.settings_hide_on_click_description,
                checked = settings.hideOnCopy,
                clickListener = { _, isChecked ->
                    if (settings.hideOnCopy != isChecked) {
                        settings.hideOnCopy = isChecked
                    }
                }
        ))
        list.add(SeparatorVerticalBlock())
        list.add(SwitchBlock(
                titleRes = R.string.settings_restore_filter_title,
                descriptionRes = R.string.settings_restore_filter_description,
                checked = settings.restoreFilterOnStart,
                clickListener = { _, isChecked ->
                    if (settings.restoreFilterOnStart != isChecked) {
                        settings.restoreFilterOnStart = isChecked
                    }
                }
        ))
        val starredFilter = viewModel.appState.getFilterByStarred()
        list.add(SeparatorVerticalBlock())
        list.add(SwitchBlock(
                titleRes = R.string.settings_pin_starred_title,
                descriptionRes = R.string.settings_pin_starred_description,
                checked = starredFilter.pinStarredEnabled,
                clickListener = { _, isChecked ->
                    if (starredFilter.pinStarredEnabled != isChecked) {
                        starredFilter.pinStarredEnabled = isChecked
                        viewModel.onSaveFilter(starredFilter, withReload = true)
                    }
                }
        ))
        list.add(SeparatorVerticalBlock())
        list.add(SwitchBlock(
                titleRes = R.string.settings_focus_on_title_title,
                descriptionRes = R.string.settings_focus_on_title_description,
                checked = settings.focusOnTitle,
                clickListener = { _, isChecked ->
                    if (settings.focusOnTitle != isChecked) {
                        settings.focusOnTitle = isChecked
                    }
                }
        ))
        list.add(SeparatorVerticalBlock())
        list.add(SwitchBlock(
            titleRes = R.string.settings_auto_save_title,
            descriptionRes = R.string.settings_auto_save_description,
            checked = settings.autoSave,
            clickListener = { _, isChecked ->
                if (settings.autoSave != isChecked) {
                    settings.autoSave = isChecked
                }
            }
        ))
        list.add(SeparatorVerticalBlock())
        list.add(SeparateScreenBlock(
                titleRes = R.string.settings_swipe_actions_title,
                descriptionRes = R.string.settings_swipe_actions_description,
                clickListener = {
                    navigateTo(R.id.action_settings_swipe_actions)
                }
        ))
        list.add(SeparatorVerticalBlock())
        list.add(SeparateScreenBlock(
                titleRes = R.string.settings_double_click_title,
                descriptionRes = R.string.settings_double_click_description,
                clickListener = {
                    navigateTo(R.id.action_settings_double_click_actions)
                }
        ))
        list.add(SeparatorVerticalBlock())

        // APPEARANCE
        list.add(HeaderBlock(R.string.settings_group_appearance))
        list.add(DialogBlock(
                titleRes = R.string.settings_theme_title,
                descriptionRes = R.string.settings_theme_description,
                value = viewModel.string(Theme.valueOf(settings).titleRes).toString(),
                clickListener = {
                    val themes = Theme.values()
                    val theme = Theme.valueOf(settings)
                    val themeIndex = theme.ordinal
                    val themeItems = themes.map { getString(it.titleRes) }

                    val builder = MaterialAlertDialogBuilder(ctx)
                    builder.setTitle(R.string.settings_theme_title)
                    builder.setSingleChoiceItems(themeItems.toTypedArray(), themeIndex) { dialog, which ->
                        dialog.dismiss()
                        val selected = themes[which]
                        viewModel.onChangeTheme(selected)
                    }
                    builder.show()
                }
        ))
        list.add(SeparatorVerticalBlock())

        list.add(HeaderBlock(R.string.settings_group_backup))
        list.add(LabelBlock(
                titleRes = R.string.settings_backup_title,
                descriptionRes = R.string.settings_backup_description,
                clickListener = {
                    val options = mutableListOf<SelectValueDialogRequest.Option<BackupItemType>>()
                    BackupItemType.values().forEach { type ->
                        options.add(SelectValueDialogRequest.Option(
                                checked = true,
                                title = viewModel.string(type.titleRes),
                                model = type
                        ))
                    }
                    val request = SelectValueDialogRequest(
                            title = viewModel.string(R.string.settings_backup_title),
                            withImmediateNotify = false,
                            withClearAll = true,
                            withClearAllCustomTitleRes = R.string.button_confirm,
                            withClearAllCustomListener = {
                                if (it.isNotEmpty()) {
                                    viewModel.backupManager.backup(act, it)
                                }
                                true
                            },
                            options = options,
                            single = false,
                            onSelected = {}
                    )
                    viewModel.dialogState.requestSelectValueDialog(request)
                }
        ))
        list.add(SeparatorVerticalBlock())
        list.add(LabelBlock(
                titleRes = R.string.settings_restore_title,
                description = viewModel.string(R.string.settings_restore_description, viewModel.appConfig.getBackupSupportedImportFormats()).toString(),
                clickListener = {
                    viewModel.backupManager.restore(act)
                }
        ))
        list.add(SeparatorVerticalBlock(marginHoriz = 0))

        // ABOUT
        list.add(AboutBlock(userState = viewModel.userState))

        return list
    }

}
