package clipto.config

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import clipto.analytics.Analytics
import clipto.common.extensions.notNull
import clipto.common.logging.L
import clipto.common.misc.GsonUtils
import clipto.domain.RuneConfig
import clipto.domain.User
import clipto.extensions.log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.reflect.TypeToken
import com.wb.clipboard.BuildConfig
import com.wb.clipboard.R
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

class AppConfig(val app: Application) : IAppConfig {

    private val cfg by lazy {
        FirebaseRemoteConfig.getInstance()
            .apply {
                Schedulers.io().scheduleDirect {
                    runCatching {
                        val interval = TimeUnit.HOURS.toSeconds(2)
                        val config = FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(interval).build()
                        setConfigSettingsAsync(config)
                        setDefaultsAsync(R.xml.remote_config_defaults)
                        fetchAndActivate()
                    }.exceptionOrNull()?.let {
                        Analytics.onError("error_fetch_config", it)
                    }
                }
            }
    }

    private val runesConfig by lazy {
        runCatching {
            val json = cfg.getString("rune_configs")
            val configs: List<RuneConfig> = GsonUtils.get().fromJson(json, RUNES_CONFIG_TYPE.type)
            configs
        }.getOrNull() ?: emptyList()
    }

    private val checkTypeForTextAvailabilityIgnorePackages by lazy {
        runCatching {
            cfg.getString("texpander_check_type_for_text_availability_packages").split(",").map { it.trim() }
        }.getOrNull() ?: emptyList()
    }

    override fun getRxTimeout(): Long = 30_000

    override fun getUiTimeout(): Long = 300

    override fun getApiVersion(): Int = BuildConfig.VERSION_CODE

    override fun isNewVersionAvailable(): Boolean {
        try {
            val version = getLatestVersion()
            val regex = "[.-]".toRegex()
            val ver = version.split(regex)
            val latestVersionCode =
                if (ver.size >= 3) {
                    val major = ver[0].toIntOrNull() ?: 0
                    val minor = ver[1].toIntOrNull() ?: 0
                    val patch = ver[2].toIntOrNull() ?: 0
                    major * 1000000 + minor * 10000 + patch * 100
                } else {
                    BuildConfig.VERSION_CODE
                }
            var isAvailable = latestVersionCode > BuildConfig.VERSION_CODE
            val remindInterval = getLatestVersionRemindIntervalInHours()
            if (isAvailable && remindInterval > 0) {
                val prefs = app.getSharedPreferences(PREF_APP_LATEST_VERSION, Context.MODE_PRIVATE)
                val latestCheck = GsonUtils.parseDate(prefs.getString(version, null))
                isAvailable = latestCheck == null || System.currentTimeMillis() - latestCheck.time > TimeUnit.HOURS.toMillis(remindInterval.toLong())
                L.log(this, "isNewVersionAvailable ({}): version={}, lastCheck={}, interval={}", isAvailable, version, latestCheck, remindInterval)
            }
            return isAvailable
        } catch (e: Exception) {
            return false
        }
    }

    override fun remindAboutNewVersionLater(version: String) {
        val prefs = app.getSharedPreferences(PREF_APP_LATEST_VERSION, Context.MODE_PRIVATE)
        val latestCheckString = GsonUtils.formatDate(Date())
        L.log(this, "remindAboutNewVersionLater: version={}, lastCheck={}", version, latestCheckString)
        prefs.edit().putString(version, latestCheckString).apply()
    }

    override fun getDate(user: User?, id: String): Date? = user?.firebaseId
        ?.let { app.getSharedPreferences(it, Context.MODE_PRIVATE) }
        ?.getString(id, null)
        ?.let { GsonUtils.parseDateWithMillis(it) ?: GsonUtils.parseDate(it) }
        .also { log("READ DATE :: {} -> {}", id, it) }

    override fun saveDate(user: User?, id: String, date: Date?) {
        user?.firebaseId
            ?.let { app.getSharedPreferences(it, Context.MODE_PRIVATE) }
            ?.edit(commit = true) {
                val value = GsonUtils.formatDateWithMillis(date)
                log("SAVE DATE :: {} -> {}", id, value)
                putString(id, value)
            }
    }

    override fun attachmentUploadLimit(): Int = cfg.getLong("attachment_upload_limit").toInt()
    override fun attachmentUploadLimitInBytes(): Int = attachmentUploadLimitInKilobytes() * 1024
    override fun attachmentUploadLimitInKilobytes(): Int = attachmentUploadLimit() * 1024
    override fun hasAccessToAttachments() = cfg.getBoolean("access_attachments")

    // Mobile
    override fun hasAccessToScanBarcode() = cfg.getBoolean("access_scan_barcode")

    // Clipboard
    @Deprecated("unused")
    override fun canUpdateNotificationOnResume(): Boolean = cfg.getBoolean("can_update_notification_on_resume")

    @Deprecated("unused")
    override fun canTakeNoteFromClipboardOnResume(): Boolean = cfg.getBoolean("can_take_note_from_clipboard_on_resume")
    override fun canPauseClipboardOnScreenLock(): Boolean = cfg.getBoolean("can_pause_clipboard_on_screen_lock")
    override fun canSilentlyVibrateOnClipboardChanges(): Boolean = cfg.getBoolean("can_silently_vibrate_on_clipboard_changes")
    override fun clipboardAwareInterval(): Long = cfg.getLong("clipboard_aware_interval")
    override fun clipboardHideOnCopyDelay(): Long = cfg.getLong("clipboard_hide_on_copy_delay")
    override fun limitClipboardNotesDefault(): Int = cfg.getLong("limit_clipboard_notes_default").toInt()
    override fun limitClipboardNotesCleanupCount(): Int = cfg.getLong("limit_clipboard_notes_cleanup_count").toInt()
    override fun limitClipboardNotesRange(): IntArray = cfg.getString("limit_clipboard_notes_range").let {
        GsonUtils.get().fromJson(it, IntArray::class.java)
    }

    override fun logcatCancelTimeout(): Int = cfg.getLong("logcat_cancel_timeout").toInt()
    override fun logcatReadTimeout(): Int = cfg.getLong("logcat_read_timeout").toInt()
    override fun notificationUseApplicationStyle() = cfg.getBoolean("notification_use_application_style")

    @Deprecated("unused")
    override fun canRestoreLastClipOnStart(): Boolean = cfg.getBoolean("can_restore_last_clip_on_start")

    // Common
    override fun maxLengthTag(): Int = cfg.getLong("max_length_tag").toInt()
    override fun maxLengthTitle(): Int = cfg.getLong("max_length_title").toInt()
    override fun maxLengthDescription(): Int = cfg.getLong("max_length_description").toInt()
    override fun maxLengthAbbreviation(): Int = cfg.getLong("max_length_abbreviation").toInt()

    // Note
    override fun canCreatePublicLinks(): Boolean = cfg.getBoolean("can_create_public_links")
    override fun noteScrollBarMultiplier(): Int = cfg.getLong("note_scrollbar_multiplier").toInt()
    override fun textLengthForAsyncRendering(): Int = cfg.getLong("text_length_for_async_rendering").toInt()
    override fun clipInfoMaxTextLines(): Int = cfg.getLong("clip_info_max_text_lines").toInt()
    override fun clipEditFocusOnText(): Boolean = cfg.getBoolean("clip_edit_focus_on_text")
    override fun autoSaveInterval(): Long = cfg.getLong("auto_save_interval")
    override fun notePreviewModeAutoScrollMultiplier(): Int = cfg.getLong("note_preview_mode_multiplier").toInt()
    override fun notePreventAccidentClicksDistance(): Int = cfg.getLong("note_prevent_accident_clicks_distance").toInt()
    override fun noteLinkClickRadius(): Int = cfg.getLong("note_link_click_radius").toInt()
    override fun noteSupportFastScroll(): Boolean = cfg.getBoolean("note_support_fast_scroll")
    override fun noteSupportFastPager(): Boolean = cfg.getBoolean("note_support_fast_pager")
    override fun noteSupportFastPagerHapticFeedback(): Boolean = cfg.getBoolean("note_support_fast_pager_haptic_feedback")
    override fun noteSupportFastPagerHapticFeedbackMaxWhenScroll(): Int = cfg.getLong("note_support_fast_pager_haptic_feedback_max_when_scroll").toInt()
    override fun noteMaxSizeInKb(): Long = cfg.getLong("note_max_size_in_kb")
    override fun getAbbreviationLeadingSymbol(): String = cfg.getString("note_abbreviation_leading_symbol")
    override fun getFastScrollBarMinTextLines(): Int = cfg.getLong("note_fast_scroll_min_text_lines").toInt()

    // Markdown
    override fun markdownEditorActivated() = cfg.getBoolean("markdown_editor_activated")
    override fun markdownStrikethroughNormalizationActivated(): Boolean = cfg.getBoolean("markdown_strikethrough_normalization_activated")
    override fun markdownBulletListNormalizationActivated(): Boolean = cfg.getBoolean("markdown_bullet_list_normalization_activated")

    // Main List
    override fun mainListDisplayCounter(): Boolean = cfg.getBoolean("main_list_display_counter")
    override fun mainListSupportFastScroll(): Boolean = cfg.getBoolean("main_list_support_fast_scroll")

    // Tag
    override fun canCreateTagAutoRules(): Boolean = cfg.getBoolean("can_create_tag_auto_rules")
    override fun tagRuleMaxLength(): Int = cfg.getLong("tag_rule_max_length").toInt()
    override fun getColorsMatrix(): List<List<String?>> = getTagColors().map { listOf(null).plus(it) }
    override fun getTagColors(): List<List<String>> = runCatching {
        val colorsMatrix: List<List<String>> = GsonUtils.get().fromJson(cfg.getString("tag_colors"), COLORS_MATRIX_TYPE.type)
        colorsMatrix
    }.getOrDefault(emptyList())

    // Recycle Bin
    override fun limitDeletedNotesDefault(): Int = cfg.getLong("limit_deleted_notes_default").toInt()
    override fun limitDeletedNotesRange(): IntArray = cfg.getString("limit_deleted_notes_range").let {
        GsonUtils.get().fromJson(it, IntArray::class.java)
    }

    override fun getClipListSize(): Int = 20
    override fun canRequestRebuildIndex(): Boolean = cfg.getBoolean("can_request_rebuild_index")
    override fun getLatestVersionRemindIntervalInHours(): Int = cfg.getLong("app_latest_version_remind_interval_in_hours").toInt()
    override fun getLatestVersion(): String = cfg.getString("app_latest_version")
    override fun getLatestVersionChanges(): String = cfg.getString("app_latest_changes")
    override fun apiReadTimeout(): Long = cfg.getLong("api_read_timeout")
    override fun canReportIssues(): Boolean = cfg.getBoolean("can_report_issues")
    override fun canReportNegativeFeedback(): Boolean = cfg.getBoolean("can_report_negative_feedback")
    override fun canReportUnexpectedErrors(): Boolean = cfg.getBoolean("can_report_unexpected_errors")
    override fun canReportUnexpectedErrorDirectly(): Boolean = cfg.getBoolean("can_report_unexpected_errors_directly")
    override fun dataLoadingTimeout(): Long = cfg.getLong("data_loading_timeout")
    override fun externalActionDelay(): Long = cfg.getLong("external_action_delay")
    override fun firebaseAnalyticsCollectionEnabled(): Boolean = cfg.getBoolean("firebase_analytics_collection_enabled")
    override fun firestoreBatchSize(): Int = cfg.getLong("firestore_batch_size").toInt()
    override fun firestoreBatchDelay(): Long = cfg.getLong("firestore_batch_delay")
    override fun firebaseLanguageIdentificationThreshold(): Float = cfg.getDouble("firebase_language_identification_threshold").toFloat()
    override fun maxNotesForContextActions(): Int = cfg.getLong("max_notes_for_context_actions").toInt()
    override fun textLayoutDelay(): Long = cfg.getLong("text_layout_delay")
    override fun queryOnlyLatestChangedNotesThreshold(): Int = cfg.getLong("query_only_latest_changed_notes_threshold").toInt()
    override fun queryOnlyLatestChangedNotesReconnectDelay(): Long = cfg.getLong("query_only_latest_changed_notes_reconnect_delay")
    override fun queryOnlyLatestChangedNotesMinimumInterval(): Long = cfg.getLong("query_only_latest_changed_notes_minimum_interval")
    override fun queryOnlyLatestChangedNotesUseInitialQuery(): Boolean = cfg.getBoolean("query_only_latest_changed_notes_use_initial_query")
    override fun canIncludeRemoteConfigInEmail(): Boolean = cfg.getBoolean("can_include_remote_config_in_email")
    override fun canDeleteFiles(): Boolean = cfg.getBoolean("can_delete_files")
    override fun canRemoveNotSyncedNotesOnLogout(): Boolean = cfg.getBoolean("can_remove_not_synced_notes_on_logout")
    override fun getInternetStateCheckInterval(): Long = cfg.getLong("internet_state_check_interval")
    override fun getMaxOpenGraphSizeInKb(): Int = cfg.getLong("max_open_graph_size_in_kb").toInt()

    override fun syncPlanActivated(): Boolean = cfg.getBoolean("sync_plan_activated")
    override fun syncPlanNotesFreeLimit(): Int = cfg.getLong("sync_plan_notes_free_limit").toInt()
    override fun syncPlanNotesRecommendedLimit(): Int = cfg.getLong("sync_plan_notes_recommended_limit").toInt()
    override fun syncPlanContributorProgramEnabled(): Boolean = cfg.getBoolean("sync_plan_contributor_program_enabled")
    override fun syncPlanNotesBonusForSnippet(): Int = cfg.getLong("sync_plan_notes_bonus_for_snippet").toInt()
    override fun syncPlans(): List<List<String>> = runCatching {
        val plansJson = cfg.getString("sync_plans")
        val plans: List<List<String>> = GsonUtils.get().fromJson(plansJson, SYNC_PLANS_TYPE.type)
        plans
    }.getOrNull() ?: emptyList()

    // Urls
    override fun getLinkPreviewExampleUrl(): String = cfg.getString("url_link_preview_example")
    override fun getInviteFriendRewardUrl(): String = cfg.getString("url_invite_friend_reward")
    override fun getAdbInstructionUrl(): String = cfg.getString("url_adb_instruction")
    override fun getGlobalCopyInstructionUrl(): String = cfg.getString("url_global_copy_instruction")
    override fun getNotificationPasteInstructionUrl(): String = cfg.getString("url_notification_paste_instruction")
    override fun getTranslateUrl(): String = cfg.getString("url_translate")
    override fun getFacebookUrl(): String = cfg.getString("url_facebook")
    override fun getRedditUrl(): String = cfg.getString("url_reddit")
    override fun getGithubUrl(): String = cfg.getString("url_github")
    override fun getFaqUrl(): String = cfg.getString("url_faq")
    override fun getDiscordUrl(): String = cfg.getString("url_discord")
    override fun getChangelogUrl(): String = cfg.getString("url_changelog")
    override fun getSupportEmail(): String = cfg.getString("email_support")
    override fun getUnexpectedErrorInstructionUrl(): String = cfg.getString("url_unexpected_error_instruction")

    override fun getAppGuideId(): String = cfg.getString("app_guide_id")

    // Backup
    override fun getBackupSupportedImportFormats(): String = cfg.getString("backup_supported_import_formats")

    // Smart Actions
    override fun smartActionShortenLinkEnabled(): Boolean = cfg.getBoolean("smart_action_shorten_link_enabled")
    override fun smartActionShortenLinkMinimumLength(): Int = cfg.getLong("smart_action_shorten_link_min_length").toInt()

    // Security
    override fun autoLockInMinutes(): Int = cfg.getLong("auto_lock_in_minutes").toInt()

    @Deprecated("unused")
    override fun autoLockWithForeground(): Boolean = cfg.getBoolean("auto_lock_with_foreground")
    override fun suggestSetPassCodeOnSignIn(): Boolean = cfg.getBoolean("suggest_set_passcode_on_sign_in")

    // Dynamic Values
    override fun deepReplaceLevel(): Int = cfg.getLong("deep_replace_level").toInt()

    // Runes
    override fun isRunesEnabled(): Boolean = cfg.getBoolean("runes_enabled")
    override fun getRuneConfigs(): List<RuneConfig> = runesConfig

    override fun getRuneSwipePowerDelay(): Long = cfg.getLong("rune_swipe_power_delay")

    override fun texpanderTextSize(): Int = cfg.getLong("texpander_text_size").toInt()
    override fun texpanderLayoutDelay(): Long = cfg.getLong("texpander_layout_delay")
    override fun texpanderAutoHide(): Boolean = cfg.getBoolean("texpander_auto_hide")
    override fun texpanderDoubleClickToShow(): Boolean = cfg.getBoolean("texpander_double_click_to_show")
    override fun texpanderPanelDestroyOnInactivityIntervalInSeconds(): Int = cfg.getLong("texpander_panel_destroy_on_inactivity_interval_in_seconds").toInt()
    override fun texpanderDoubleClickToShowThreshold(): Int = cfg.getLong("texpander_double_click_to_show_threshold").toInt()
    override fun texpanderIgnoreInternalEvents(): Boolean = cfg.getBoolean("texpander_ignore_internal_events")
    override fun texpanderAdvancedConfigUrl(): String = cfg.getString("url_texpaner_advanced_config")
    override fun texpanderEventThreshold(): Long = cfg.getLong("texpander_event_threshold")
    override fun texpanderLastEventStateCheckDelay(): Long = cfg.getLong("texpander_last_event_state_check_delay")
    override fun texpanderUndoRedoInputMemory(): Int = cfg.getLong("texpander_input_memory").toInt()
    override fun texpanderCheckTypeForTextAvailability(packageName: CharSequence?): Boolean = cfg.getBoolean("texpander_check_type_for_text_availability") &&
            !checkTypeForTextAvailabilityIgnorePackages.contains(packageName?.toString().notNull())

    override fun getNotificationIntentMaxSize(): Long = cfg.getLong("notification_intent_max_size")
    override fun getNotificationTextMaxSize(): Int = cfg.getLong("notification_text_max_size").toInt()

    override fun getYoutubeDataApiKey(): String = cfg.getString("youtube_data_api_key")

    override fun isSnippetsPublicLibraryAuthRequired(): Boolean = cfg.getBoolean("snippets_public_library_auth_required")
    override fun isSnippetsPublicLibraryAvailable(): Boolean = cfg.getBoolean("snippets_public_library_available")
    override fun getSnippetsKitBonusProgramUrl(): String = cfg.getString("url_snippets_kit_bonus_program")
    override fun getSnippetsKitMinimumSize(): Int = cfg.getLong("snippets_kit_minimum_size").toInt()

    override fun getDynamicValueLabelMaxLength(): Int = cfg.getLong("dynamic_value_label_max_length").toInt()
    override fun getDynamicValueRenderingDelay(): Long = cfg.getLong("dynamic_value_rendering_delay")
    override fun getDynamicValueRefreshDelay(): Long = cfg.getLong("dynamic_value_refresh_delay")
    override fun getDynamicTextRequestDelay(): Long = cfg.getLong("dynamic_text_request_delay")

    companion object {
        private const val PREF_APP_LATEST_VERSION = "app_latest_version"
        private val COLORS_MATRIX_TYPE = object : TypeToken<List<List<String?>>>() {}
        private val SYNC_PLANS_TYPE = object : TypeToken<List<List<String>>>() {}
        private val RUNES_CONFIG_TYPE = object : TypeToken<List<RuneConfig>>() {}
    }
}